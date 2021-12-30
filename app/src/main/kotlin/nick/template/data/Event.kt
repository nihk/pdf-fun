package nick.template.data

import android.net.Uri

sealed class Event {
    data class GetPage(val page: Int) : Event()
    data class OpenFile(val uri: Uri) : Event()
    object ShowFileSystem : Event()
}
