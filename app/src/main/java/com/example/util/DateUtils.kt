package com.example.util

import com.example.domain.model.AppLanguage
import java.util.Calendar

data class DayMonthYear(
    val day: Int,
    val month: Int,
    val year: Int
)

object DateUtils {
    private val odiaMonths = listOf(
        "ଜାନୁଆରୀ", "ଫେବୃଆରୀ", "ମାର୍ଚ୍ଚ", "ଅପ୍ରେଲ",
        "ମଇ", "ଜୁନ", "ଜୁଲାଇ", "ଅଗଷ୍ଟ",
        "ସେପ୍ଟେମ୍ବର", "ଅକ୍ଟୋବର", "ନଭେମ୍ବର", "ଡିସେମ୍ବର"
    )

    private val englishMonths = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun today(): DayMonthYear {
        val cal = Calendar.getInstance()
        return DayMonthYear(
            day = cal.get(Calendar.DAY_OF_MONTH),
            month = cal.get(Calendar.MONTH) + 1,
            year = cal.get(Calendar.YEAR)
        )
    }

    fun formatDate(day: Int, month: Int, year: Int, lang: AppLanguage): String {
        val safeMonthIndex = (month - 1).coerceIn(0, 11)
        val monthName = if (lang == AppLanguage.ODIA) odiaMonths[safeMonthIndex] else englishMonths[safeMonthIndex]
        return if (lang == AppLanguage.ODIA) {
            "$day $monthName $year"
        } else {
            "$day $monthName $year"
        }
    }

    fun formatNumeric(day: Int, month: Int, year: Int): String {
        val d = day.toString().padStart(2, '0')
        val m = month.toString().padStart(2, '0')
        return "$d/$m/$year"
    }

    fun monthLabel(month: Int, lang: AppLanguage): String {
        val idx = (month - 1).coerceIn(0, 11)
        return if (lang == AppLanguage.ODIA) odiaMonths[idx] else englishMonths[idx]
    }

    fun daysInMonth(month: Int, year: Int): Int {
        return when (month) {
            2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}
