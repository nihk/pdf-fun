package nick.template.ui

import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nick.template.data.Effect
import nick.template.data.Event
import nick.template.data.Page
import nick.template.data.PdfRepository
import nick.template.data.Result
import nick.template.data.State

class MainViewModel(
    private val handle: SavedStateHandle,
    private val pdfRepository: PdfRepository
) : ViewModel() {
    private val events = MutableSharedFlow<Event>()
    val states: StateFlow<State>
    val effects: Flow<Effect>

    init {
        events
            .onEach { event -> Log.d("asdf", "event: $event") }
            .onCompletion {
                Log.d("asdf", "Closing time")
                pdfRepository.close()
            }
            .share() // Share emissions to the individual streams in toResults()
            .toResults()
            .share() // Share emissions to streams below
            .also { results ->
                states = results.toStates()
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.Lazily,
                        initialValue = State()
                    )

                effects = results.toEffects()
            }
    }

    private fun <T> Flow<T>.share() = shareIn(viewModelScope, SharingStarted.Lazily)

    private fun Flow<Result>.toStates(): Flow<State> {
        return scan(State()) { state, result ->
            when (result) {
                is Result.PagesResult -> state.copy(pages = result.pages)
                is Result.PageResult -> state.copy(
                    pages = state.pages.toMutableList().apply {
                        set(result.page.number, result.page)
                    }
                )
                else -> state
            }
        }
    }

    private fun Flow<Result>.toEffects(): Flow<Effect> {
        return merge(
            filterIsInstance<Result.ShowFileSystemResult>().mapLatest { result ->
                Effect.ShowFileSystemEffect(mimeTypes = result.mimeTypes)
            },
            filterIsInstance<Result.MoveToPageResult>().mapLatest { result ->
                Effect.MoveToPageEffect(page = result.page)
            }
        )
    }

    private fun Flow<Event>.toResults(): Flow<Result> {
        return merge(
            filterIsInstance<Event.ShowFileSystem>().map {
                Result.ShowFileSystemResult(mimeTypes = listOf("application/pdf"))
            },
            filterIsInstance<Event.OpenFile>().flatMapConcat { event ->
                val pageCount = pdfRepository.openFile(event.uri)
                val pages = List(pageCount, ::Page)
                flowOf(Result.PagesResult(pages), Result.MoveToPageResult(page = 0))
            },
            // flatMapMerge to allow concurrent page requests. Ultimately PdfRenderer will render
            // pages serially, but this ViewModel doesn't know that, so it tries to do concurrently
            // loaded pages as best as it can.
            filterIsInstance<Event.GetPage>().flatMapMerge { event ->
                val currentPage = states.value.pages[event.page]

                if (currentPage.bitmap != null) {
                    // Already loaded the bitmap
                    Result.NoOpResult.let(::flowOf)
                } else {
                    flow {
                        val loading = Result.PageResult(
                            page = currentPage.copy(isLoading = true)
                        )
                        emit(loading)

                        val bitmap = pdfRepository.page(event.page)
                        val result = Result.PageResult(
                            page = currentPage.copy(bitmap = bitmap, isLoading = false)
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
