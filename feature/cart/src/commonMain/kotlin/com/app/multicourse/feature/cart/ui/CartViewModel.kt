package com.app.multicourse.feature.cart.ui

import com.app.multicourse.domain.common.isSuccess
import com.app.multicourse.domain.model.OrderComment
import com.app.multicourse.domain.usecase.GetCustomerCommentUseCase
import com.app.multicourse.domain.usecase.GetDontCallMeStatusUseCase
import com.app.multicourse.domain.usecase.GetOrderItemsUseCase
import com.app.multicourse.domain.usecase.GetPriceCurrencyUseCase
import com.app.multicourse.domain.usecase.PickOrderUseCase
import com.app.multicourse.domain.usecase.RemoveFromCartUseCase
import com.app.multicourse.domain.usecase.SaveCustomerCommentUseCase
import com.app.multicourse.domain.usecase.SetDontCallMeStatusUseCase
import com.app.multicourse.domain.usecase.SyncDataUseCase
import com.app.multicourse.domain.usecase.UpdateCartItemUseCase
import com.app.multicourse.feature.cart.model.CurrencyUi
import com.app.multicourse.feature.cart.model.OrderItemUi
import com.app.multicourse.feature.cart.model.toDomainCartItem
import com.app.multicourse.feature.cart.model.toUiModel
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class CartViewModel(
    private val getPriceCurrencyUseCase: GetPriceCurrencyUseCase,
    private val getOrderItemsUseCase: GetOrderItemsUseCase,
    private val updateCartItemUseCase: UpdateCartItemUseCase,
    private val removeFromCartUseCase: RemoveFromCartUseCase,
    private val pickOrderUseCase: PickOrderUseCase,
    private val syncDataUseCase: SyncDataUseCase,
    private val getCustomerCommentUseCase: GetCustomerCommentUseCase,
    private val saveCustomerCommentUseCase: SaveCustomerCommentUseCase,
    private val getDontCallMeStatusUseCase: GetDontCallMeStatusUseCase,
    private val setDontCallMeStatusUseCase: SetDontCallMeStatusUseCase,
    @Assisted private val navigator: CartNavigator,
    @Assisted componentContext: ComponentContext,
) : ComponentContext by componentContext {

    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow(CartScreenState())
    val state = _state.asStateFlow()

    val cartItems = getCartItemsFlow()
    val totalPrice = getTotalPriceFlow()

    init {
        loadPriceCurrency()
        loadCustomerComment()
        loadDontCallMeStatus()
    }

    fun onCartItemClick(orderItemUi: OrderItemUi) {
        navigator.openDetails(orderItemUi.id)
    }

    fun onDeleteClick(orderItemUi: OrderItemUi) {
        viewModelScope.launch {
            removeFromCartUseCase.execute(orderItemUi.toDomainCartItem())
        }
    }

    fun onItemPlusClick(orderItemUi: OrderItemUi) {
        viewModelScope.launch {
            val count = orderItemUi.count + 1

            updateCartItemUseCase.execute(orderItemUi.copy(count = count).toDomainCartItem())
        }
    }

    fun onItemMinusClick(orderItemUi: OrderItemUi) {
        viewModelScope.launch {
            val count = maxOf(1, orderItemUi.count - 1)

            updateCartItemUseCase.execute(orderItemUi.copy(count = count).toDomainCartItem())
        }
    }

    fun onConfirmClick() {
        viewModelScope.launch {
            _state.update { it.copy(isOrderConfirming = true) }

            val result = pickOrderUseCase.execute()

            _state.update { it.copy(isOrderConfirming = false) }

            if (result.isSuccess()) {
                withContext(Dispatchers.Main) {
                    navigator.openContactInfoScreen()
                }
            } else {
                _state.update {
                    it.copy(
                        showError = true,
                        errorMessage = "Виникла помилка. Спробуйте ще раз."
                    )
                }
            }
        }
    }

    fun onDontCallMeChange(dontCallMe: Boolean) {
        _state.update { it.copy(dontCallMe = dontCallMe) }

        viewModelScope.launch {
            setDontCallMeStatusUseCase.execute(dontCallMe)
        }
    }

    fun onCustomerCommentChange(comment: String) {
        _state.update { it.copy(customerComment = comment) }

        viewModelScope.launch {
            saveCustomerCommentUseCase.execute(OrderComment(comment))
        }
    }

    fun hideError() {
        _state.update { it.copy(showError = false, errorMessage = null) }
    }

    fun onBackClick() {
        navigator.moveBack()
    }

    private fun showError(message: String) {
        _state.update { it.copy(showError = true, errorMessage = message) }
    }

    private fun getCartItemsFlow(): StateFlow<List<OrderItemUi>> {
        return getOrderItemsUseCase.execute()
            .map { items -> items.map { it.toUiModel() } }
            .onEach {
                _state.update { it.copy(isLoading = false) }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, listOf())
    }

    private fun getTotalPriceFlow(): StateFlow<Long> {
        return cartItems
            .map { list -> list.sumOf { it.totalPrice } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    }

    private fun loadCustomerComment() {
        viewModelScope.launch {
            val result = getCustomerCommentUseCase.execute()

            val comment = if (result.isSuccess()) {
                result.data?.text ?: ""
            } else {
                ""
            }

            withContext(Dispatchers.Main) {
                _state.update { it.copy(customerComment = comment) }
            }
        }
    }

    private fun loadPriceCurrency() {
        viewModelScope.launch {
            val result = getPriceCurrencyUseCase.execute()

            val currencyUi = if (result.isSuccess()) {
                result.data.toUiModel()
            } else {
                CurrencyUi.Default
            }

            withContext(Dispatchers.Main) {
                _state.update { it.copy(priceCurrency = currencyUi) }
            }
        }
    }

    private fun loadDontCallMeStatus() {
        viewModelScope.launch {
            val result = getDontCallMeStatusUseCase.execute()

            val dontCallMe = if (result.isSuccess()) {
                result.data
            } else {
                false
            }

            withContext(Dispatchers.Main) {
                _state.update { it.copy(dontCallMe = dontCallMe) }
            }
        }
    }

    companion object {
        fun create(
            navigator: CartNavigator,
            componentContext: ComponentContext,
            cartViewModelFactory: CartViewModelFactory,
        ): CartViewModel {
            return cartViewModelFactory(navigator, componentContext)
        }
    }
}

typealias CartViewModelFactory = (
    navigator: CartNavigator,
    componentContext: ComponentContext
) -> CartViewModel