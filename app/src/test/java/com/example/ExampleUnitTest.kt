package com.example

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
}
