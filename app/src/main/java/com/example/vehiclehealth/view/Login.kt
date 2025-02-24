package com.example.vehiclehealth.view

import com.example.vehiclehealth.ui.theme.VehicleHealthTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vehiclehealth.R
import com.example.vehiclehealth.services.AuthService
import com.google.firebase.FirebaseApp

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Initialise Firebase
        enableEdgeToEdge()
        setContent {
            VehicleHealthTheme() {
                val navController = rememberNavController()

                LoginScreen(navController, AuthService())
            }
        }
    }
}

@Composable
fun LoginScreen(navController: NavController, authService: AuthService) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1D2B)), // Dark theme background
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.ic_home),
                contentDescription = "VehicleHealth Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontSize = 24.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Login to continue",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.White) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color(0xFF4A90E2), // Border color when focused
                    unfocusedIndicatorColor = Color.Gray // Border color when not focused
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color.White) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color(0xFF4A90E2), // Border color when focused
                    unfocusedIndicatorColor = Color.Gray // Border color when not focused
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = {
                    authService?.loginUser(email, password) { success, error ->
                        if (success) {
                            navController?.navigate("home")
                        } else {
                            errorMessage = error
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Login", color = Color.White, fontSize = 16.sp)
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Register Text
            TextButton(onClick = { navController?.navigate("register") }) {
                Text("Don't have an account? Register", color = Color(0xFF4A90E2))
            }
        }

        // Test Login Credentials
        // Email: user@gmail.com
        // Pswd: User1234
    }
}
