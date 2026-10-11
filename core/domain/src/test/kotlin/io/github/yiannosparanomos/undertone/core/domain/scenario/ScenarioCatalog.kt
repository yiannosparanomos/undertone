package io.github.yiannosparanomos.undertone.core.domain.scenario

import io.github.yiannosparanomos.undertone.core.domain.engine.CallOutcome
import io.github.yiannosparanomos.undertone.core.domain.engine.DecisionReason
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationOutcome
import io.github.yiannosparanomos.undertone.core.domain.engine.PastCall
import io.github.yiannosparanomos.undertone.core.domain.model.AppRule
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.domain.model.Override
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.RepeatCallerSettings
import io.github.yiannosparanomos.undertone.core.domain.model.RepeatGroup
import java.time.Duration

/**
 * Every behaviour UnderTone promises, as runnable scenarios.
 * IDs are stable: docs/testing/test-matrix.md maps each one to its test layer.
 *
 * P = profiles, L = levels and batches, H = hours, C = calendar, O = overrides, K = calls.
 */
object ScenarioCatalog {
    private val profiles =
        listOf(
            NotificationScenario(
                id = "P1",
                title = "Allow all lets a Teams notification through on Saturday",
                state = myRules.copy(profile = Profile.ALLOW_ALL),
                packageName = Apps.TEAMS,
                at = Week.SAT.at("11:00"),
                expect = NotificationOutcome.PASS_THROUGH,
                expectReason = DecisionReason.ALLOW_ALL_PROFILE,
            ),
            NotificationScenario(
                id = "P2",
                title = "System DND profile leaves notifications to Android",
                state = myRules.copy(profile = Profile.SYSTEM_DND),
                packageName = Apps.MESSENGER,
                at = Week.MON.at("23:30"),
                expect = NotificationOutcome.PASS_THROUGH,
                expectReason = DecisionReason.SYSTEM_DND_PROFILE,
            ),
            CallScenario(
                id = "P3",
                title = "Allow all leaves an unknown caller to the phone's normal ringing",
                state = myRules.copy(profile = Profile.ALLOW_ALL).withTier(CallTier.NOBODY),
                call = Callers.unknown(Week.MON.at("14:00")),
                expect = CallOutcome.PASS_THROUGH,
            ),
        )

    private val levels =
        listOf(
            NotificationScenario(
                id = "L1",
                title = "An app nobody configured follows the All apps default (Ring)",
                state = myRules,
                packageName = Apps.UNCONFIGURED,
                at = Week.MON.at("12:00"),
                expect = NotificationOutcome.RING,
                expectReason = DecisionReason.ALL_APPS_DEFAULT,
            ),
            NotificationScenario(
                id = "L2",
                title = "Changing the All apps default to Silent quiets unconfigured apps",
                state =
                    myRules.copy(
                        settings =
                            referenceSettings.copy(
                                defaultRule = AppRule(AppRule.ALL_APPS, inHours = Level.SILENT),
                            ),
                    ),
                packageName = Apps.UNCONFIGURED,
                at = Week.MON.at("12:00"),
                expect = NotificationOutcome.SILENT,
                expectReason = DecisionReason.ALL_APPS_DEFAULT,
            ),
            NotificationScenario(
                id = "L3",
                title = "A focus app with no hours rings at 3 in the morning",
                state = myRules,
                packageName = Apps.MESSENGER,
                at = Week.TUE.at("03:00"),
                expect = NotificationOutcome.RING,
                expectReason = DecisionReason.APP_SCHEDULE,
            ),
            NotificationScenario(
                id = "L4",
                title = "A blocked app is dismissed",
                state = myRules,
                packageName = Apps.SPAM_SHOP,
                at = Week.MON.at("12:00"),
                expect = NotificationOutcome.BLOCKED,
                expectReason = DecisionReason.BLOCKED_APP,
            ),
            NotificationScenario(
                id = "L5",
                title = "A blocked app stays blocked during Let everything through",
                state = myRules.copy(override = Override.LetThrough(until = Week.MON.at("13:00"))),
                packageName = Apps.SPAM_SHOP,
                at = Week.MON.at("12:30"),
                expect = NotificationOutcome.BLOCKED,
                expectReason = DecisionReason.BLOCKED_APP,
            ),
            NotificationScenario(
                id = "L6",
                title = "A blocked app stays blocked even when a calendar rule lets it ring",
                state = myRules.copy(events = listOf(Events.openHouse(Week.SAT))),
                packageName = Apps.SPAM_SHOP,
                at = Week.SAT.at("11:00"),
                expect = NotificationOutcome.BLOCKED,
                expectReason = DecisionReason.BLOCKED_APP,
            ),
        )

    private val hours =
        listOf(
            NotificationScenario(
                id = "H1",
                title = "Teams rings on Monday at 10:00, inside work hours",
                state = myRules,
                packageName = Apps.TEAMS,
                at = Week.MON.at("10:00"),
                expect = NotificationOutcome.RING,
            ),
            NotificationScenario(
                id = "H2",
                title = "Teams at exactly 18:00 is outside hours and held until 09:00 next day",
                state = myRules,
                packageName = Apps.TEAMS,
                at = Week.MON.at("18:00"),
                expect = NotificationOutcome.HIDDEN,
                expectReleaseAt = Week.TUE.at("09:00"),
            ),
            NotificationScenario(
                id = "H3",
                title = "Teams on Saturday is held until Monday 09:00",
                state = myRules,
                packageName = Apps.TEAMS,
                at = Week.SAT.at("11:00"),
                expect = NotificationOutcome.HIDDEN,
                expectReleaseAt = Week.NEXT_MON.at("09:00"),
            ),
            NotificationScenario(
                id = "H4",
                title = "Teams on Friday at 17:59 still rings",
                state = myRules,
                packageName = Apps.TEAMS,
                at = Week.FRI.at("17:59"),
                expect = NotificationOutcome.RING,
            ),
            NotificationScenario(
                id = "H5",
                title = "Teams late on Sunday is held until Monday 09:00",
                state = myRules,
                packageName = Apps.TEAMS,
                at = Week.SUN.at("23:30"),
                expect = NotificationOutcome.HIDDEN,
                expectReleaseAt = Week.NEXT_MON.at("09:00"),
            ),
            NotificationScenario(
                id = "H6",
                title = "Teams at 08:59 on a weekday is held one minute",
                state = myRules,
                packageName = Apps.TEAMS,
                at = Week.MON.at("08:59"),
                expect = NotificationOutcome.HIDDEN,
                expectReleaseAt = Week.MON.at("09:00"),
            ),
            NotificationScenario(
                id = "H7",
                title = "Overnight window: Instagram at 23:30 is held until 07:00",
                state = myRules,
                packageName = Apps.INSTAGRAM,
                at = Week.MON.at("23:30"),
                expect = NotificationOutcome.HIDDEN,
                expectReleaseAt = Week.TUE.at("07:00"),
            ),
            NotificationScenario(
                id = "H8",
                title = "Overnight window: Instagram at 06:59 is still held until 07:00",
                state = myRules,
                packageName = Apps.INSTAGRAM,
                at = Week.TUE.at("06:59"),
                expect = NotificationOutcome.HIDDEN,
                expectReleaseAt = Week.TUE.at("07:00"),
            ),
            NotificationScenario(
                id = "H9",
                title = "Overnight window: Instagram at 22:59 rings",
                state = myRules,
                packageName = Apps.INSTAGRAM,
                at = Week.MON.at("22:59"),
                expect = NotificationOutcome.RING,
            ),
        )

    private val calendar =
        listOf(
            NotificationScenario(
                id = "C1",
                title = "During the weekly standup, Messenger is silent",
                state = myRules.copy(events = listOf(Events.standup(Week.MON))),
                packageName = Apps.MESSENGER,
                at = Week.MON.at("10:05"),
                expect = NotificationOutcome.SILENT,
                expectReason = DecisionReason.CALENDAR_RULE,
            ),
            NotificationScenario(
                id = "C2",
                title = "During the weekly standup, the dialer still rings",
                state = myRules.copy(events = listOf(Events.standup(Week.MON))),
                packageName = Apps.DIALER,
                at = Week.MON.at("10:05"),
                expect = NotificationOutcome.RING,
            ),
            NotificationScenario(
                id = "C3",
                title = "When the standup ends, Messenger rings again",
                state = myRules.copy(events = listOf(Events.standup(Week.MON))),
                packageName = Apps.MESSENGER,
                at = Week.MON.at("10:30"),
                expect = NotificationOutcome.RING,
                expectReason = DecisionReason.APP_SCHEDULE,
            ),
            NotificationScenario(
                id = "C4",
                title = "During the standup, Teams in work hours is silent",
                state = myRules.copy(events = listOf(Events.standup(Week.MON))),
                packageName = Apps.TEAMS,
                at = Week.MON.at("10:10"),
                expect = NotificationOutcome.SILENT,
                expectReason = DecisionReason.CALENDAR_RULE,
            ),
            NotificationScenario(
                id = "C5",
                title = "A calendar rule cannot loosen a stricter schedule (Teams stays held on Saturday)",
                state = myRules.copy(events = listOf(Events.openHouse(Week.SAT))),
                packageName = Apps.TEAMS,
                at = Week.SAT.at("11:00"),
                expect = NotificationOutcome.HIDDEN,
                expectReason = DecisionReason.APP_SCHEDULE,
                expectReleaseAt = Week.NEXT_MON.at("09:00"),
            ),
            NotificationScenario(
                id = "C6",
                title = "A title-matched focus block holds Messenger until the block ends",
                state = myRules.copy(events = listOf(Events.focus(Week.MON))),
                packageName = Apps.MESSENGER,
                at = Week.MON.at("11:00"),
                expect = NotificationOutcome.HIDDEN,
                expectReason = DecisionReason.CALENDAR_RULE,
                expectReleaseAt = Week.MON.at("12:00"),
            ),
            NotificationScenario(
                id = "C7",
                title = "All-day events are ignored by default (a vacation entry doesn't quiet the day)",
                state = myRules.copy(events = listOf(Events.vacationAllDay(Week.TUE))),
                packageName = Apps.MESSENGER,
                at = Week.TUE.at("15:00"),
                expect = NotificationOutcome.RING,
            ),
            NotificationScenario(
                id = "C8",
                title = "Overlapping standup and focus block: the stricter (hidden) wins",
                state = myRules.copy(events = listOf(Events.standup(Week.MON), Events.focus(Week.MON))),
                packageName = Apps.MESSENGER,
                at = Week.MON.at("10:20"),
                expect = NotificationOutcome.HIDDEN,
                expectReason = DecisionReason.CALENDAR_RULE,
                expectReleaseAt = Week.MON.at("12:00"),
            ),
        )

    private val overrides =
        listOf(
            NotificationScenario(
                id = "O1",
                title = "Quiet for 1 hour silences Messenger",
                state = myRules.copy(override = Override.Quiet(until = Week.MON.at("13:00"))),
                packageName = Apps.MESSENGER,
                at = Week.MON.at("12:15"),
                expect = NotificationOutcome.SILENT,
                expectReason = DecisionReason.MANUAL_OVERRIDE,
            ),
            NotificationScenario(
                id = "O2",
                title = "Quiet for 1 hour can keep chosen apps ringing",
                state =
                    myRules.copy(
                        override = Override.Quiet(until = Week.MON.at("13:00"), ringApps = setOf(Apps.DIALER)),
                    ),
                packageName = Apps.DIALER,
                at = Week.MON.at("12:15"),
                expect = NotificationOutcome.RING,
                expectReason = DecisionReason.MANUAL_OVERRIDE,
            ),
            NotificationScenario(
                id = "O3",
                title = "An override ends by itself",
                state = myRules.copy(override = Override.Quiet(until = Week.MON.at("13:00"))),
                packageName = Apps.MESSENGER,
                at = Week.MON.at("13:00"),
                expect = NotificationOutcome.RING,
                expectReason = DecisionReason.APP_SCHEDULE,
            ),
            NotificationScenario(
                id = "O4",
                title = "Let everything through beats the schedule (Teams rings on Saturday)",
                state = myRules.copy(override = Override.LetThrough(until = Week.SAT.at("12:00"))),
                packageName = Apps.TEAMS,
                at = Week.SAT.at("11:30"),
                expect = NotificationOutcome.RING,
                expectReason = DecisionReason.MANUAL_OVERRIDE,
            ),
            NotificationScenario(
                id = "O5",
                title = "A hiding override releases Gmail when the override ends",
                state = myRules.copy(override = Override.Quiet(until = Week.MON.at("11:00"), level = Level.HIDDEN)),
                packageName = Apps.GMAIL,
                at = Week.MON.at("10:00"),
                expect = NotificationOutcome.HIDDEN,
                expectReason = DecisionReason.MANUAL_OVERRIDE,
                expectReleaseAt = Week.MON.at("11:00"),
            ),
            NotificationScenario(
                id = "O6",
                title = "A manual override beats a calendar rule",
                state =
                    myRules.copy(
                        events = listOf(Events.focus(Week.MON)),
                        override = Override.LetThrough(until = Week.MON.at("11:30")),
                    ),
                packageName = Apps.MESSENGER,
                at = Week.MON.at("11:00"),
                expect = NotificationOutcome.RING,
                expectReason = DecisionReason.MANUAL_OVERRIDE,
            ),
        )

    private val familyRepeat = PastCall(Callers.FAMILY, Week.MON.at("14:00"))

    private val calls =
        listOf(
            CallScenario(
                id = "K1",
                title = "Tier Contacts: a saved contact rings",
                state = myRules,
                call = Callers.colleague(Week.MON.at("14:00")),
                expect = CallOutcome.RING,
                expectReason = DecisionReason.RING_TIER,
            ),
            CallScenario(
                id = "K2",
                title = "Tier Contacts: an unknown number is silent",
                state = myRules,
                call = Callers.unknown(Week.MON.at("14:00")),
                expect = CallOutcome.SILENT,
                expectReason = DecisionReason.TIER_SILENCED,
            ),
            CallScenario(
                id = "K3",
                title = "Tier Allow list: a contact not on the list is silent",
                state = myRules.withTier(CallTier.ALLOW_LIST),
                call = Callers.colleague(Week.MON.at("14:00")),
                expect = CallOutcome.SILENT,
            ),
            CallScenario(
                id = "K4",
                title = "Tier Allow list: someone on the list rings",
                state = myRules.withTier(CallTier.ALLOW_LIST),
                call = Callers.family(Week.MON.at("14:00")),
                expect = CallOutcome.RING,
                expectReason = DecisionReason.RING_TIER,
            ),
            CallScenario(
                id = "K5",
                title = "Tier Nobody: a single call from the allow list is silent",
                state = myRules.withTier(CallTier.NOBODY),
                call = Callers.family(Week.MON.at("14:00")),
                expect = CallOutcome.SILENT,
                expectReason = DecisionReason.TIER_SILENCED,
            ),
            CallScenario(
                id = "K6",
                title = "Tier Nobody: the allow list breaks through by calling again within 2 minutes",
                state = myRules.withTier(CallTier.NOBODY),
                call = Callers.family(Week.MON.at("14:01:30")),
                earlierCalls = listOf(familyRepeat),
                expect = CallOutcome.RING,
                expectReason = DecisionReason.REPEAT_CALLER,
            ),
            CallScenario(
                id = "K7",
                title = "Tier Nobody: a second call after 3 minutes is too late (window 2 min)",
                state = myRules.withTier(CallTier.NOBODY),
                call = Callers.family(Week.MON.at("14:03")),
                earlierCalls = listOf(familyRepeat),
                expect = CallOutcome.SILENT,
            ),
            CallScenario(
                id = "K8",
                title = "Repeat group Allow list: a contact not on the list can't break through",
                state = myRules.withTier(CallTier.NOBODY),
                call = Callers.colleague(Week.MON.at("14:01")),
                earlierCalls = listOf(PastCall(Callers.COLLEAGUE, Week.MON.at("14:00"))),
                expect = CallOutcome.SILENT,
            ),
            CallScenario(
                id = "K9",
                title = "Repeat group Everyone: an unknown number calling twice rings",
                state =
                    myRules.withTier(CallTier.NOBODY).let {
                        it.copy(
                            settings =
                                it.settings.copy(
                                    repeatCallers = RepeatCallerSettings(group = RepeatGroup.EVERYONE),
                                ),
                        )
                    },
                call = Callers.unknown(Week.MON.at("14:01")),
                earlierCalls = listOf(PastCall(Callers.UNKNOWN, Week.MON.at("14:00"))),
                expect = CallOutcome.RING,
                expectReason = DecisionReason.REPEAT_CALLER,
            ),
            CallScenario(
                id = "K10",
                title = "Calls needed 3: two calls are not enough",
                state =
                    myRules.withTier(CallTier.NOBODY).let {
                        it.copy(settings = it.settings.copy(repeatCallers = RepeatCallerSettings(callsNeeded = 3)))
                    },
                call = Callers.family(Week.MON.at("14:01")),
                earlierCalls = listOf(familyRepeat),
                expect = CallOutcome.SILENT,
            ),
            CallScenario(
                id = "K11",
                title = "Calls needed 3: the third call within the window rings",
                state =
                    myRules.withTier(CallTier.NOBODY).let {
                        it.copy(settings = it.settings.copy(repeatCallers = RepeatCallerSettings(callsNeeded = 3)))
                    },
                call = Callers.family(Week.MON.at("14:01:40")),
                earlierCalls = listOf(familyRepeat, PastCall(Callers.FAMILY, Week.MON.at("14:00:50"))),
                expect = CallOutcome.RING,
                expectReason = DecisionReason.REPEAT_CALLER,
            ),
            CallScenario(
                id = "K12",
                title = "Window 5 minutes: a second call after 4 minutes rings",
                state =
                    myRules.withTier(CallTier.NOBODY).let {
                        it.copy(
                            settings =
                                it.settings.copy(
                                    repeatCallers = RepeatCallerSettings(window = Duration.ofMinutes(5)),
                                ),
                        )
                    },
                call = Callers.family(Week.MON.at("14:04")),
                earlierCalls = listOf(familyRepeat),
                expect = CallOutcome.RING,
            ),
            CallScenario(
                id = "K13",
                title = "Repeat callers switched off: a second call stays silent",
                state =
                    myRules.withTier(CallTier.NOBODY).let {
                        it.copy(settings = it.settings.copy(repeatCallers = RepeatCallerSettings(enabled = false)))
                    },
                call = Callers.family(Week.MON.at("14:01")),
                earlierCalls = listOf(familyRepeat),
                expect = CallOutcome.SILENT,
            ),
            CallScenario(
                id = "K14",
                title = "Numbers match regardless of spaces and formatting",
                state = myRules.withTier(CallTier.NOBODY),
                call = Callers.family(Week.MON.at("14:01")),
                earlierCalls = listOf(PastCall("+306900000001", Week.MON.at("14:00"))),
                expect = CallOutcome.RING,
            ),
            CallScenario(
                id = "K15",
                title = "A call exactly at the edge of the window still counts",
                state = myRules.withTier(CallTier.NOBODY),
                call = Callers.family(Week.MON.at("14:02")),
                earlierCalls = listOf(familyRepeat),
                expect = CallOutcome.RING,
            ),
            CallScenario(
                id = "K16",
                title = "During the standup the tier tightens to Allow list (a contact is silent)",
                state = myRules.copy(events = listOf(Events.standup(Week.MON))),
                call = Callers.colleague(Week.MON.at("10:10")),
                expect = CallOutcome.SILENT,
            ),
            CallScenario(
                id = "K17",
                title = "A calendar rule cannot loosen the tier (unknown caller still silent)",
                state = myRules.copy(events = listOf(Events.openHouse(Week.SAT))),
                call = Callers.unknown(Week.SAT.at("11:00")),
                expect = CallOutcome.SILENT,
            ),
            CallScenario(
                id = "K18",
                title = "Quiet for 1 hour: the allow list rings, other contacts don't",
                state = myRules.copy(override = Override.Quiet(until = Week.MON.at("15:00"))),
                call = Callers.colleague(Week.MON.at("14:30")),
                expect = CallOutcome.SILENT,
            ),
            CallScenario(
                id = "K19",
                title = "Let everything through: an unknown number rings",
                state = myRules.copy(override = Override.LetThrough(until = Week.MON.at("15:00"))),
                call = Callers.unknown(Week.MON.at("14:30")),
                expect = CallOutcome.RING,
                expectReason = DecisionReason.RING_TIER,
            ),
        )

    val all: List<Scenario> = profiles + levels + hours + calendar + overrides + calls
}
