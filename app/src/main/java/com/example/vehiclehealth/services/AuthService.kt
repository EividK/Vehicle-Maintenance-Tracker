package com.example.vehiclehealth.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthService { // Service class to handle authentication using firebase

    // FirebaseAuth instance to manage user authentication
    // serves as the entry point for all auth operations
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Register a user
    fun registerUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        // handles account creation on firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // If registration succeeds return true with no error message
                    onResult(true, null)
                } else {
                    // If registration fails return false and the error message
                    onResult(false, task.exception?.message)
                }
            }
    }

    // Login a user
    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        // Authenticates the credentials with firebase db
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login successful
                    onResult(true, null)
                } else {
                    // Login failed
                    onResult(false, task.exception?.message)
                }
            }
    }

    // Checks if user is currently logged in
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Logout the user
    fun logoutUser() {
        auth.signOut()
    }

    // Gets current logged in user
    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun updateUserPassword(newPassword: String, callback: (Boolean, String?) -> Unit) {
        // retrieves currently logged in user
        val user = FirebaseAuth.getInstance().currentUser
        // safe call to update password in firebase db if not null
        user?.updatePassword(newPassword)
            ?.addOnSuccessListener {
                // Password updated successfully
                // Callback to handle the success or failure of the password update
                callback(true, null)
            }
            ?.addOnFailureListener { exception ->
                // Password update failed
                callback(false, exception.message)
            }
    }

    // Existing Login Credentials in Database
    // admin@tus.ie
    // admin1
}
