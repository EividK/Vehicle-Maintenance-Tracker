package com.example.vehiclehealth.services

/**
 * Handles User Authentication using Firebase
 */

import android.content.Context
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.concurrent.TimeUnit

class AuthService(private val context: Context? = null) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun registerUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid
                    if (uid != null) {
                        firestore.collection("roles")
                            .document(uid)
                            .set(mapOf("role" to "USER"))

                        val userData = mapOf(
                            "email"     to email,
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        firestore.collection("users")
                            .document(uid)
                            .set(userData)
                    }
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                } }
    }

    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun logoutUser() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun updatePassword(
        newPassword: String,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        auth.currentUser?.let { user ->
            user.updatePassword(newPassword)
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { e -> onResult(false, e.message) }
        } ?: onResult(false, "No user signed in")
    }

    fun deleteCurrentUser(
        currentPassword: String,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {

        if (currentPassword.isBlank()) {
            onResult(false, "Please enter your password")
            return
        }

        val user = auth.currentUser
        if (user == null) {
            onResult(false, "No user signed in")
            return
        }
        val uid = user.uid
        val email = user.email
            ?: return onResult(false, "User has no email")

        val cred = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(cred)
            .addOnFailureListener { e ->
                onResult(false, "Re-Authentication failed: ${e.message}")
            }
            .addOnSuccessListener {
                deleteNotifications(uid) {
                    deleteVehicles(uid) {
                        deleteServiceHistory(uid) {
                            deleteUserProfile(uid) {
                                deleteUserRole(uid) {
                                    user.delete()
                                        .addOnSuccessListener {
                                            auth.signOut()
                                            onResult(true, null)
                                        }
                                        .addOnFailureListener { e ->
                                            onResult(false, "Failed to delete user: ${e.message}")
                                        }
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun deleteNotifications(uid: String, onDone: () -> Unit) {
        firestore.collection("Notifications")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snap ->
                deleteCollection(snap, onDone)
            }
            .addOnFailureListener { _ ->
                onDone()
            }
    }

    private fun deleteVehicles(uid: String, onDone: () -> Unit) {
        firestore.collection("users")
            .document(uid)
            .collection("vehicles")
            .get()
            .addOnSuccessListener { snap ->
                deleteCollection(snap, onDone)
            }
            .addOnFailureListener { _ ->
                onDone()
            }
    }

    private fun deleteServiceHistory(uid: String, onDone: () -> Unit) {
        firestore.collection("ServiceHistory")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snap ->
                deleteCollection(snap, onDone)
            }
            .addOnFailureListener { _ ->
                onDone()
            }
    }

    private fun deleteUserProfile(uid: String, onDone: () -> Unit) {
        firestore.collection("users")
            .document(uid)
            .delete()
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { _ -> onDone() }
    }

    private fun deleteUserRole(uid: String, onDone: () -> Unit) {
        firestore.collection("roles")
            .document(uid)
            .delete()
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { _ -> onDone() }
    }

    private fun deleteCollection(
        snapshot: QuerySnapshot,
        onComplete: () -> Unit
    ) {
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit()
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { onComplete() }
    }
}
