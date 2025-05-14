package com.example.purrytify.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.mediarouter.media.MediaRouter.RouteInfo
import com.example.purrytify.ui.model.AudioDeviceViewModel
import com.example.purrytify.ui.theme.Poppins

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSheet(
    viewModel: AudioDeviceViewModel,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    val devices by remember { mutableStateOf(viewModel.devices) }
    val selectedDevice by viewModel.selectedDevice

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Connect",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Poppins
                )
            }

            selectedDevice?.let {
                SelectedDeviceCard(it)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Speakers & Jams",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            LazyColumn {
                items(devices) { device ->
                    if (device.id == selectedDevice?.id) {
                        // nothing
                        Spacer(modifier = Modifier)
                    } else {
                        DeviceCard(device) {
                            viewModel.selectDevice(device)
                        }
                    }
                }
            }

            if (devices.size == 1) {
                Text(
                    "No devices found on this network",
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectedDeviceCard(route: RouteInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = when (route.deviceType) {
                    RouteInfo.DEVICE_TYPE_WIRED_HEADSET, RouteInfo.DEVICE_TYPE_WIRED_HEADPHONES, RouteInfo.DEVICE_TYPE_BLUETOOTH_A2DP, RouteInfo.DEVICE_TYPE_BLE_HEADSET, RouteInfo.DEVICE_TYPE_USB_HEADSET -> Icons.Default.Headphones
                    RouteInfo.DEVICE_TYPE_TV -> Icons.Default.Monitor
                    RouteInfo.DEVICE_TYPE_CAR -> Icons.Default.DirectionsCar
                    0 -> Icons.Default.Phone
                    else -> Icons.Default.Speaker
                },
                contentDescription = "Device Icon",
                tint = Color.Green,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = route.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "This Phone",
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(route: RouteInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = when (route.deviceType) {
                    RouteInfo.DEVICE_TYPE_WIRED_HEADSET, RouteInfo.DEVICE_TYPE_WIRED_HEADPHONES, RouteInfo.DEVICE_TYPE_BLUETOOTH_A2DP, RouteInfo.DEVICE_TYPE_BLE_HEADSET, RouteInfo.DEVICE_TYPE_USB_HEADSET -> Icons.Default.Headphones
                    RouteInfo.DEVICE_TYPE_TV -> Icons.Default.Monitor
                    RouteInfo.DEVICE_TYPE_CAR -> Icons.Default.DirectionsCar
                    0 -> Icons.Default.Phone
                    else -> Icons.Default.Speaker
                },
                contentDescription = "Device Icon",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = route.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "This Phone",
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}