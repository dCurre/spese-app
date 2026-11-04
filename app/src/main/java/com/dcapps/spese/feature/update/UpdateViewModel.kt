package com.dcapps.spese.feature.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.update.ApkInstaller
import com.dcapps.spese.core.update.DownloadState
import com.dcapps.spese.core.update.UpdateInfo
import com.dcapps.spese.core.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateUiState(
    val info: UpdateInfo? = null,
    val dismissed: Boolean = false,
    val downloading: Boolean = false,
    val progress: Int = 0,
    val error: String? = null,
) {
    val showDialog: Boolean get() = info != null && !dismissed
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val apkInstaller: ApkInstaller,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            val info = updateRepository.checkForUpdate()
            if (info != null) _uiState.update { it.copy(info = info) }
        }
    }

    fun onUpdateClick() {
        val info = _uiState.value.info ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(downloading = true, progress = 0, error = null) }
            val result = apkInstaller.download(info.apkUrl) { percent ->
                _uiState.update { it.copy(progress = percent) }
            }
            when (result) {
                is DownloadState.Done -> {
                    _uiState.update { it.copy(downloading = false) }
                    apkInstaller.install(result.file)
                }
                is DownloadState.Failed ->
                    _uiState.update { it.copy(downloading = false, error = result.reason) }
                is DownloadState.Progress -> Unit
            }
        }
    }

    fun canInstall(): Boolean = apkInstaller.canInstall()

    fun dismiss() = _uiState.update { it.copy(dismissed = true) }
}
