package xyz.candycrawler.wizardstataggregator.infrastructure.db.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime

object TrackedLimitedStatSetsTable : Table("tracked_limited_stat_sets") {
    val setCode = varchar("set_code", 10)
    val watchUntil = date("watch_until")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(setCode, name = "pk_tracked_limited_stat_sets")
}

