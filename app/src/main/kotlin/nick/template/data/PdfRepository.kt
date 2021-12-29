package nick.template.data

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import androidx.core.graphics.createBitmap
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface PdfRepository {
    // Returns the page count of the file
    suspend fun initialize(filename: String): Int
    suspend fun page(page: Int): Bitmap
    fun close()
}

class AssetPdfRepository @Inject constructor(
    private val assetManager: AssetManager
) : PdfRepository {
    private var pdfRenderer: PdfRenderer? = null
    private val mutex = Mutex()

    override suspend fun initialize(filename: String): Int = withContext(Dispatchers.IO) {
        val fileDescriptor = assetManager
            .openFd(filename)
            .parcelFileDescriptor
        PdfRenderer(fileDescriptor)
            .also { pdfRenderer = it }
            .pageCount
    }

    override fun close() {
        pdfRenderer?.close()
        pdfRenderer = null
    }

    override suspend fun page(page: Int): Bitmap {
        // PdfRenderer restricts only 1 page opened at a time, multiple page requests have to
        // be done serially.
        mutex.withLock {
            return withContext(Dispatchers.IO) {
                requireNotNull(pdfRenderer) { "Didn't initialize this repository" }
                    .openPage(page)
                    .render(Resources.getSystem().displayMetrics.widthPixels)
            }
        }
    }

    private fun PdfRenderer.Page.render(width: Int) = use {
        createBitmap(width).also { bitmap ->
            render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }
    }

    private fun PdfRenderer.Page.createBitmap(bitmapWidth: Int): Bitmap {
        return createBitmap(
            width = bitmapWidth,
            // Scale the height based on the max width
            height = (bitmapWidth.toFloat() / width * height).toInt()
        ).also { bitmap ->
            Canvas(bitmap).apply {
                // White background to support transparent PDF pages
                drawColor(Color.WHITE)
                drawBitmap(bitmap, 0f, 0f, null)
            }
        }
    }
}
