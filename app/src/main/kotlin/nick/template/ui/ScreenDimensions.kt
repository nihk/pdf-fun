package nick.template.ui

import android.content.res.Resources
import javax.inject.Inject

interface ScreenDimensions {
    val width: Int
    val height: Int
}

class AndroidScreenDimensions @Inject constructor(
    private val resources: Resources
) : ScreenDimensions {
    override val width: Int
        get() = resources.displayMetrics.widthPixels

    override val height: Int
        get() = resources.displayMetrics.heightPixels
}
