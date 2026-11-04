package com.dcapps.spese.feature.bugreport

import androidx.lifecycle.ViewModel
import com.dcapps.spese.core.bugreport.BugReportController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Espone lo stato del [BugReportController] alla UI (MainActivity). */
@HiltViewModel
class BugReportGateViewModel @Inject constructor(
    private val controller: BugReportController,
) : ViewModel() {
    val visible: StateFlow<Boolean> = controller.visible
    fun request() = controller.request()
    fun dismiss() = controller.dismiss()
}
