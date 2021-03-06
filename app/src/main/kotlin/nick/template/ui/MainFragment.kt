package nick.template.ui

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import nick.template.R
import nick.template.data.Effect
import nick.template.data.Event
import nick.template.databinding.MainFragmentBinding
import nick.template.ui.adapters.PageAdapter

class MainFragment @Inject constructor(
    private val factory: MainViewModel.Factory
) : Fragment(R.layout.main_fragment) {
    private val viewModel: MainViewModel by viewModels { factory.create(this) }
    private val openFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val event = Event.OpenFile(uri ?: return@registerForActivityResult)
        viewModel.processEvent(event)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = MainFragmentBinding.bind(view)

        val adapter = PageAdapter()
        binding.pager.adapter = adapter
        binding.pager.offscreenPageLimit = 1 // Pre-load adjacent pages

        val events = merge(
            binding.openFile.clicks().map { Event.ShowFileSystem },
            adapter.renderRequests().map(Event::GetPage)
        )
            .onEach(viewModel::processEvent)

        val states = viewModel.states
            .onEach { state -> adapter.submitList(state.pages) }

        val effects = viewModel.effects
            .onEach { effect ->
                when(effect) {
                    is Effect.ShowFileSystemEffect -> openFile.launch(effect.mimeTypes.toTypedArray())
                    is Effect.MoveToPageEffect -> binding.pager.setCurrentItem(effect.page, false)
                }
            }

        merge(events, states, effects).launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun View.clicks() = callbackFlow {
        setOnClickListener { trySend(Unit) }
        awaitClose { setOnClickListener(null) }
    }
}
