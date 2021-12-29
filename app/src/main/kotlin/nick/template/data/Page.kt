package nick.template.data

import android.graphics.Bitmap

data class Page(
    val number: Int,
    val isLoading: Boolean = false,
    val bitmap: Bitmap? = null
)
