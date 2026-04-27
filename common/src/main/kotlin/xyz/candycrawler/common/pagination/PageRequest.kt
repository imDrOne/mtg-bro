package xyz.candycrawler.common.pagination

data class PageRequest(
    val page: Int,
    val size: Int,
    val sortBy: String,
    val sortDir: SortDir,
) {
    init {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..100) { "size must be between 1 and 100" }
    }
}
