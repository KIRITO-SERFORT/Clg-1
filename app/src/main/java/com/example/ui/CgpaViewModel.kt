package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.preferences.CgpaStore
import com.example.domain.CgpaUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Holds the calculator inputs and saves them on this phone after every change. */
class CgpaViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(CgpaStore.load(app))
    val state: StateFlow<CgpaUiState> = _state.asStateFlow()

    fun edit(transform: (CgpaUiState) -> CgpaUiState) {
        _state.update(transform)
        CgpaStore.save(getApplication(), _state.value)
    }

    fun resetAll() {
        CgpaStore.clear(getApplication())
        _state.value = CgpaUiState()
    }

    fun newId(): String = java.lang.Long.toString(System.nanoTime(), 36)
}
