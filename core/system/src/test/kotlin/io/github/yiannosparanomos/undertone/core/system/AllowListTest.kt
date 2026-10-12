package io.github.yiannosparanomos.undertone.core.system

import io.github.yiannosparanomos.undertone.core.data.settings.AllowListDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The allow list's own rules (spec §5): starred contacts are always on it, and *expanded* adds
 * to that list without ever taking anyone off it.
 */
class AllowListTest {
    @Test
    fun `starred mode consults no numbers of its own`() {
        val list = AllowListDto(mode = AllowListDto.Mode.STARRED, numbers = listOf("+35799000000"))
        // The number is stored but ignored until the user switches to expanded.
        assertFalse(list.containsNumber("+35799000000"))
    }

    @Test
    fun `expanded mode matches a number the user added`() {
        val list = AllowListDto(mode = AllowListDto.Mode.EXPANDED, numbers = listOf("+35799000000"))
        assertTrue(list.containsNumber("+35799000000"))
    }

    @Test
    fun `numbers match on digits so formatting does not matter`() {
        val list = AllowListDto(mode = AllowListDto.Mode.EXPANDED, numbers = listOf("+30 210 123 4567"))
        assertTrue(list.containsNumber("+302101234567"))
        assertTrue(list.containsNumber("+30-210-123-4567"))
        assertFalse(list.containsNumber("+302101234568"))
    }

    @Test
    fun `a withheld number matches nothing`() {
        val list = AllowListDto(mode = AllowListDto.Mode.EXPANDED, numbers = listOf("+35799000000"))
        assertFalse(list.containsNumber(""))
        assertFalse(list.containsNumber("unknown"))
    }

    @Test
    fun `an empty expanded list allows nobody extra`() {
        assertFalse(AllowListDto(mode = AllowListDto.Mode.EXPANDED).containsNumber("+35799000000"))
    }
}
