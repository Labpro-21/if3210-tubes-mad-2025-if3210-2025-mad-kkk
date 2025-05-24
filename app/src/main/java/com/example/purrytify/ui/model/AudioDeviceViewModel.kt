package com.example.purrytify.ui.model

import android.app.Application
import android.util.Log
import androidx.mediarouter.media.MediaRouter
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter.RouteInfo

class AudioDeviceViewModel(application: Application) : AndroidViewModel(application) {
    private val _devices = mutableStateListOf<RouteInfo>()
    val devices: List<RouteInfo> = _devices

    private val _selectedDevice = mutableStateOf<RouteInfo?>(null)
    val selectedDevice: State<RouteInfo?> = _selectedDevice

    private val mediaRouter = MediaRouter.getInstance(application)

    private val callback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: RouteInfo) {
            super.onRouteAdded(router, route)
            addDevice(route)
        }

        override fun onRouteRemoved(router: MediaRouter, route: RouteInfo) {
            super.onRouteRemoved(router, route)
            _devices.remove(route)
        }

        override fun onRouteSelected(router: MediaRouter, route: RouteInfo, reason: Int) {
            super.onRouteSelected(router, route, reason)
            _selectedDevice.value = route
        }

        override fun onRouteChanged(router: MediaRouter, route: RouteInfo) {
            super.onRouteChanged(router, route)
            refreshDevices()
        }
    }

    init {
        mediaRouter.addCallback(
            MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                .build(),
            callback,
            MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
        )
    }

    private fun addDevice(route: RouteInfo) {
        _devices.add(route)
        Log.d("DEBUG MEDIA ROUTE", route.deviceType.toString())
    }

    fun refreshDevices() {
        _devices.clear()
        _selectedDevice.value = mediaRouter.selectedRoute
        mediaRouter.routes.forEach { route ->
            _devices.add(route)
        }
    }

    fun selectDevice(route: RouteInfo) {
        mediaRouter.selectRoute(route)
    }

    override fun onCleared() {
        super.onCleared()
        mediaRouter.removeCallback(callback)
    }

    class AudioDeviceViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AudioDeviceViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AudioDeviceViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
