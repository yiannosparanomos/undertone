package io.github.yiannosparanomos.undertone.core.domain.scenario

import io.github.yiannosparanomos.undertone.core.domain.engine.CallEngine
import io.github.yiannosparanomos.undertone.core.domain.engine.CallOutcome
import io.github.yiannosparanomos.undertone.core.domain.engine.DecisionReason
import io.github.yiannosparanomos.undertone.core.domain.engine.IncomingCall
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationEngine
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationOutcome
import io.github.yiannosparanomos.undertone.core.domain.engine.PastCall
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import java.time.LocalDateTime

/**
 * One behaviour of UnderTone, written so a person can read it and the engine can check it.
 * Every scenario has a stable [id] that docs/testing/test-matrix.md refers to.
 */
sealed interface Scenario {
    val id: String
    val title: String

    /** Returns null if the engine behaves as expected, otherwise a description of the mismatch. */
    fun verify(): String?
}

data class NotificationScenario(
    override val id: String,
    override val title: String,
    val state: RuleState,
    val packageName: String,
    val at: LocalDateTime,
    val expect: NotificationOutcome,
    val expectReason: DecisionReason? = null,
    val expectReleaseAt: LocalDateTime? = null,
) : Scenario {
    override fun verify(): String? {
        val decision = NotificationEngine.decide(packageName, at, state)
        val problems =
            buildList {
                if (decision.outcome != expect) add("outcome ${decision.outcome}, expected $expect")
                if (expectReason != null && decision.reason != expectReason) {
                    add("reason ${decision.reason}, expected $expectReason")
                }
                if (expectReleaseAt != null && decision.releaseAt != expectReleaseAt) {
                    add("releaseAt ${decision.releaseAt}, expected $expectReleaseAt")
                }
            }
        return problems.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    override fun toString() = "$id $title"
}

data class CallScenario(
    override val id: String,
    override val title: String,
    val state: RuleState,
    val call: IncomingCall,
    val earlierCalls: List<PastCall> = emptyList(),
    val expect: CallOutcome,
    val expectReason: DecisionReason? = null,
) : Scenario {
    override fun verify(): String? {
        val decision = CallEngine.decide(call, state, earlierCalls)
        val problems =
            buildList {
                if (decision.outcome != expect) add("outcome ${decision.outcome}, expected $expect")
                if (expectReason != null && decision.reason != expectReason) {
                    add("reason ${decision.reason}, expected $expectReason")
                }
            }
        return problems.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    override fun toString() = "$id $title"
}
