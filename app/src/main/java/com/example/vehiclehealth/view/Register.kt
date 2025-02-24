package com.example.vehiclehealth.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vehiclehealth.R
import com.example.vehiclehealth.services.AuthService
import com.example.vehiclehealth.ui.theme.VehicleHealthTheme
import com.google.firebase.FirebaseApp

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Initialise Firebase
        enableEdgeToEdge()
        setContent {
            VehicleHealthTheme {
            }
        }
    }
}

@Composable
fun RegisterScreen(navController: NavController, authService: AuthService) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .height(300.dp)
                .width(300.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_home),
                contentDescription = "VehicleHealth Logo",
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, top = 270.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Register", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(40.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
                .padding(end = 16.dp)
        )

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
                .padding(end = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            authService.registerUser(email, password) { success, error ->
                if (success) {
                    navController.navigate("login")
                } else {
                    errorMessage = error
                }
            }
        }) {
            Text("Register")
        }

        errorMessage?.let { // runs if errorMessage is not null
            Spacer(modifier = Modifier.height(8.dp))
            // grabs error and displays to screen
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Login")
        }
    }
}
