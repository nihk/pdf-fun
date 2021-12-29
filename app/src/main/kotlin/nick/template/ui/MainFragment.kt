package nick.template.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
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

        binding.pager.pageSelections()
            .onEach { page -> viewModel.processEvent(Event.GetPage(page)) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.pages
            .onEach { pages -> adapter.submitList(pages) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun ViewPager2.pageSelections(): Flow<Int> = callbackFlow {
        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                trySend(position)
            }
        }

        registerOnPageChangeCallback(callback)

        awaitClose { unregisterOnPageChangeCallback(callback) }
    }
}
