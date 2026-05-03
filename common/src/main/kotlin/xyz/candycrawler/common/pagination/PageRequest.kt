package xyz.candycrawler.common.pagination

private const val MAX_PAGE_SIZE = 100

data class PageRequest(val page: Int, val size: Int, val sortBy: String, val sortDir: SortDir) {
    init {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..MAX_PAGE_SIZE) { "size must be between 1 and $MAX_PAGE_SIZE" }
    }
}
