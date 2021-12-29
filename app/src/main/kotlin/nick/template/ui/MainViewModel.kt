package nick.template.ui

import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import nick.template.data.Event
import nick.template.data.Page
import nick.template.data.PdfRepository
import nick.template.data.Result

class MainViewModel(
    private val handle: SavedStateHandle,
    private val pdfRepository: PdfRepository
) : ViewModel() {
    private val events = MutableSharedFlow<Event>()
    val pages: StateFlow<List<Page>>

    init {
        pages = events
            .onSubscription { processEvent(Event.Initialize) }
            .onEach { event -> Log.d("asdf", "event: $event") }
            .shareIn( // Share emissions to the individual streams in toResults()
                scope = viewModelScope,
                started = SharingStarted.Lazily
            )
            .toResults()
            .scan(emptyList<Page>()) { state, result ->
                when (result) {
                    is Result.PagesResult -> result.pages
                    is Result.PageResult -> state.toMutableList().apply {
                        set(result.page.number, result.page)
                    }
                    Result.NoOpResult -> state
                }
            }
            .onCompletion {
                Log.d("asdf", "Closing time")
                pdfRepository.close()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )
    }

    private fun Flow<Event>.toResults(): Flow<Result> {
        return merge(
            filterIsInstance<Event.Initialize>().map {
                val pageCount = pdfRepository.initialize("wtc.pdf")
                val pages = List(pageCount, ::Page)
                Result.PagesResult(pages)
            },
            // flatMapMerge to allow concurrent page requests. Ultimately PdfRenderer will render
            // pages serially, but this ViewModel doesn't know that, so it tries to do concurrently
            // loaded pages as best as it can.
            filterIsInstance<Event.GetPage>().flatMapMerge { event ->
                val currentPage = pages.value[event.page]

                if (currentPage.bitmap != null) {
                    // Already loaded the bitmap
                    Result.NoOpResult.let(::flowOf)
                } else {
                    flow {
                        val bitmap = pdfRepository.page(event.page)
                        val result = Result.PageResult(
                            page = currentPage.copy(bitmap = bitmap)
                        )
                        emit(result)
                    }
                }
            }
        )
    }

    fun processEvent(event: Event) {
        viewModelScope.launch {
            events.emit(event)
        }
    }

    class Factory @Inject constructor(private val pdfRepository: PdfRepository) {
        fun create(owner: SavedStateRegistryOwner): AbstractSavedStateViewModelFactory {
            return object : AbstractSavedStateViewModelFactory(owner, null) {
                override fun <T : ViewModel?> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(handle, pdfRepository) as T
                }
            }
        }
    }
}
