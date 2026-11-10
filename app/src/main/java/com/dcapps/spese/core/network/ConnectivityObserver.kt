package com.dcapps.spese.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Osserva lo stato di connettività del dispositivo ed espone un [isOnline] reattivo per la UI.
 *
 * "Online" = esiste una rete di default con capability INTERNET e VALIDATED (cioè raggiunge davvero
 * Internet, non solo un AP senza uscita). Usa [ConnectivityManager.registerDefaultNetworkCallback]
 * così lo stato è sempre allineato senza polling. Lo stato iniziale è letto dalla rete attiva.
 *
 * Non è la "verità" sul fatto che il backend risponda (per quello c'è il sync best-effort), ma è
 * abbastanza per: mostrare un indicatore offline e lucchettare le operazioni che richiedono rete
 * (es. creare una lista condivisa).
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(currentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = currentlyOnline()
        }

        override fun onLost(network: Network) {
            _isOnline.value = currentlyOnline()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            _isOnline.value = caps.hasInternet()
        }
    }

    init {
        // Singleton a vita applicazione: non serve deregistrare.
        runCatching { cm.registerDefaultNetworkCallback(callback) }
    }

    private fun currentlyOnline(): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasInternet()
    }

    private fun NetworkCapabilities.hasInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
