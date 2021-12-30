package nick.template.data

sealed class Result {
    object NoOpResult : Result()
    data class PagesResult(val pages: List<Page>) : Result()
    data class PageResult(val page: Page) : Result()
    object ShowFileSystemResult : Result()
}
