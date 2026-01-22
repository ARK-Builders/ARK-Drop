package dev.arkbuilders.drop.data.helper

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("MissingPermission")
actual class NetworkStatus(private val context: Context) {
    actual fun isOnline() = onlineStatus.value

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _onlineStatus = MutableStateFlow(checkIsOnline())
    actual val onlineStatus: StateFlow<Boolean> = _onlineStatus

    init {
        val networkRequest =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                    }
                }
                .build()

        connectivityManager.registerNetworkCallback(
            networkRequest,
            object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    _onlineStatus.tryEmit(false)
                }

                override fun onAvailable(network: Network) {
                    _onlineStatus.tryEmit(true)
                }
            },
        )
    }

    private fun checkIsOnline(): Boolean {
        val network: Network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities: NetworkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        var isOnline =
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isOnline = isOnline &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
        }
        return isOnline
    }
}
