package me.martelli.wheresmycar.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.martelli.wheresmycar.proto.Device

@HiltViewModel
class AppViewModel @Inject constructor(
    private val configs: ConfigsRepo,
    private val devices: DevicesRepo,
) : ViewModel() {
    private val eventSink = { event: Event ->
        when (event) {
            is Event.CompleteOnboarding -> completeOnboarding()
            is Event.AddDevice -> addDevice(event.device)
            is Event.RemoveDevice -> removeDevice(event.device)
            is Event.UpdateDevice -> updateDevice(event.device)
        }
    }

    val uiState = combine(
        configs.onboardingCompleted,
        devices.devices,
    ) { onboardingCompleted, devices ->
        UiState(false, onboardingCompleted, devices, eventSink)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState(loading = true, onboardingCompleted = false, devices = emptyList()) {
            // drop events since we haven't loaded yet
        }
    )

    private fun completeOnboarding() {
        viewModelScope.launch {
            configs.completeOnboarding()
        }
    }

    private fun addDevice(device: Device) {
        viewModelScope.launch {
            devices.addDevice(device)
        }
    }

    private fun removeDevice(device: Device) {
        viewModelScope.launch {
            devices.removeDevice(device)
        }
    }

    private fun updateDevice(device: Device) {
        viewModelScope.launch {
            devices.updateDevice(device)
        }
    }
}

data class UiState(
    val loading: Boolean,
    val onboardingCompleted: Boolean,
    val devices: List<Device>,
    val eventSink: (Event) -> Unit
)

sealed class Event {
    data object CompleteOnboarding : Event()
    data class AddDevice(val device: Device) : Event()
    data class RemoveDevice(val device: Device) : Event()
    data class UpdateDevice(val device: Device) : Event()
}
