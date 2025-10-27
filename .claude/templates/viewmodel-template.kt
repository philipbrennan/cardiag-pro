package com.[domain].[app].ui.[feature]

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.[domain].[app].data.repository.[Feature]Repository
import com.[domain].[app].ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [Feature] screen.
 *
 * Manages UI state and business logic for displaying/managing [entities].
 */
@HiltViewModel
class [Feature]ViewModel @Inject constructor(
    private val repository: [Feature]Repository
) : ViewModel() {

    // UI State
    private val _state = MutableStateFlow<UiState<List<[Entity]>>>(UiState.Loading)
    val state: StateFlow<UiState<List<[Entity]>>> = _state.asStateFlow()

    // Single event state (for navigation, toasts, etc.)
    private val _event = MutableSharedFlow<[Feature]Event>()
    val event: SharedFlow<[Feature]Event> = _event.asSharedFlow()

    init {
        loadData()
    }

    /**
     * Load data from repository
     */
    fun loadData() {
        viewModelScope.launch {
            _state.value = UiState.Loading

            repository.getAll()
                .catch { error ->
                    Timber.e(error, "Failed to load [entities]")
                    _state.value = UiState.Error(
                        error.message ?: "Failed to load [entities]"
                    )
                }
                .collect { items ->
                    _state.value = if (items.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(items)
                    }
                }
        }
    }

    /**
     * Refresh data (for pull-to-refresh)
     */
    fun refresh() {
        loadData()
    }

    /**
     * Handle item click
     */
    fun onItemClick(item: [Entity]) {
        viewModelScope.launch {
            Timber.d("Item clicked: ${item.id}")
            _event.emit([Feature]Event.NavigateToDetail(item.id))
        }
    }

    /**
     * Handle item delete
     */
    fun onItemDelete(item: [Entity]) {
        viewModelScope.launch {
            try {
                repository.delete(item)
                Timber.d("Item deleted: ${item.id}")
                _event.emit([Feature]Event.ShowMessage("Item deleted"))
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete item")
                _event.emit([Feature]Event.ShowError("Failed to delete item"))
            }
        }
    }

    /**
     * Handle item favorite toggle
     */
    fun onFavoriteToggle(item: [Entity]) {
        viewModelScope.launch {
            try {
                val updated = item.copy(isFavorite = !item.isFavorite)
                repository.update(updated)
                Timber.d("Item favorite toggled: ${item.id}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to update favorite status")
                _event.emit([Feature]Event.ShowError("Failed to update favorite"))
            }
        }
    }
}

/**
 * Events for navigation and one-time UI actions
 */
sealed class [Feature]Event {
    data class NavigateToDetail(val itemId: String) : [Feature]Event()
    data class ShowMessage(val message: String) : [Feature]Event()
    data class ShowError(val message: String) : [Feature]Event()
}
