package com.app.multicourse.feature.favourites

import com.app.multicourse.domain.common.ResultHandler
import com.app.multicourse.domain.usecase.GetFavouriteMenuItemsUseCase
import com.app.multicourse.domain.usecase.GetPriceCurrencyUseCase
import com.app.multicourse.feature.favourites.mapper.toUiModel
import com.app.multicourse.ui.composable.model.CurrencyUi
import com.app.multicourse.ui.composable.model.MenuItemUi
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

data class FavouriteScreenState(
    val isLoading: Boolean = true,
    val menuItemList: List<MenuItemUi> = listOf(),
    val priceCurrency: CurrencyUi = CurrencyUi(""),
)

@Inject
class FavouriteViewModel(
    private val getFavouriteMenuItemsUseCase: GetFavouriteMenuItemsUseCase,
    private val getPriceCurrencyUseCase: GetPriceCurrencyUseCase,
    private val handler: ResultHandler,
    @Assisted private val componentContext: ComponentContext,
    @Assisted private val navigator: FavouriteNavigator,
) : ComponentContext by componentContext {
    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow(FavouriteScreenState())
    val state = _state.asStateFlow()

    init {
        loadPriceCurrency()
        loadFavouriteMenuItems()
    }

    fun onFavouriteItemClick(menuItemUi: MenuItemUi) {
        navigator.openDetails(menuItemUi.id)
    }

    private fun loadPriceCurrency() {
        viewModelScope.launch {
            handler.handle {
                getPriceCurrencyUseCase.execute()
            }.onSuccess { response ->
                val currencyUi = response.data.toUiModel()

                _state.emit(state.value.copy(priceCurrency = currencyUi))
            }
        }
    }

    private fun loadFavouriteMenuItems() {
        viewModelScope.launch {
            val favouriteItemsFlow = getFavouriteMenuItemsUseCase.execute()

            favouriteItemsFlow
                .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000L))
                .collect { list ->
                    val result = list.map { it.toUiModel() }

                    _state.emit(state.value.copy(menuItemList = result, isLoading = false))
                }
        }
    }

    fun onBackClick() {
        navigator.moveBack()
    }

    companion object {
        fun create(
            componentContext: ComponentContext,
            navigator: FavouriteNavigator,
            favouriteViewModelFactory: FavouriteViewModelFactory,
        ): FavouriteViewModel {
            return favouriteViewModelFactory(componentContext, navigator)
        }
    }
}

typealias FavouriteViewModelFactory = (
    componentContext: ComponentContext,
    navigator: FavouriteNavigator,
) -> FavouriteViewModel