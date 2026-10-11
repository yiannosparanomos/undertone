package io.github.yiannosparanomos.undertone.core.domain.scenario

import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Runs every scenario in [ScenarioCatalog] as its own test, named "<id> <title>". */
@RunWith(Parameterized::class)
class ScenarioTest(
    private val scenario: Scenario,
) {
    @Test
    fun behavesAsDescribed() {
        val failure = scenario.verify()
        assertNull("$scenario: $failure", failure)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun scenarios(): List<Array<Any>> = ScenarioCatalog.all.map { arrayOf<Any>(it) }
    }
}
