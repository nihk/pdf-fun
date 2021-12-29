package nick.template.data

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nick.template.di.CacheDir
import nick.template.di.IoContext

interface PdfRepository {
    // Returns the page count of the file
    suspend fun initialize(filename: String): Int
    suspend fun page(page: Int): Bitmap
    suspend fun close()
}

class AssetPdfRepository @Inject constructor(
    private val assetManager: AssetManager,
    @CacheDir private val cacheDir: File,
    @IoContext private val ioContext: CoroutineContext
) : PdfRepository {
    private var pdfRenderer: PdfRenderer? = null
    private val mutex = Mutex()

    override suspend fun initialize(filename: String): Int = withContext(ioContext) {
        // Contents of assets are compressed by default, and the PdfRenderer class cannot open it.
        // Work around this by copying the file into the cache directory.
        val file = File(cacheDir, filename)
        assetManager.open(filename).use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
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
        ).applyCanvas {
            // White background to support transparent PDF pages
            drawColor(Color.WHITE)
        }
    }
}
