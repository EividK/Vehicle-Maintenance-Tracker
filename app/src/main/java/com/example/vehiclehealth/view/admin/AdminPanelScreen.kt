package com.example.vehiclehealth.view.admin

import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.security.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.Date

private data class ActionLog(val timestamp: Long, val description: String)
private enum class AdminSection { Main, Users, Vehicles, Announcements }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentSection by remember { mutableStateOf(AdminSection.Main) }

    var showLogsDialog by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<ActionLog>() }

    LaunchedEffect(showLogsDialog) {
        if (showLogsDialog) {
            logs.clear()
            db.collection("actionLogs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { snap ->
                    snap.documents.forEach { doc ->
                        val ts   = doc.getLong("timestamp") ?: 0L
                        val act  = doc.getString("action")    ?: "Unknown"
                        val why  = doc.getString("reason")    ?: doc.getString("targetEmail") ?: ""
                        val id   = doc.getString("targetId")  ?: ""
                        val info = doc.getString("vehicleInfo") ?: ""
                        val desc = when (act) {
                            "Promote", "Demote", "Suspend", "Unsuspend" ->
                                "$act user $id${if(why.isNotBlank()) ": $why" else ""}"
                            "DeleteUser" ->
                                "Deleted user $why"
                            "DeleteServiceHistory", "DeleteVehicle" ->
                                "$act on $info"
                            else -> "$act on $id $why"
                        }
                        logs.add(ActionLog(ts, desc))
                    }
                }
        }
    }

    val userRoles = remember { mutableStateMapOf<String, UserRole>() }
    val userProfiles = remember { mutableStateListOf<Pair<String, String>>() }
    val vehicles = remember { mutableStateListOf<Vehicle>() }

    LaunchedEffect(Unit) {
        db.collection("roles").get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { doc ->
                    val roleStr = doc.getString("role") ?: "USER"
                    userRoles[doc.id] = UserRole.valueOf(roleStr)
                }
            }
        db.collection("users").get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { doc ->
                    val email = doc.getString("email") ?: ""
                    userProfiles.add(doc.id to email)
                }
            }
        db.collectionGroup("vehicles").get()
            .addOnSuccessListener { snap ->
                vehicles.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(Vehicle::class.java)?.let { v ->
                        v.id = doc.id
                        vehicles.add(v)
                    }
                }
            }
    }

    val totalUsers = userRoles.size
    val adminCount = userRoles.count { it.value == UserRole.ADMIN }
    val userCount = userRoles.count { it.value == UserRole.USER }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Text(
                        when (currentSection) {
                            AdminSection.Main -> "Admin Panel"
                            AdminSection.Users -> "Manage Users"
                            AdminSection.Vehicles -> "Manage Vehicles"
                            AdminSection.Announcements -> "Announcements"
                        },
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentSection == AdminSection.Main) {
                            onBack()
                        } else {
                            currentSection = AdminSection.Main
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF2A293D))
            )
        },
        containerColor = Color(0xFF1E1D2B)
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(Color(0xFF1E1D2B)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Total Users", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Box(Modifier.height(120.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val total = totalUsers.coerceAtLeast(1)
                    val adminAngle = 360f * adminCount / total
                    drawArc(color = Color.Red, startAngle = 0f, sweepAngle = adminAngle, useCenter = true)
                    drawArc(color = Color.Green, startAngle = adminAngle, sweepAngle = 360f - adminAngle, useCenter = true)
                }
            }
            Text("Total Users: $totalUsers (Admins: $adminCount, Users: $userCount)", color = Color.Gray)

            Spacer(Modifier.height(24.dp))

            when (currentSection) {
                AdminSection.Main -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { currentSection = AdminSection.Users }
                    ) {
                        Text("View Users")
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { currentSection = AdminSection.Vehicles }
                    ) {
                        Text("View Vehicles")
                    }
                    Button(
                        onClick = { showLogsDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Logs")
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { currentSection = AdminSection.Announcements }
                    ) {
                        Text("Send Announcement")
                    }
                }
                AdminSection.Users -> {
                    UsersScreen(
                        db = db,
                        auth = auth,
                        coroutineScope = coroutineScope,
                        userProfiles = userProfiles,
                        userRoles = userRoles,
                        onBack = { currentSection = AdminSection.Main }
                    )
                }
                AdminSection.Vehicles -> {
                    VehiclesScreen(
                        db = db,
                        auth = auth,
                        coroutineScope = coroutineScope,
                        vehicles = vehicles,
                        onBack = { currentSection = AdminSection.Main }
                    )
                }

                AdminSection.Announcements -> {
                    AnnouncementsSection(
                        initialTopic = "announcements",
                        onSend = { topic, title, body ->
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user == null) {
                                Toast.makeText(context, "Please sign in first", Toast.LENGTH_SHORT).show()
                                return@AnnouncementsSection
                            }

                            val functions = Firebase.functions("us-central1")

                            functions
                                .getHttpsCallable("sendTopicMessage")
                                .call(
                                    mapOf(
                                        "topic" to topic,
                                        "title" to title,
                                        "body"  to body,
                                        "data"  to mapOf("type" to "announcement")
                                    )
                                )
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Sent to $topic!", Toast.LENGTH_SHORT).show()
                                    currentSection = AdminSection.Main
                                }
                                .addOnFailureListener { e ->
                                    when ((e as? FirebaseFunctionsException)?.code) {
                                        FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                                            Toast.makeText(context,
                                                "Not authenticated—please sign out and back in",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        FirebaseFunctionsException.Code.PERMISSION_DENIED -> {
                                            Toast.makeText(context,
                                                "You’re not an admin",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        else -> {
                                            Toast.makeText(context,
                                                "Error sending announcement: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                        },
                        onCancel = { currentSection = AdminSection.Main }
                    )
                }
            }
        }
    }

    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = { Text("Action Logs") },
            text = {
                Box(Modifier.height(300.dp).fillMaxWidth()) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(logs.sortedByDescending { it.timestamp }) { entry ->
                            val formatted = DateFormat.format("yyyy-MM-dd HH:mm", Date(entry.timestamp))
                            Text(
                                text = "$formatted — ${entry.description}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogsDialog = false }) {
                    Text("Close", color = Color.DarkGray)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsSection(
    initialTopic: String = "announcements",
    onSend: (topic: String, title: String, body: String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body  by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf(initialTopic) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "New Announcement",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(color = Color.White),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor          = Color.White,
                focusedBorderColor   = Color.Gray,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Body") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5,
            textStyle = LocalTextStyle.current.copy(color = Color.White),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor          = Color.White,
                focusedBorderColor   = Color.Gray,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Topic:", color = Color.White)
            Spacer(Modifier.width(16.dp))
            listOf("announcements", "feature_updates").forEach { t ->
                Row(
                    Modifier
                        .clickable { topic = t }
                        .padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (topic == t),
                        onClick  = { topic = t },
                        colors    = RadioButtonDefaults.colors(selectedColor = Color.White)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = t, color = Color.White)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick  = {
                    if (title.isBlank() || body.isBlank()) {
                        Toast.makeText(context, "Title and body cannot be empty", Toast.LENGTH_SHORT).show()
                    } else {
                        onSend(topic, title, body)
                    }
                }
            ) {
                Text("Send", color = Color.White)
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick  = onCancel,
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
            ) {
                Text("Cancel")
            }
        }
    }
}

