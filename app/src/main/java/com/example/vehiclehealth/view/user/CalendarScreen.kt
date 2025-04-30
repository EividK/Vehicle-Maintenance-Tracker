package com.example.vehiclehealth.view.user

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.vehiclehealth.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import com.google.api.services.calendar.CalendarScopes
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var googleAccount by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    var eventsByDate by remember { mutableStateOf<Map<LocalDate, List<String>>>(emptyMap()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        coroutineScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                googleAccount = task.getResult(ApiException::class.java)
                Toast.makeText(context, "Signed in successfully", Toast.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                Log.e("CalendarScreen", "Google sign-in error", e)
                Toast.makeText(context, "Google sign-in failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(googleAccount) {
        if (googleAccount == null) {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(CalendarScopes.CALENDAR), Scope(CalendarScopes.CALENDAR_EVENTS))
                .build()
            val client = GoogleSignIn.getClient(context, options)
            signInLauncher.launch(client.signInIntent)
        }
    }

    LaunchedEffect(googleAccount, currentMonth) {
        googleAccount?.let { acct ->
            val cred = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(CalendarScopes.CALENDAR)
            ).apply { selectedAccount = acct.account }
            val service = Calendar.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory(),
                cred
            )
                .setApplicationName(context.packageName)
                .build()

            val startMillis = currentMonth.atDay(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val endMillis = currentMonth.plusMonths(1).atDay(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()

            val items = try {
                withContext(Dispatchers.IO) {
                    service.events().list("primary")
                        .setTimeMin(DateTime(startMillis))
                        .setTimeMax(DateTime(endMillis))
                        .setSingleEvents(true)
                        .execute()
                        .items
                }
            } catch (sec: SecurityException) {
                Log.e("CalendarScreen", "Calendar broker SecurityException", sec)
                emptyList()
            } catch (io: Exception) {
                Log.e("CalendarScreen", "Error fetching calendar events", io)
                emptyList()
            }

            eventsByDate = items
                .mapNotNull { ev ->
                    val millis = ev.start?.dateTime?.value ?: ev.start?.date?.value
                    val title  = ev.summary ?: "(No title)"
                    millis?.let {
                        Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate() to title
                    }
                }
                .groupBy({ it.first }, { it.second })
        }
    }

    LaunchedEffect(Unit) {
        createNotificationChannel(context)
    }

    Scaffold(
        containerColor = Color(0xFF1E1D2B),
        topBar = {
            SmallTopAppBar(
                title = { Text("My Service Calendar", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF2A293D))
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1D2B))
                .padding(padding)
        ) {
            MonthHeader(
                month = currentMonth,
                onPrevious = { currentMonth = currentMonth.minusMonths(1) },
                onNext     = { currentMonth = currentMonth.plusMonths(1) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            MonthGrid(
                yearMonth     = currentMonth,
                eventsByDate  = eventsByDate,
                onDateClick   = { date ->
                    selectedDate = date
                    showAddDialog = true
                }
            )
        }
    }

    if (showAddDialog && selectedDate != null) {
        AddEventDialog(
            date = selectedDate!!,
            onDismiss = { showAddDialog = false },
            onAddEvent = { title, reminderMins ->
                googleAccount?.let { acct ->
                    val cred = GoogleAccountCredential.usingOAuth2(
                        context,
                        listOf(CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_EVENTS)
                    ).apply { selectedAccount = acct.account }

                    val service = Calendar.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        GsonFactory(),
                        cred
                    )
                        .setApplicationName(context.packageName)
                        .build()

                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val startMs = selectedDate!!.atTime(9, 0)
                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            val endMs = selectedDate!!.atTime(10, 0)
                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                            val event = Event().apply {
                                summary = title
                                start = EventDateTime().setDateTime(DateTime(startMs))
                                end   = EventDateTime().setDateTime(DateTime(endMs))
                                reminders = Event.Reminders().setUseDefault(false).setOverrides(
                                    listOf(EventReminder().setMethod("popup").setMinutes(reminderMins))
                                )
                            }

                            service.events().insert("primary", event).execute()

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Event added to Google Calendar", Toast.LENGTH_SHORT).show()
                                scheduleNotification(context, title, selectedDate!!, reminderMins)
                                currentMonth = currentMonth.plusMonths(0)
                            }

                        } catch (e: Exception) {
                            Log.e("CalendarScreen", "Failed to add event: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error adding event: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } ?: run {
                    Toast.makeText(context, "Google account not signed in.", Toast.LENGTH_LONG).show()
                }
                showAddDialog = false
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddEventDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onAddEvent: (title: String, reminderMins: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var reminderText by remember { mutableStateOf("60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Event") },
        text = {
            Column {
                Text("Date: ${date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = reminderText,
                    onValueChange = { reminderText = it.filter { c -> c.isDigit() } },
                    label = { Text("Reminder (minutes before)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val mins = reminderText.toIntOrNull() ?: 60
                onAddEvent(title, mins)
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@SuppressLint("ScheduleExactAlarm")
@RequiresApi(Build.VERSION_CODES.O)
fun scheduleNotification(context: Context, title: String, date: LocalDate, reminderMins: Int) {
    val triggerMillis = date.atTime(9, 0)
        .minusMinutes(reminderMins.toLong())
        .atZone(ZoneId.systemDefault())
        .toInstant().toEpochMilli()
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("title", title)
        putExtra("requestCode", date.hashCode())
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        date.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Event Reminder"
        val channelId = "calendar_reminders"
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Reminder")
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_calendar)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("NotificationReceiver", "POST_NOTIFICATIONS permission not granted.")
                return
            }
        }

        NotificationManagerCompat.from(context)
            .notify(intent.getIntExtra("requestCode", 0), notification)
    }
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Calendar Reminders"
        val descriptionText = "Reminders for calendar events"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("calendar_reminders", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthGrid(
    yearMonth: YearMonth,
    eventsByDate: Map<LocalDate, List<String>>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstOfMonth = yearMonth.atDay(1)
    val leadEmpty = firstOfMonth.dayOfWeek.value % 7

    val cells = mutableListOf<LocalDate?>()
    repeat(leadEmpty) { cells.add(null) }
    for (day in 1..yearMonth.lengthOfMonth()) {
        cells.add(yearMonth.atDay(day))
    }
    while (cells.size < 42) {
        cells.add(null)
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEach { wd ->
            Text(wd, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }

    Spacer(Modifier.height(4.dp))

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement   = Arrangement.spacedBy(2.dp)
    ) {
        items(cells) { date ->
            if (date != null) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(Color(0xFF2A293D), shape = MaterialTheme.shapes.small)
                        .clickable { onDateClick(date) }
                        .padding(4.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text  = date.dayOfMonth.toString(),
                        color = if (date == LocalDate.now()) Color.Cyan else Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    eventsByDate[date]?.let { list ->
                        if (list.isNotEmpty()) {
                            Text(
                                text     = list.size.toString(),
                                modifier = Modifier.align(Alignment.BottomCenter),
                                style    = MaterialTheme.typography.bodySmall,
                                color    = Color.Yellow
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.aspectRatio(1f)) {}
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("<", color = Color.White, modifier = Modifier.clickable(onClick = onPrevious))
        Text(
            text  = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Text(">", color = Color.White, modifier = Modifier.clickable(onClick = onNext))
    }
}
