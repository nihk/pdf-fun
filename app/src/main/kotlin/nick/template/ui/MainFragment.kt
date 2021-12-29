package nick.template.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import nick.template.R
import nick.template.data.Event
import nick.template.databinding.MainFragmentBinding
import nick.template.ui.adapters.PageAdapter

class MainFragment @Inject constructor(
    private val factory: MainViewModel.Factory
) : Fragment(R.layout.main_fragment) {
    private val viewModel: MainViewModel by viewModels { factory.create(this) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = MainFragmentBinding.bind(view)

        val adapter = PageAdapter()
        binding.pager.adapter = adapter
        binding.pager.offscreenPageLimit = 1 // Pre-load adjacent pages

        adapter.renderRequests()
            .map(Event::GetPage)
            .onEach(viewModel::processEvent)
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.pages
            .onEach(adapter::submitList)
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }
}
