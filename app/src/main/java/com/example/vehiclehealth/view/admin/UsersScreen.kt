package com.example.vehiclehealth.view.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vehiclehealth.security.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UsersScreen(
    userProfiles: SnapshotStateList<Pair<String, String>>,
    userRoles: SnapshotStateMap<String, UserRole>,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    coroutineScope: CoroutineScope,
    onBack: () -> Unit
) {
    val actorId = auth.currentUser?.uid ?: "unknown"
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    // suspension
    var showSuspendDialog by remember { mutableStateOf(false) }
    var suspendReason by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf<String?>(null) }

    // delete
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteUserId by remember { mutableStateOf<String?>(null) }
    var deleteEmail by remember { mutableStateOf("") }
    var deleteReason by remember { mutableStateOf("") }



    val suspensions = remember { mutableStateMapOf<String, Boolean>() }
    DisposableEffect(Unit) {
        val listener = db.collection("suspensions")
            .addSnapshotListener { snap, _ ->
                suspensions.clear()
                snap?.documents?.forEach { doc ->
                    suspensions[doc.id] = doc.getBoolean("suspended") == true
                }
            }
        onDispose { listener.remove() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search users") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(userProfiles.filter { it.second.contains(searchQuery, ignoreCase = true) }) { (uid, email) ->
                val role = userRoles[uid] ?: UserRole.USER
                val isSuspended = suspensions[uid] == true
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A293D))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(email, color = Color.White)
                        Text(
                            "Role: $role" + if (isSuspended) " (Suspended)" else "",
                            color = Color.Gray
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = { auth.sendPasswordResetEmail(email) },
                                enabled = !isSuspended
                            ) {
                                Text("Reset PW", color = if (isSuspended) Color.Gray else Color.Cyan)
                            }
                            TextButton(onClick = {
                                val newRole = if (role == UserRole.ADMIN) "USER" else "ADMIN"

                                db.collection("roles")
                                    .document(uid).set(mapOf("role" to newRole))
                                    .addOnSuccessListener {
                                        userRoles[uid] = UserRole.valueOf(newRole)
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to update role", Toast.LENGTH_SHORT).show()
                                    }

                                // action logging for admins
                                db.collection("actionLogs").add(
                                    mapOf(
                                        "timestamp" to System.currentTimeMillis(),
                                        "action"    to if (newRole=="ADMIN") "Promote" else "Demote",
                                        "targetId"  to uid,
                                        "targetEmail" to email,
                                        "actorId"   to actorId
                                    )
                                )
                            },
                                enabled = !isSuspended
                            ) {
                                Text(if (role == UserRole.ADMIN) "Demote" else "Promote", color = if (isSuspended) Color.Gray else Color.Yellow)
                            }
                            if (!isSuspended) {
                                TextButton(onClick = {
                                    selectedUserId = uid
                                    suspendReason = ""
                                    showSuspendDialog = true
                                }) {
                                    Text("Suspend", color = Color.Red)
                                }
                            } else {
                                TextButton(onClick = {
                                    // unsuspend
                                    coroutineScope.launch {
                                        db.collection("suspensions").document(uid)
                                            .delete()
                                            .addOnSuccessListener {
                                                suspensions[uid] = false
                                            }
                                        db.collection("actionLogs").add(
                                            mapOf(
                                                "timestamp"  to System.currentTimeMillis(),
                                                "action"     to "Unsuspend",
                                                "targetId"   to uid,
                                                "targetEmail" to email,
                                                "actorId"    to actorId
                                            )
                                        )
                                    }
                                }) {
                                    Text("Unsuspend", color = Color.Green)
                                }
                            }
                            TextButton(onClick = {
                                deleteUserId = uid
                                deleteEmail = email
                                deleteReason = ""
                                showDeleteDialog = true
                            }) {
                                Text("Delete", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Suspension dialog
    if (showSuspendDialog && selectedUserId != null) {
        AlertDialog(
            onDismissRequest = { showSuspendDialog = false },
            title = { Text("Suspension Reason") },
            text = {
                OutlinedTextField(
                    value = suspendReason,
                    onValueChange = { suspendReason = it },
                    label = { Text("Enter reason") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedUserId?.let { uid ->
                        coroutineScope.launch {
                            db.collection("suspensions").document(uid)
                                .set(mapOf(
                                    "uid" to uid,
                                    "suspended" to true,
                                    "reason" to suspendReason,
                                    "timestamp" to System.currentTimeMillis()
                                ))
                            showSuspendDialog = false
                            suspendReason = ""
                            selectedUserId = null
                        }
                    }
                }) {
                    Text("Confirm Suspension", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSuspendDialog = false
                    suspendReason = ""
                    selectedUserId = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }


    // Delete confirmation dialog
    if (showDeleteDialog && deleteUserId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Delete") },
            text = {
                Column {
                    Text(
                        "Are you sure you want to delete user \"$deleteEmail\"? This cannot be undone.",
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Reason for deletion") },
                        textStyle = LocalTextStyle.current.copy(color = Color.DarkGray),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uidToDelete    = deleteUserId!!
                    val emailToDelete  = deleteEmail
                    val reasonToLog    = deleteReason
                    showDeleteDialog   = false
                    deleteUserId       = null
                    deleteEmail        = ""
                    deleteReason       = ""

                    userProfiles.remove(uidToDelete to emailToDelete)

                    coroutineScope.launch {
                        try {
                            val vehSnap = db.collection("users")
                                .document(uidToDelete)
                                .collection("vehicles")
                                .get()
                                .await()
                            if (!vehSnap.isEmpty) {
                                val batchV = db.batch()
                                vehSnap.documents.forEach { batchV.delete(it.reference) }
                                batchV.commit().await()
                            }

                            val histSnap = db.collection("ServiceHistory")
                                .whereEqualTo("userId", uidToDelete)
                                .get()
                                .await()
                            if (!histSnap.isEmpty) {
                                val batchH = db.batch()
                                histSnap.documents.forEach { batchH.delete(it.reference) }
                                batchH.commit().await()
                            }

                            db.collection("roles").document(uidToDelete).delete().await()
                            db.collection("users").document(uidToDelete).delete().await()
                            db.collection("suspensions").document(uidToDelete).delete().await()

                            db.collection("actionLogs").add(
                                mapOf(
                                    "timestamp"    to System.currentTimeMillis(),
                                    "action"       to "DeleteUser",
                                    "targetId"     to uidToDelete,
                                    "targetEmail"  to emailToDelete,
                                    "reason"       to reasonToLog,
                                    "actorId"      to actorId
                                )
                            ).await()

                            Toast.makeText(context, "User deleted successfully", Toast.LENGTH_SHORT).show()

                            // FirebaseFunctions.getInstance("us-central1")
                            //   .getHttpsCallable("deleteUserAccount")
                            //   .call(mapOf("uid" to uidToDelete))
                            //   .await()

                        } catch (e: Exception) {
                            Toast.makeText(context, "Error deleting user: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    deleteUserId = null
                    deleteEmail = ""
                    deleteReason = ""
                }) {
                    Text("Cancel", color = Color.DarkGray)
                }
            }
        )
    }
}
