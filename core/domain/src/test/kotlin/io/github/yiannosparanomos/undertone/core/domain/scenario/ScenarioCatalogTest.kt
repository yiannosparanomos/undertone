package io.github.yiannosparanomos.undertone.core.domain.scenario

import org.junit.Assert.assertEquals
import org.junit.Test

class ScenarioCatalogTest {
    @Test
    fun scenarioIdsAreUnique() {
        val duplicates =
            ScenarioCatalog.all
                .groupBy { it.id }
                .filterValues { it.size > 1 }
                .keys
        assertEquals("Duplicate scenario ids", emptySet<String>(), duplicates)
    }
}
