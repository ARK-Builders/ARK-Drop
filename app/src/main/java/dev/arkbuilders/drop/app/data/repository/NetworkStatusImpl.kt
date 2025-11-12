package dev.arkbuilders.drop.app.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arkbuilders.drop.app.domain.repository.NetworkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class NetworkStatusImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : NetworkStatus {
        private val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        private val _onlineStatus = MutableStateFlow(checkIsOnline())
        override val onlineStatus: StateFlow<Boolean> = _onlineStatus

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

            cm.registerNetworkCallback(
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
            val network: Network = cm.activeNetwork ?: return false
            val networkCapabilities: NetworkCapabilities =
                cm.getNetworkCapabilities(network) ?: return false

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET,
                ) &&
                    networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                    ) &&
                    networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED,
                    )
            } else {
                networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET,
                ) &&
                    networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                    )
            }
        }
    }
