package xyz.candycrawler.wizardstataggregator.domain.stat.limited.exception

class InvalidCardLimitedStatsException(reason: String) : RuntimeException("CardLimitedStats is invalid: $reason")
