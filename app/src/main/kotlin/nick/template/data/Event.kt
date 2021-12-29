package nick.template.data

sealed class Event {
    object Initialize : Event()
    data class GetPage(val page: Int): Event()
}
