package com.example.purrytify.ui.screen

import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.purrytify.R
import com.example.purrytify.navigation.Screen
import com.example.purrytify.service.baseUrl
import com.example.purrytify.ui.component.NoInternetScreen
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.LoadImage
import com.example.purrytify.ui.model.ListeningStreak
import com.example.purrytify.ui.model.MonthlySoundCapsule
import com.example.purrytify.ui.model.ProfileViewModel
import com.example.purrytify.worker.LogoutListener
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.time.Month
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import android.provider.Settings
import androidx.core.content.edit
import com.example.purrytify.service.Profile
import com.example.purrytify.ui.model.SongStats
import com.example.purrytify.ui.util.DirectPDFGenerator
import androidx.core.net.toUri


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    globalViewModel: GlobalViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val secondaryColor = Color(0xFF01667A)
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.ProfileViewModelFactory(context.applicationContext as Application)
    )
    val scope = rememberCoroutineScope()
    val userState by viewModel.userState.collectAsState()
    val songStats by viewModel.songStats.collectAsState()
    val isConnected by globalViewModel.isConnected.collectAsState()

    val monthlyCapsules by viewModel.monthlyCapsules.collectAsState()
    val streaks by viewModel.streaks.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    var showLocationSheet by remember { mutableStateOf(false) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    val locationSheetState = rememberModalBottomSheetState()

    var checkLocationOnResume by remember { mutableStateOf(false) }
    var showLocationUpdateDialog by remember { mutableStateOf(false) }

    var isExportingPDF by remember { mutableStateOf(false) }
    var showPDFDialog by remember { mutableStateOf(false) }
    var pdfFile by remember { mutableStateOf<File?>(null) }

//    var showShareScreen by remember { mutableStateOf(false) }
//    var selectedCapsule by remember { mutableStateOf<MonthlySoundCapsule?>(null) }
//    var selectedStreak by remember { mutableStateOf<ListeningStreak?>(null) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Create a temporary file for camera capture
    val tempImageFile = remember {
        File(context.cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
    }
    val tempImageUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempImageFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = tempImageUri
            // Navigate to cropping screen
            navController.navigate(Screen.Profile.CropImage.createRoute(tempImageUri.toString()))
        }
        showBottomSheet = false
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            cameraLauncher.launch(tempImageUri)
        } else {
            // Permission denied, show dialog
            showPermissionDialog = true
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUri = it
            // Navigate to cropping screen
            navController.navigate(Screen.Profile.CropImage.createRoute(it.toString()))
        }
        showBottomSheet = false
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, get location
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val countryCode = viewModel.getCountryCodeFromLocation(it, context)
                        viewModel.updateLocation(countryCode)
                        Toast.makeText(
                            context,
                            "Location updated to $countryCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            // Permission denied, show dialog
            showLocationPermissionDialog = true
        }
        showLocationSheet = false
    }

    LogoutListener {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
        globalViewModel.clearUserId()
        globalViewModel.logout()
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            viewModel.loadUserProfile(
                onLogout = {
                    viewModel.logout(onComplete = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        globalViewModel.clearUserId()
                        globalViewModel.logout()
                    })
                },
                onSuccess = {
                    viewModel.loadSongStats()
                    viewModel.loadSoundCapsules()
                }
            )
        } else {
            viewModel.isLoading = false
        }
    }

    // Check SharedPreferences when the screen is active
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("check_location_on_resume", false)) {
            // Clear the flag
            prefs.edit() { putBoolean("check_location_on_resume", false) }

            // Show dialog to confirm location update
            showLocationUpdateDialog = true
        }
    }

    // Effect to check location when the flag is set
    LaunchedEffect(checkLocationOnResume) {
        if (checkLocationOnResume) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val countryCode = viewModel.getCountryCodeFromLocation(it, context)
                        Log.d("LOCATION_CC", countryCode)
                        viewModel.updateLocation(countryCode)
                        Toast.makeText(
                            context,
                            "Location updated to $countryCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    checkLocationOnResume = false
                }
            }
        }
    }

    if (viewModel.isLoading || userState == null || songStats == null) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        if (isConnected && viewModel.success) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    secondaryColor,
                                    MaterialTheme.colorScheme.background
                                ),
                            )
                        )
                        .padding(top = 48.dp, bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            LoadImage(
                                "${baseUrl}uploads/profile-picture/${userState!!.profilePhoto}",
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape),
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        showBottomSheet = true
                                    }
//                                    .background(Color.Gray.copy(alpha = 0.3f))
                                    ,
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit), // Make sure you have this icon
                                    contentDescription = "Edit Profile Picture",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = userState!!.username,
//                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )

                        Text(
                            text = userState!!.location,
//                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 24.sp,
                            modifier = Modifier.clickable(onClick = { showLocationSheet = true })
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.logout(onComplete = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    globalViewModel.clearUserId()
                                    globalViewModel.logout()
                                })
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            enabled = !viewModel.isLoggingOut,
                            modifier = Modifier
                                .width(120.dp)
                                .height(36.dp)
                        ) {
                            Text(
                                text = "Log Out",
                                color = Color.White
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(value = songStats!!.totalSongs.toString(), label = "SONGS")
                    StatItem(value = songStats!!.likedSongs.toString(), label = "LIKED")
                    StatItem(value = songStats!!.listenedSongs.toString(), label = "LISTENED")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your sound capsule",
//                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (!isExportingPDF) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download), // Make sure to have this icon
                            contentDescription = "Download",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                                .clickable(onClick = {
                                    scope.launch {
                                        isExportingPDF = true
                                        exportProfileToPDFDirect(
                                            context = context,
                                            userState = userState,
                                            songStats = songStats,
                                            monthlyCapsules = monthlyCapsules,
                                            streaks = streaks,
                                            onSuccess = { file ->
                                                pdfFile = file
                                                showPDFDialog = true
                                                isExportingPDF = false
                                            },
                                            onError = { error ->
                                                isExportingPDF = false
                                                Toast.makeText(
                                                    context,
                                                    "Failed exporting to PDF",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                Log.d("ERROR PDF", error)
                                                // Handle error (show toast, etc.)
                                            }
                                        )
                                    }
                                }, enabled = !isExportingPDF)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }

                }

                // Monthly Sound Capsules
                if (monthlyCapsules.isNotEmpty()) {
                    if (monthlyCapsules.size != streaks.size) {
                        Log.d("FATAL", "MONTHLY CAPSULED AND STREAKS DOES NOT HAVE THE SAME SIZE")
                    }
                    for (i in 0 until monthlyCapsules.size) {
                        MonthlySoundCapsuleSection(
                            capsule = monthlyCapsules[i],
                            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                            onClickArtist = { month, year ->
                                navController.navigate(Screen.Profile.TopArtist.createRoute(month, year))
                            },
                            onClickSong = { month, year ->
                                navController.navigate(Screen.Profile.TopSong.createRoute(month, year))
                            },
                            onClickTimeListened = {
                                navController.navigate(Screen.Profile.TimeListened.route)
                            },
                            onShare = { month, year ->
                                navController.navigate(Screen.Profile.ShareMonthlyCapsule.createRoute(month, year))
//                                selectedCapsule = monthlyCapsules[i]
////                                shareType = ShareType.MONTHLY_CAPSULE
//                                showShareScreen = true

                            }
                        )
                        if (streaks[i] != null) {
                            ListeningStreakItem(
                                streak = streaks[i]!!,
                                onShare = {
//                                    selectedStreak = streaks[i]
////                                    shareType = ShareType.LISTENING_STREAK
//                                    showShareScreen = true
                                },
                                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                            )
                        }
                    }
                }

                // Listening Streaks
//                if (streaks.isNotEmpty()) {
//                    streaks.take(3).forEach { streak ->
//                        ListeningStreakItem(
//                            streak = streak,
//                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
//                        )
//                    }
//                }

//                Spacer(modifier = Modifier.height(80.dp))
                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            NoInternetScreen {
                scope.launch {
                    viewModel.loadUserProfile(
                        onLogout = {
                            viewModel.logout(onComplete = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                                globalViewModel.clearUserId()
                                globalViewModel.logout()
                            })
                        },
                        onSuccess = {
                            viewModel.loadSongStats()
                        }
                    )
                }
            }
        }
    }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray)
                )
            },
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White
        ) {
            ProfilePictureBottomSheet(
                onCameraClick = {
                    // Check camera permission first
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) -> {
                            // Permission already granted, launch camera
                            cameraLauncher.launch(tempImageUri)
                        }
                        else -> {
                            // Request camera permission
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    showBottomSheet = false
                },
                onGalleryClick = {
                    // Launch gallery to pick image
                    galleryLauncher.launch("image/*")
                },
                onDismiss = { showBottomSheet = false }
            )
        }
    }

    if (showLocationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLocationSheet = false },
            sheetState = locationSheetState,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray)
                )
            },
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White
        ) {
            LocationSelectionBottomSheet(
                onAutomaticClick = {
                    // Check location permission first
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) -> {
                            // Permission already granted, get location
                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    location?.let {
                                        val countryCode = viewModel.getCountryCodeFromLocation(it, context)
                                        Log.d("LOCATION_CC", countryCode)
                                        viewModel.updateLocation(countryCode)
                                        Toast.makeText(
                                            context,
                                            "Location updated to $countryCode",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    if (location == null ) {
                                        Log.d("LOCATION_CC", "IS NULL")
                                    }
                                }
                            }
                        }
                        else -> {
                            // Request location permission
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                    showLocationSheet = false
                },
                onManualClick = {
                    Toast.makeText(
                        context,
                        "Select your location by dropping a pin on the map, then return to app",
                        Toast.LENGTH_LONG
                    ).show()

                    // Launch Google Maps for manual selection
                    try {
                        // Use search intent with the search box open
                        val mapIntent = Intent(Intent.ACTION_VIEW, "geo:0,0?z=2".toUri())

                        // Just launch the map without trying to handle the result
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)

                            // Set a flag in SharedPreferences to indicate we should check location when resuming
                            val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
                            prefs.edit() { putBoolean("check_location_on_resume", true) }
                        } else {
                            // Try alternate approach
                            val alternateIntent = Intent(Intent.ACTION_VIEW,
                                "https://maps.google.com/".toUri())

                            if (alternateIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(alternateIntent)

                                // Set the same flag
                                val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
                                prefs.edit() { putBoolean("check_location_on_resume", true) }
                            } else {
                                Toast.makeText(
                                    context,
                                    "No map application found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileScreen", "Error launching maps: ${e.message}")
                        Toast.makeText(
                            context,
                            "Could not open maps",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    showLocationSheet = false
                },
                onDismiss = { showLocationSheet = false }
            )
        }
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text("Camera Permission Required")
            },
            text = {
                Text("To take a photo, please allow camera access in your device settings.")
            },
            confirmButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text("OK")
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }

    if (showLocationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showLocationPermissionDialog = false },
            title = {
                Text("Location Permission Required")
            },
            text = {
                Text("To set your location automatically, please allow location access in your device settings.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                        showLocationPermissionDialog = false
                    }
                ) {
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLocationPermissionDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }

    // Location update confirmation dialog using Compose
    if (showLocationUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showLocationUpdateDialog = false },
            title = {
                Text("Update Location")
            },
            text = {
                Text("Would you like to update your country based on the location you selected?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Get current location
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            checkLocationOnResume = true
                        } else {
                            // Request permission
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        showLocationUpdateDialog = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLocationUpdateDialog = false }
                ) {
                    Text("No")
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }

    if (showPDFDialog && pdfFile != null) {
        AlertDialog(
            onDismissRequest = { showPDFDialog = false },
            title = {
                Text("PDF Export Successful")
            },
            text = {
                Text("Your sound capsule has been exported to PDF successfully!")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Open the PDF
                        openPDF(context, pdfFile!!)
                        showPDFDialog = false
                    }
                ) {
                    Text("Open PDF")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPDFDialog = false }
                ) {
                    Text("Close")
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp
        )
        Text(
            text = label,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun MonthlySoundCapsuleSection(
    capsule: MonthlySoundCapsule,
    modifier: Modifier = Modifier,
    onClickArtist: (Int, Int) -> Unit,
    onClickSong: (Int, Int) -> Unit,
    onClickTimeListened: () -> Unit,
    onShare: (Int, Int) -> Unit
) {
    val month = Month.valueOf(capsule.month.split(" ")[0].uppercase(Locale.getDefault())).value
    val year = capsule.month.split(" ")[1].toInt()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121212))
    ) {
// Header with month and share icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = capsule.month,
//                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_share), // Make sure to have this icon
                contentDescription = "Share",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
                    .clickable(onClick = { onShare(month, year) })
            )
        }

        // Time Listened Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E1E))
                .padding(vertical = 12.dp)
        ) {
            Column (modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable(onClick = onClickTimeListened)) {
                Text(
                    text = "Time listened",
//                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${capsule.totalListeningMinutes} minutes",
//                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            }
        }

        // Top Artist and Top Song Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                // Top Artist
                Column (modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onClickArtist(month, year) } )) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top artist",
//                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = "See More",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Artist Image
                        LoadImage(
                            capsule.topArtist?.imagePath ?: R.drawable.starboy.toString(),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = capsule.topArtist?.name ?: "No Data",
//                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4A90E2),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Top Song
            Box(
                modifier = Modifier
                    .weight(1f) // Added weight to ensure equal sizing
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(vertical = 12.dp, horizontal = 16.dp) // Added padding inside box
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onClickSong(month, year) } )) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top song",
//                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = "See More",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Song Image
                        LoadImage(
                            capsule.topSong?.imagePath ?: R.drawable.starboy.toString(),
                            contentDescription = "Song Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = capsule.topSong?.title ?: "No Data",
//                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFEB3B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListeningStreakItem(
    streak: ListeningStreak,
    modifier: Modifier = Modifier,
    onShare: () -> Unit
) {
    val startDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(streak.startDate))
//    val endDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(streak.endDate))
//    val dateRange = if (startDate == endDate) startDate else "$startDate - $endDate"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        // Album art for the streak

        LoadImage(
            streak.trackDetails?.imagePath ?: R.drawable.starboy.toString(),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(240.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Streak title and description
        Text(
            text = "You had a ${streak.dayCount}-day streak",
//            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Song details
        streak.trackDetails?.let { track ->
            Text(
                text = "You played ${track.title} by ${track.artist} day after day. You were on fire",
//                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date range with share button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = startDate,
//                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_share),
                contentDescription = "Share",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
                    .clickable(onClick = { onShare() })
            )
        }
    }
}

@Composable
fun ProfilePictureBottomSheet(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Profile photo",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Close",
                tint = Color.Gray,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { onDismiss() }
                    .padding(4.dp)
                    .size(24.dp)
            )
        }

        // Options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Camera option
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onCameraClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = "Camera",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Camera",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            // Gallery option
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onGalleryClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gallery),
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Gallery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun LocationSelectionBottomSheet(
    onAutomaticClick: () -> Unit,
    onManualClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Set location",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Close",
                tint = Color.Gray,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { onDismiss() }
                    .padding(4.dp)
                    .size(24.dp)
            )
        }

        // Options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Automatic option
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onAutomaticClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location),
                        contentDescription = "Automatic",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Automatic",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            // Manual option
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onManualClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_map),
                        contentDescription = "Manual",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Manual",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

suspend fun exportProfileToPDFDirect(
    context: Context,
    userState: Profile?, // Your actual UserState type
    songStats: SongStats?, // Your actual SongStats type
    monthlyCapsules: List<MonthlySoundCapsule>, // Your actual MonthlySoundCapsule type
    streaks: List<ListeningStreak?>, // Your actual ListeningStreak type
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val pdfGenerator = DirectPDFGenerator(context)
        val file = pdfGenerator.generateProfilePDF(
            userState = userState,
            songStats = songStats,
            monthlyCapsules = monthlyCapsules,
            streaks = streaks
        )

        if (file != null) {
            onSuccess(file)
        } else {
            onError("Failed to generate PDF")
        }
    } catch (e: Exception) {
        onError(e.message ?: "Unknown error")
    }
}

fun openPDF(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val openIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Check if there's an app that can handle PDF files
        if (openIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(openIntent)
        } else {
            // If no PDF viewer is available, try to open with any available app
            val chooserIntent = Intent.createChooser(openIntent, "Open PDF with")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (chooserIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooserIntent)
            } else {
                // Show a toast if no app can handle the PDF
                Toast.makeText(
                    context,
                    "No PDF viewer app found. Please install a PDF reader.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(
            context,
            "Error opening PDF: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
        Log.d("ERROR PDF", e.message.toString())
    }
}