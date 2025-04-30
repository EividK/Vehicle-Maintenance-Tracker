package com.example.vehiclehealth.utils

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

fun addEventToCalendar(
    context: Context,
    title: String,
    description: String,
    location: String? = null,
    beginTime: Long,
    endTime: Long
) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, description)
        location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}
