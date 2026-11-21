package com.example

import com.example.data.model.AppUpdateConfig
import com.example.data.model.ReferralConfig
import com.example.data.model.StaffRole
import com.example.data.model.Tournament
import com.example.utils.ArenaFormatters
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun registrationWindow_beforeOpen_blocksRegistration() {
    val t = Tournament(
      status = "upcoming",
      maxSlots = 100L,
      filledSlots = 10L,
      registrationOpenAtMillis = 2000L,
      registrationCloseAtMillis = 5000L
    )
    assertTrue(t.isRegistrationNotStarted(1000L))
    assertFalse(t.isRegistrationClosed(1000L))
    assertFalse(t.isRegistrationOpen(1000L))
  }

  @Test
  fun registrationWindow_betweenOpenAndClose_allowsRegistration() {
    val t = Tournament(
      status = "upcoming",
      maxSlots = 100L,
      filledSlots = 10L,
      registrationOpenAtMillis = 2000L,
      registrationCloseAtMillis = 5000L
    )
    assertFalse(t.isRegistrationNotStarted(3000L))
    assertFalse(t.isRegistrationClosed(3000L))
    assertTrue(t.isRegistrationOpen(3000L))
  }

  @Test
  fun registrationWindow_afterClose_blocksRegistration() {
    val t = Tournament(
      status = "upcoming",
      maxSlots = 100L,
      filledSlots = 10L,
      registrationOpenAtMillis = 2000L,
      registrationCloseAtMillis = 5000L
    )
    assertFalse(t.isRegistrationNotStarted(5000L))
    assertTrue(t.isRegistrationClosed(5000L))
    assertFalse(t.isRegistrationOpen(5000L))
  }

  @Test
  fun countdownFormatter_formatsHoursMinutesSeconds() {
    val now = 1_000_000L
    assertEquals("1h 5m 30s", ArenaFormatters.formatCountdown(now + 3930_000L, now))
    assertEquals("00s", ArenaFormatters.formatCountdown(now - 1000L, now))
  }

  @Test
  fun referralConfig_buildInviteLink_supportsWebsiteFormats() {
    assertEquals(
      "ahxanarena://register?ref=TEAM7",
      ReferralConfig(websiteBaseUrl = "").buildInviteLink("TEAM7")
    )
    assertEquals(
      "https://ahxanarena.com?ref=TEAM7",
      ReferralConfig(websiteBaseUrl = "https://ahxanarena.com").buildInviteLink("TEAM7")
    )
    assertEquals(
      "https://ahxanarena.com?ref=TEAM7",
      ReferralConfig(websiteBaseUrl = "ahxanarena.com").buildInviteLink("TEAM7")
    )
    assertEquals(
      "https://ahxanarena.com/join?src=app&ref=TEAM7",
      ReferralConfig(websiteBaseUrl = "https://ahxanarena.com/join?src=app").buildInviteLink("TEAM7")
    )
    assertEquals(
      "https://ahxanarena.com/r/TEAM7",
      ReferralConfig(websiteBaseUrl = "https://ahxanarena.com/r/{CODE}").buildInviteLink("TEAM7")
    )
  }

  @Test
  fun appUpdateConfig_blocksOlderBuildsOnlyWhenForceEnabledAndUrlPresent() {
    val active = AppUpdateConfig(
      forceUpdateEnabled = true,
      latestVersionCode = 2L,
      latestVersionName = "2.0",
      updateDownloadUrl = "ahxanarena.com/latest.apk"
    )
    assertTrue(active.isUpdateRequired(currentVersionCode = 1))
    assertFalse(active.isUpdateRequired(currentVersionCode = 2))
    assertFalse(active.isUpdateRequired(currentVersionCode = 3))
    assertEquals("https://ahxanarena.com/latest.apk", active.normalizedDownloadUrl())

    val disabled = active.copy(forceUpdateEnabled = false)
    assertFalse(disabled.isUpdateRequired(currentVersionCode = 1))

    val missingUrl = active.copy(updateDownloadUrl = "   ")
    assertFalse(missingUrl.isUpdateRequired(currentVersionCode = 1))
  }

  @Test
  fun staffRole_permissionHelpersTrackExactEnabledOptions() {
    val empty = StaffRole(userId = "u1", email = "staff@example.com")
    assertFalse(empty.hasAnyPermission())
    assertTrue(empty.grantedPermissionLabels().isEmpty())

    val roomAndTickets = empty.copy(publishRooms = true, replyTickets = true)
    assertTrue(roomAndTickets.hasAnyPermission())
    assertEquals(listOf("Publish Rooms", "Reply Tickets"), roomAndTickets.grantedPermissionLabels())
  }
}
