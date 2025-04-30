package com.example.vehiclehealth.view.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vehiclehealth.R
import com.example.vehiclehealth.bottomnavigationbar.BottomNavigationBar
import com.google.relay.compose.BoxScopeInstanceImpl.align

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
                navScheduleBtn = { navController.navigate("calendar") },
            navAddImg = painterResource(id = R.drawable.bottom_navigation_bar_add_icon),
                navAddBtn = { onVinClick() },
            navMapsImg = painterResource(id = R.drawable.bottom_navigation_bar_nearby_centres_icon_button),
                navMapsBtn = { navController.navigate("garages") },
            navNotificationsImg = painterResource(id = R.drawable.bottom_navigation_bar_notifications_icon),
                navNotificationsBtn = { navController.navigate("notifications") },

            modifier = Modifier
                .padding(bottom = 20.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }

}