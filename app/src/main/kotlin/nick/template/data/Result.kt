package nick.template.data

sealed class Result {
    object NoOpResult : Result()
    data class PagesResult(val pages: List<Page>) : Result()
    data class PageResult(val page: Page) : Result()
    data class ShowFileSystemResult(val mimeTypes: List<String>) : Result()
    data class MoveToPageResult(val page: Int) : Result()
}
