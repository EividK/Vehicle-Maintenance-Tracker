package com.example.vehiclehealth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize UI elements
        val emailField = findViewById<EditText>(R.id.etEmail)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)

        // Handle login button click
        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (validateLogin(email, password)) {
                // Proceed to another screen (e.g., MainActivity)
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // Show error message
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Simple login validation function
    private fun validateLogin(email: String, password: String): Boolean {
        return email.isNotEmpty() && password.isNotEmpty()
    }
}
