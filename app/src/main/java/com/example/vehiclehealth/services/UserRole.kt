package com.example.vehiclehealth.security

/**
 * Role access control
 */

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class UserRole { USER, ADMIN }

class AuthServiceWithRoles {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getCurrentUserRole(): UserRole {
        val user = auth.currentUser
        val uid = user?.uid ?: return UserRole.USER

        return try {
            val snap = firestore.collection("roles")
                .document(uid)
                .get()
                .await()
            val roleString = snap.getString("role")

            UserRole.valueOf(roleString ?: "USER")
        } catch (e: Exception) {
            UserRole.USER
        }
    }
}
