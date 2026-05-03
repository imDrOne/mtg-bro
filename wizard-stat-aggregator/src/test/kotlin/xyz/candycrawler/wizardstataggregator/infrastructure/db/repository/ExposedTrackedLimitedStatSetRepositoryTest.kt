package xyz.candycrawler.wizardstataggregator.infrastructure.db.repository

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.TrackedLimitedStatSet
import xyz.candycrawler.wizardstataggregator.infrastructure.db.entity.TrackedLimitedStatSetRecord
import xyz.candycrawler.wizardstataggregator.infrastructure.db.mapper.TrackedLimitedStatSetSqlMapper
import java.time.LocalDate
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ExposedTrackedLimitedStatSetRepositoryTest {

    @Mock
    private lateinit var sqlMapper: TrackedLimitedStatSetSqlMapper

    @InjectMocks
    private lateinit var repository: ExposedTrackedLimitedStatSetRepository

    @Test
    fun `save delegates upsert and returns mapped domain`() {
        val domain = TrackedLimitedStatSet("DMU", LocalDate.of(2026, 5, 1))
        val record = TrackedLimitedStatSetRecord("DMU", LocalDate.of(2026, 5, 1), null, null)
        given(sqlMapper.upsert(record)).willReturn(record)

        val result = repository.save(domain)

        assertEquals(domain, result)
    }

    @Test
    fun `findActive delegates to mapper and maps result`() {
        val today = LocalDate.of(2026, 4, 27)
        val record = TrackedLimitedStatSetRecord("BLB", LocalDate.of(2026, 5, 1), null, null)
        given(sqlMapper.selectActive(today)).willReturn(listOf(record))

        val result = repository.findActive(today)

        assertEquals(listOf(TrackedLimitedStatSet("BLB", LocalDate.of(2026, 5, 1))), result)
    }

    @Test
    fun `deleteBySetCode delegates to mapper`() {
        repository.deleteBySetCode("DMU")

        then(sqlMapper).should().deleteBySetCode("DMU")
    }
}
