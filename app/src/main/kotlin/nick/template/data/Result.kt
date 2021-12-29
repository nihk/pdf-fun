package nick.template.data

sealed class Result {
    object NoOpResult : Result()
    data class UpdatedPagesResult(val pages: List<Page>) : Result()
}
