package com.app.multicourse.feature.about

import com.app.multicourse.domain.common.isSuccess
import com.app.multicourse.domain.usecase.GetAboutInfoUseCase
import com.app.multicourse.feature.about.util.DialerUtil
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class AboutViewModel(
    private val getAboutInfoUseCase: GetAboutInfoUseCase,
    private val dialerUtil: DialerUtil,
    @Assisted private val navigator: AboutDialogNavigator,
    @Assisted componentContext: ComponentContext,
) : ComponentContext by componentContext {

    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow(AboutUiState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    fun hideErrorMessage() {
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {

            val result = getAboutInfoUseCase.execute()

            if (result.isSuccess()) {
                _state.update {
                    it.copy(
                        latitude = result.data.latitude,
                        longitude = result.data.longitude,
                        markerDescription = result.data.address,
                        address = result.data.address,
                        phone = result.data.phone,
                    )
                }
            } else {
                _state.update { it.copy(errorMessage = "Error data loading") }
            }
        }
    }

    fun onDismiss() {
        navigator.moveBack()
    }

    fun openDialer() {
        viewModelScope.launch {
            dialerUtil.openDialer(state.value.phone)
        }
    }
    companion object {
        fun create(
            navigator: AboutDialogNavigator,
            componentContext: ComponentContext,
            aboutViewModelFactory: AboutViewModelFactory,
        ): AboutViewModel {
            return aboutViewModelFactory(navigator, componentContext)
        }
    }
}

typealias AboutViewModelFactory = (
    navigator: AboutDialogNavigator,
    componentContext: ComponentContext,
) -> AboutViewModel