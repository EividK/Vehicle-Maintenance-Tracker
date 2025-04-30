package com.example.vehiclehealth.view.user

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.vehiclehealth.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.compose.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageLocatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var hasLocationPermission by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }
    LaunchedEffect(Unit) { permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    var currentLoc by remember { mutableStateOf<LatLng?>(null) }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        currentLoc = LatLng(loc.latitude, loc.longitude)
                    } else {
                        client.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            CancellationTokenSource().token
                        ).addOnSuccessListener { fresh ->
                            if (fresh != null) {
                                currentLoc = LatLng(fresh.latitude, fresh.longitude)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Unable to get location. Please check if GPS is turned on.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
                .addOnFailureListener { e -> Log.e("MapScreen", "lastLocation failed", e) }
        }
    }

    Places.initialize(context, context.getString(R.string.google_maps_key))
    val placesClient: PlacesClient = remember { Places.createClient(context) }
    val garages = remember { mutableStateListOf<Pair<Place, Float>>() }
    fun loadNearby() {
        currentLoc?.let {
            val request = FindCurrentPlaceRequest.newInstance(
                listOf(
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.RATING,
                    Place.Field.ADDRESS,
                    Place.Field.TYPES
                )
            )
            placesClient.findCurrentPlace(request)
                .addOnSuccessListener { resp ->
                    garages.clear()
                    resp.placeLikelihoods
                        .mapNotNull { it.place }
                        .filter { p ->
                            p.types?.any { t ->
                                t == Place.Type.CAR_REPAIR ||
                                        t == Place.Type.CAR_WASH ||
                                        t == Place.Type.GAS_STATION
                            } == true
                        }
                        .map { p -> p to (p.rating ?: 0f) }
                        .sortedByDescending { it.second as Nothing }
                        .take(15)
                        .forEach { garages.add(it as Pair<Place, Float>) }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Places load failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    data class Dest(val latLng: LatLng, val label: String)

    var selectedDest by remember { mutableStateOf<Dest?>(null) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Find Garages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (currentLoc == null) {
                            Toast.makeText(context, "Waiting for GPS…", Toast.LENGTH_SHORT).show()
                        } else {
                            loadNearby()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxSize()) {

                Box(Modifier.weight(2f).fillMaxHeight()) {
                    currentLoc?.let { loc ->
                        val cameraState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(loc, 14f)
                        }
                        GoogleMap(
                            modifier = Modifier.matchParentSize(),
                            cameraPositionState = cameraState,
                            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                            onMapClick = { latLng ->
                                selectedDest = Dest(latLng, "Custom location")
                            }
                        ) {
                            garages.forEach { (place, rating) ->
                                place.latLng?.let { ll ->
                                    MarkerInfoWindow(
                                        state = MarkerState(position = ll),
                                        title = place.name ?: "(no name)",
                                        snippet = "Rating: ${"%.1f".format(rating)}",
                                        onClick = {
                                            selectedDest = Dest(ll, place.name ?: "Garage")
                                            false
                                        }
                                    )
                                }
                            }
                        }
                    } ?: Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                        Text("Acquiring location…", color = Color.Gray)
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
                    Text("Nearby Garages", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Divider(color = Color.Gray)
                    if (garages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No nearby garages found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(garages) { (place, rating) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A293D))
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(place.name ?: "(no name)", color = Color.White)
                                        Text("Rating: ${"%.1f".format(rating)}", color = Color.LightGray)
                                        place.address?.let {
                                            Text(it, color = Color.Gray, maxLines = 1)
                                        }
                                        TextButton(onClick = {
                                            place.latLng?.let { ll ->
                                                selectedDest = Dest(ll, place.name ?: "Garage")
                                            }
                                        }) {
                                            Text("Go Here", color = Color.Cyan)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            selectedDest?.let { dest ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A293D))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(dest.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Lat: ${"%.5f".format(dest.latLng.latitude)}, " +
                                    "Lng: ${"%.5f".format(dest.latLng.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            val uri = Uri.parse(
                                "google.navigation:q=${dest.latLng.latitude},${dest.latLng.longitude}"
                            )
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                .setPackage("com.google.android.apps.maps")
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "No navigation app found", Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Navigate", color = Color.White)
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { selectedDest = null }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(currentLoc) {
        if (currentLoc != null) loadNearby()
    }
}

