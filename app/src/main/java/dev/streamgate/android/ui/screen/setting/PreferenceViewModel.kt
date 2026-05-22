package dev.streamgate.android.ui.screen.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.streamgate.android.data.repository.UserPreferencesRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PreferenceViewModel @Inject constructor(
    private val repository: UserPreferencesRepository
): ViewModel() {

    val frameRate = repository.frameRateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 30
    )

    fun updateFrameRate(newFrameRate: Int) {
        viewModelScope.launch { repository.saveFrameRate(newFrameRate) }
    }

}