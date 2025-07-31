package com.app.multicourse.feature.details.ui

import com.app.multicourse.domain.common.ResultHandler
import com.app.multicourse.domain.model.CartItem
import com.app.multicourse.domain.usecase.AddToCartUseCase
import com.app.multicourse.domain.usecase.AddToFavouriteUseCase
import com.app.multicourse.domain.usecase.CheckIsFavouriteUseCase
import com.app.multicourse.domain.usecase.GetMenuItemByIdUseCase
import com.app.multicourse.domain.usecase.GetPriceCurrencyUseCase
import com.app.multicourse.domain.usecase.RemoveFromFavouriteUseCase
import com.app.multicourse.feature.details.ui.util.mapper.toUiModel
import com.app.multicourse.ui.composable.model.CurrencyUi
import com.app.multicourse.ui.composable.model.MenuItemUi
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

data class DetailsUiState(
    val menuItem: MenuItemUi = MenuItemUi("", null, "", "", 0),
    val currency: CurrencyUi = CurrencyUi(""),
    val itemCount: Int = 1,
    val isFavourite: Boolean = false,
)

@Inject
class DetailsViewModel(
    @Assisted itemId: String,
    private val getMenuItemByIdUseCase: GetMenuItemByIdUseCase,
    private val getPriceCurrencyUseCase: GetPriceCurrencyUseCase,
    private val checkIsFavouriteUseCase: CheckIsFavouriteUseCase,
    private val addToCartUseCase: AddToCartUseCase,
    private val addToFavouriteUseCase: AddToFavouriteUseCase,
    private val removeFromFavouriteUseCase: RemoveFromFavouriteUseCase,
    private val handler: ResultHandler,
    @Assisted componentContext: ComponentContext,
    @Assisted private val navigator: DetailsNavigator,
) : ComponentContext by componentContext {

    private val _state = MutableStateFlow(DetailsUiState())
    val state = _state.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    init {
        loadMenuItem(itemId)
        loadPriceCurrency()
        checkIsFavourite(itemId)
    }

    private fun loadPriceCurrency() {
        viewModelScope.launch {
            handler.handle {
                getPriceCurrencyUseCase.execute()
            }.onSuccess { response ->
                _state.emit(state.value.copy(currency = response.data.toUiModel()))
            }
        }
    }

    fun onPlusClick() {
        viewModelScope.launch {
            _state.emit(state.value.copy(itemCount = state.value.itemCount + 1))
        }
    }

    fun onMinusClick() {
        viewModelScope.launch {
            _state.emit(state.value.copy(itemCount = maxOf(1, state.value.itemCount - 1)))
        }
    }

    fun onDismissRequest() {
        navigator.closeDialog()
    }

    fun onAddToCartClick() {
        viewModelScope.launch {
            handler.handle {
                addToCartUseCase.execute(
                    cartItem = CartItem(
                        state.value.menuItem.id,
                        state.value.itemCount,
                        state.value.menuItem.price
                    )
                )
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    navigator.closeDialog()
                }
            }
        }
    }

    fun onFavouriteClick() {
        viewModelScope.launch {
            handler.handle {
                if (state.value.isFavourite) {
                    removeFromFavouriteUseCase.execute(itemId = state.value.menuItem.id)
                } else {
                    addToFavouriteUseCase.execute(itemId = state.value.menuItem.id)
                }
            }.onSuccess {
                checkIsFavourite(state.value.menuItem.id)
            }
        }
    }

    private fun loadMenuItem(itemId: String) {
        viewModelScope.launch {
            handler.handle {
                getMenuItemByIdUseCase.execute(itemId)
            }.onSuccess { response ->
                val menuItem = response.data?.toUiModel() ?: return@onSuccess

                _state.emit(state.value.copy(menuItem = menuItem))
            }
        }
    }

    private fun checkIsFavourite(itemId: String) {
        viewModelScope.launch {
            handler.handle {
                checkIsFavouriteUseCase.execute(itemId)
            }.onSuccess { result ->
                _state.emit(state.value.copy(isFavourite = result.data))
            }
        }
    }

    companion object

}

//typealias DetailsViewModelFactory = (
//    itemId: String,
//    componentContext: ComponentContext,
//    navigator: DetailsNavigator,
//) -> DetailsViewModel


//public fun DetailsViewModel.Companion.create(
//    componentContext: ComponentContext,
//    navigator: DetailsNavigator,
//    factory: DetailsViewModelFactory,
//): DetailsViewModel = factory(
//    "s",
//    componentContext,
//    navigator
//)