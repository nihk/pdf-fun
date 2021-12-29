package nick.template.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nick.template.data.Page
import nick.template.databinding.PageBinding

class PageAdapter : ListAdapter<Page, PageViewHolder>(PageDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        return LayoutInflater.from(parent.context)
            .let { inflater -> PageBinding.inflate(inflater, parent, false) }
            .let { binding -> PageViewHolder(binding) }
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

object PageDiffCallback : DiffUtil.ItemCallback<Page>() {
    override fun areItemsTheSame(oldItem: Page, newItem: Page): Boolean {
        return oldItem.number == newItem.number
    }

    override fun areContentsTheSame(oldItem: Page, newItem: Page): Boolean {
        return oldItem == newItem
    }
}

class PageViewHolder(private val binding: PageBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(page: Page) {
        binding.loading.isVisible = page.bitmap == null
        binding.image.setImageBitmap(page.bitmap)
    }
}
