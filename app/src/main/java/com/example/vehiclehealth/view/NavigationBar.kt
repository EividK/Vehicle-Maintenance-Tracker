package com.example.vehiclehealth.view

import android.app.AlertDialog
import android.content.Intent
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vehiclehealth.R
import com.example.vehiclehealth.bottomnavigationbar.BottomNavigationBar
import com.example.vehiclehealth.services.VinDecoderService

@Composable
fun MainNavigationBar(navController: NavController, onVinClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter

    ) {
        BottomNavigationBar(
            navMyVehiclesImg = painterResource(id = R.drawable.bottom_navigation_bar_my_vehicles_icon_button),
                navMyVehiclesBtn = { navController.navigate("home") },
            navScheduleImg = painterResource(id = R.drawable.bottom_navigation_bar_schedule),
                navScheduleBtn = { navController.navigate("#") },
            navAddImg = painterResource(id = R.drawable.bottom_navigation_bar_add_icon),
                navAddBtn = { onVinClick() },
            navMapsImg = painterResource(id = R.drawable.bottom_navigation_bar_nearby_centres_icon_button),
                navMapsBtn = { navController.navigate("#") },
            navNotificationsImg = painterResource(id = R.drawable.bottom_navigation_bar_notifications_icon),
                navNotificationsBtn = { navController.navigate("#") },

            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .padding(bottom = 40.dp)
                .wrapContentHeight(Alignment.CenterVertically)
        )
    }
}