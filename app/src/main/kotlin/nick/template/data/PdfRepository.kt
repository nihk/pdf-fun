package nick.template.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.graphics.createBitmap
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nick.template.di.IoContext
import nick.template.ui.ScreenDimensions

interface PdfRepository {
    // Returns the page count of the file
    suspend fun openFile(uri: Uri): Int
    suspend fun page(page: Int): Bitmap
    suspend fun close()
}

class FileSystemPdfRepository @Inject constructor(
    @IoContext private val ioContext: CoroutineContext,
    private val contentResolver: ContentResolver,
    private val screenDimensions: ScreenDimensions
) : PdfRepository {
    private var pdfRenderer: PdfRenderer? = null
    private val mutex = Mutex()

    override suspend fun openFile(uri: Uri): Int = withContext(ioContext) {
        close()
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            ?: error("Uri not found: $uri")

        PdfRenderer(parcelFileDescriptor)
            .also { pdfRenderer = it }
            .pageCount
    }

    override suspend fun close() = withContext(ioContext) {
        pdfRenderer?.close()
        pdfRenderer = null
    }

    override suspend fun page(page: Int): Bitmap {
        // PdfRenderer restricts only 1 page opened at a time, multiple page requests have to
        // be done serially.
        mutex.withLock {
            return withContext(ioContext) {
                requireNotNull(pdfRenderer) { "Didn't initialize this repository" }
                    .openPage(page)
                    .render(screenDimensions.width)
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
        ).apply {
            // White background to support transparent PDF pages
            eraseColor(Color.WHITE)
        }
    }
}
