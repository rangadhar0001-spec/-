package com.example.util

import com.example.domain.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `daysInMonth handles leap years and standard months correctly`() {
        assertEquals(29, DateUtils.daysInMonth(2, 2024))
        assertEquals(28, DateUtils.daysInMonth(2, 2025))
        assertEquals(29, DateUtils.daysInMonth(2, 2000))
        assertEquals(28, DateUtils.daysInMonth(2, 1900))
        assertEquals(30, DateUtils.daysInMonth(4, 2025))
        assertEquals(31, DateUtils.daysInMonth(1, 2025))
    }

    @Test
    fun `formatNumeric pads day and month with leading zeros`() {
        assertEquals("05/03/2026", DateUtils.formatNumeric(5, 3, 2026))
        assertEquals("24/11/2026", DateUtils.formatNumeric(24, 11, 2026))
    }

    @Test
    fun `formatDate formats Odia and English months accurately`() {
        assertEquals("15 ମାର୍ଚ୍ଚ 2026", DateUtils.formatDate(15, 3, 2026, AppLanguage.ODIA))
        assertEquals("15 Mar 2026", DateUtils.formatDate(15, 3, 2026, AppLanguage.ENGLISH))
    }
}
