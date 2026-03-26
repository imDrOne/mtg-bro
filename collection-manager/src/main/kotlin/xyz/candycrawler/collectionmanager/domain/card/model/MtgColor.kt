package xyz.candycrawler.collectionmanager.domain.card.model

enum class MtgColor {
    W, U, B, R, G;

    companion object {
        fun sortIndex(colorCode: String): Int =
            entries.indexOfFirst { it.name.equals(colorCode, ignoreCase = true) }
    }
}
