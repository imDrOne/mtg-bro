package xyz.candycrawler.wizardstataggregator.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.wizardstataggregator.infrastructure.db.entity.TrackedLimitedStatSetRecord
import xyz.candycrawler.wizardstataggregator.lib.AbstractIntegrationTest
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Transactional
class TrackedLimitedStatSetSqlMapperTest(@Autowired private val sqlMapper: TrackedLimitedStatSetSqlMapper) :
    AbstractIntegrationTest() {

    @Test
    fun `upsert inserts tracked set`() {
        val record = buildRecord(setCode = "DMU", watchUntil = LocalDate.of(2026, 5, 1))

        val saved = sqlMapper.upsert(record)

        assertEquals("DMU", saved.setCode)
        assertEquals(LocalDate.of(2026, 5, 1), saved.watchUntil)
        assertNotNull(saved.createdAt)
        assertNotNull(saved.updatedAt)
    }

    @Test
    fun `upsert updates watchUntil for existing set`() {
        sqlMapper.upsert(buildRecord(setCode = "BLB", watchUntil = LocalDate.of(2026, 5, 1)))

        val updated = sqlMapper.upsert(buildRecord(setCode = "BLB", watchUntil = LocalDate.of(2026, 6, 1)))

        assertEquals("BLB", updated.setCode)
        assertEquals(LocalDate.of(2026, 6, 1), updated.watchUntil)
        assertEquals(listOf(updated), sqlMapper.selectAll().filter { it.setCode == "BLB" })
    }

    @Test
    fun `selectActive returns only sets whose watchUntil is today or later`() {
        sqlMapper.upsert(buildRecord(setCode = "OLD", watchUntil = LocalDate.of(2026, 4, 26)))
        sqlMapper.upsert(buildRecord(setCode = "TODAY", watchUntil = LocalDate.of(2026, 4, 27)))
        sqlMapper.upsert(buildRecord(setCode = "NEXT", watchUntil = LocalDate.of(2026, 4, 28)))

        val result = sqlMapper.selectActive(LocalDate.of(2026, 4, 27))

        assertEquals(listOf("NEXT", "TODAY"), result.map { it.setCode })
    }

    @Test
    fun `deleteBySetCode removes tracked set`() {
        sqlMapper.upsert(buildRecord(setCode = "TDM", watchUntil = LocalDate.of(2026, 5, 1)))

        sqlMapper.deleteBySetCode("TDM")

        assertEquals(emptyList(), sqlMapper.selectAll().filter { it.setCode == "TDM" })
    }

    private fun buildRecord(setCode: String, watchUntil: LocalDate): TrackedLimitedStatSetRecord =
        TrackedLimitedStatSetRecord(
            setCode = setCode,
            watchUntil = watchUntil,
            createdAt = null,
            updatedAt = null,
        )
}
