package com.example.vehiclehealth.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vehiclehealth.R
import com.example.vehiclehealth.services.AuthService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, authService: AuthService) {
    var email            by rememberSaveable { mutableStateOf("") }
    var password         by rememberSaveable { mutableStateOf("") }
    var errorMessage     by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordVisible  by rememberSaveable { mutableStateOf(false) }
    val context          = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1D2B))
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Image(
                painter           = painterResource(id = R.drawable.ic_home),
                contentDescription= "VehicleHealth Logo",
                modifier          = Modifier.size(120.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text  = "Register",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(24.dp))

            TextField(
                value                   = email,
                onValueChange           = { email = it },
                placeholder             = { Text("Email", color = Color.Gray) },
                singleLine              = true,
                modifier                = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                shape                   = RoundedCornerShape(12.dp),
                colors                  = TextFieldDefaults.colors(
                    focusedContainerColor    = Color.Transparent,
                    unfocusedContainerColor  = Color.Transparent,
                    focusedTextColor         = Color.White,
                    unfocusedTextColor       = Color.White,
                    focusedIndicatorColor    = Color(0xFF4A90E2),
                    unfocusedIndicatorColor  = Color.Gray
                ),
                keyboardOptions         = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(Modifier.height(16.dp))

            TextField(
                value                   = password,
                onValueChange           = { password = it },
                placeholder             = { Text("Password", color = Color.Gray) },
                singleLine              = true,
                visualTransformation    = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon            = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(image, contentDescription = if (passwordVisible) "Hide password" else "Show password", tint = Color.White)
                    }
                },
                modifier                = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                shape                   = RoundedCornerShape(12.dp),
                colors                  = TextFieldDefaults.colors(
                    focusedContainerColor    = Color.Transparent,
                    unfocusedContainerColor  = Color.Transparent,
                    focusedTextColor         = Color.White,
                    unfocusedTextColor       = Color.White,
                    focusedIndicatorColor    = Color(0xFF4A90E2),
                    unfocusedIndicatorColor  = Color.Gray
                ),
                keyboardOptions         = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    authService.registerUser(email.trim(), password) { success, error ->
                        if (success) {
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                        } else {
                            errorMessage = error
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                shape    = RoundedCornerShape(24.dp)
            ) {
                Text("Register", color = Color.White, fontSize = 16.sp)
            }

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { navController.navigate("login") }) {
                Text("Already have an account? Login", color = Color(0xFF4A90E2))
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
