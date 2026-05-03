package xyz.candycrawler.wizardstataggregator.domain.stat.limited.exception

class InvalidTrackedLimitedStatSetException(reason: String) :
    RuntimeException("TrackedLimitedStatSet is invalid: $reason")
