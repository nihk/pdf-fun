package nick.template.data

sealed class Effect {
    data class ShowFileSystemEffect(val mimeTypes: List<String>) : Effect()
    data class MoveToPageEffect(val page: Int) : Effect()
}
