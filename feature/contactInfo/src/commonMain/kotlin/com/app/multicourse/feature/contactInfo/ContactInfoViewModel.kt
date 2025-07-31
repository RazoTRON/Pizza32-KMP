package com.app.multicourse.feature.contactInfo

import com.app.multicourse.domain.common.ResultHandler
import com.app.multicourse.domain.common.isError
import com.app.multicourse.domain.common.isSuccess
import com.app.multicourse.domain.model.Address
import com.app.multicourse.domain.usecase.CanDeliverUseCase
import com.app.multicourse.domain.usecase.CheckDeliveryAvailableUseCase
import com.app.multicourse.domain.usecase.CreateOrderUseCase
import com.app.multicourse.domain.usecase.GetDeliveryCostUseCase
import com.app.multicourse.domain.usecase.GetOrderItemsUseCase
import com.app.multicourse.domain.usecase.GetPriceCurrencyUseCase
import com.app.multicourse.domain.usecase.GetProposedAddressUseCase
import com.app.multicourse.domain.util.CreatingOrderException
import com.app.multicourse.domain.util.DeliveryException
import com.app.multicourse.domain.util.DeliveryIsUnavailableException
import com.app.multicourse.domain.util.MakeOrderIsUnavailableException
import com.app.multicourse.domain.util.NoBuildNumberException
import com.app.multicourse.domain.util.UnavailableDeliveryAddressException
import com.app.multicourse.feature.contactInfo.res.deliveryCostError
import com.app.multicourse.feature.contactInfo.res.deliveryOrderError
import com.app.multicourse.feature.contactInfo.res.enterApartNumError
import com.app.multicourse.feature.contactInfo.res.errorCreatingOrder
import com.app.multicourse.feature.contactInfo.res.incorrectAddressError
import com.app.multicourse.feature.contactInfo.res.incorrectNameError
import com.app.multicourse.feature.contactInfo.res.incorrectPhoneError
import com.app.multicourse.feature.contactInfo.res.proposedAddressError
import com.app.multicourse.feature.contactInfo.res.unknownCreatingOrderError
import com.app.multicourse.feature.contactInfo.util.PaymentMethodUi
import com.app.multicourse.feature.contactInfo.util.isAddressCorrect
import com.app.multicourse.feature.contactInfo.util.isNameCorrect
import com.app.multicourse.feature.contactInfo.util.isPhoneCorrect
import com.app.multicourse.feature.contactInfo.util.mapper.toOrderInfo
import com.app.multicourse.feature.contactInfo.util.toDomainModel
import com.app.multicourse.ui.res.StringRes
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class ContactInfoViewModel(
    private val getPriceCurrencyUseCase: GetPriceCurrencyUseCase,
    private val getOrderItemsUseCase: GetOrderItemsUseCase,
    private val getDeliveryCostUseCase: GetDeliveryCostUseCase,
    private val getProposedAddressUseCase: GetProposedAddressUseCase,
    private val canDeliverUseCase: CanDeliverUseCase,
    private val checkDeliveryAvailableUseCase: CheckDeliveryAvailableUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val handler: ResultHandler,
    @Assisted private val navigator: ContactInfoNavigator,
    @Assisted componentContext: ComponentContext,
) : ComponentContext by componentContext {

    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow(ContactInfoUiState())
    val state = _state.asStateFlow()

    @OptIn(FlowPreview::class)
    private val _addressField = MutableStateFlow(Address(id = "", street = "", city = "")).apply {
        viewModelScope.launch {
            debounce(500).collect { address ->
                loadProposedAddresses(address.street)
            }
        }
    }
    val addressField = _addressField.asStateFlow()

    init {
        loadPriceCurrency()
        updateTotalPrice()

        viewModelScope.launch {
            if (!checkIsDeliveryAvailable()) {
                showError("В даний момент доставка недоступна. Спробуйте пізніше.")
            }
        }
    }

    fun createOrder() {
        viewModelScope.launch {
            _state.update { it.copy(isOrderCreating = true) }

            if (!isContactInfoDataCorrect()) {
                _state.update { it.copy(isOrderCreating = false) }
                return@launch
            }

            val result = createOrderUseCase.execute(
                orderInfo = state.value.toOrderInfo(addressField.value),
                paymentMethod = state.value.payBy.toDomainModel()
            )

            if (result.isSuccess()) {
                val redirectUrl = result.data.redirectUrl

                withContext(Dispatchers.Main) {
                    if (redirectUrl != null && state.value.payBy == PaymentMethodUi.ONLINE) {
                        navigator.openPayOrderScreen(redirectUrl)
                    } else {
                        navigator.openSuccessOrderScreen()
                    }
                }

            }

            if (result.isError()) {
                val message = when (result.exception) {
                    is MakeOrderIsUnavailableException -> StringRes.deliveryOrderError
                    is DeliveryIsUnavailableException -> StringRes.deliveryOrderError
                    is UnavailableDeliveryAddressException -> StringRes.incorrectAddressError
                    is CreatingOrderException -> StringRes.errorCreatingOrder
                    else -> StringRes.unknownCreatingOrderError
                }

                showError(message)
            }

            _state.update { it.copy(isOrderCreating = false) }
        }
    }

    private fun showError(errorMessage: String) {
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = errorMessage, showError = true) }
        }
    }

    fun hideError() {
        viewModelScope.launch {
            _state.update { it.copy(showError = false) }
        }
    }

    fun onNameChange(name: String) {
        _state.update { it.copy(name = name) }

        viewModelScope.launch {
            handleCreateOrderButton()
        }
    }

    fun onPhoneChange(phone: String) {
        _state.update { it.copy(phone = phone) }

        viewModelScope.launch {
            handleCreateOrderButton()
        }
    }

    fun onEmailChange(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(email = email) }
        }
    }

    fun onAddressChange(address: String) {
        _addressField.update { Address(id = "", street = address, city = "") }

        viewModelScope.launch {
            handleCreateOrderButton()
        }
    }

    private fun updateDeliveryCost(address: Address) {
        viewModelScope.launch {
            val result = getDeliveryCostUseCase.execute(address)

            if (result.isSuccess()) {
                _state.update { it.copy(deliveryCost = result.data.value) }

                updateTotalPrice()
            } else {
                showError(StringRes.deliveryCostError)
            }
        }
    }

    private fun updateTotalPrice() {
        viewModelScope.launch {
            val orderItemsFlow = getOrderItemsUseCase.execute()

            val orderCost = orderItemsFlow.first().sumOf {
                it.menuItem.price * it.cartItem.count
            }

            val totalPrice = orderCost + state.value.deliveryCost

            _state.update { it.copy(totalPrice = totalPrice) }
        }
    }

    fun onProposedAddressClick(address: Address) {
        viewModelScope.launch {
            _state.emit(state.value.copy(isLoading = true))

            _addressField.emit(address)

            handler.handle {
                canDeliverUseCase.execute(address)
            }.onSuccess {
                hideError()
                handleCreateOrderButton()
                updateDeliveryCost(address)
            }.onError {
                disableCreateOrderButton()

                val message = when (it.exception) {
                    is NoBuildNumberException -> StringRes.enterApartNumError
                    is DeliveryException -> StringRes.deliveryOrderError
                    else -> StringRes.proposedAddressError
                }

                showError(message)
            }

            _state.emit(state.value.copy(isLoading = false))
        }
    }

    fun onPaymentMethodChange(method: PaymentMethodUi) {
        viewModelScope.launch {
            _state.emit(state.value.copy(payBy = method))
        }
    }

    private fun handleCreateOrderButton() {
        viewModelScope.launch {
            if (!isNameCorrect(state.value.name)) {
                _state.emit(state.value.copy(isConfirmButtonEnabled = false))
                return@launch
            }
            if (!isPhoneCorrect(state.value.phone)) {
                _state.emit(state.value.copy(isConfirmButtonEnabled = false))
                return@launch
            }
            if (!isAddressCorrect(addressField.value)) {
                _state.emit(state.value.copy(isConfirmButtonEnabled = false))
                return@launch
            }

            _state.emit(state.value.copy(isConfirmButtonEnabled = true))
        }
    }

    private fun isContactInfoDataCorrect(): Boolean {
        if (!isNameCorrect(state.value.name)) {
            showError(StringRes.incorrectNameError)
            return false
        }
        if (!isPhoneCorrect(state.value.phone)) {
            showError(StringRes.incorrectPhoneError)
            return false
        }
        if (!isAddressCorrect(addressField.value)) {
            showError(StringRes.incorrectAddressError)
            return false
        }

        return true
    }

    private fun disableCreateOrderButton() {
        viewModelScope.launch {
            _state.emit(state.value.copy(isConfirmButtonEnabled = false))
        }
    }

    private fun loadProposedAddresses(query: String) {
        viewModelScope.launch {
            if (query.length < 4) {
                _state.emit(state.value.copy(proposedAddresses = listOf()))

                return@launch
            }

            _state.emit(state.value.copy(isLoading = true))

            handler.handle {
                getProposedAddressUseCase.execute(query)

            }.onSuccess { result ->
                _state.update {
                    it.copy(proposedAddresses = result.data)
                }

            }.onError {
                showError(StringRes.proposedAddressError)
            }

            _state.emit(state.value.copy(isLoading = false))
        }
    }

    private suspend fun checkIsDeliveryAvailable(): Boolean {
        return viewModelScope.async {
            var result = false

            handler.handle {
                checkDeliveryAvailableUseCase.execute()
            }.onSuccess {
                if (it.data.isAvailable) {
                    result = true
                }
            }.onError {
                showError(StringRes.unknownCreatingOrderError)
            }

            result
        }.await()
    }

    private fun loadPriceCurrency() {
        viewModelScope.launch {
            handler.handle {
                getPriceCurrencyUseCase.execute()
            }.onSuccess { result ->
                _state.update { it.copy(currency = result.data.name) }
            }
        }
    }

    fun onBackClick() {
        navigator.moveBack()
    }

    companion object {
        fun create(
            navigator: ContactInfoNavigator,
            componentContext: ComponentContext,
            contactInfoViewModelFactory: ContactInfoViewModelFactory
        ): ContactInfoViewModel {
            return contactInfoViewModelFactory(navigator, componentContext)
        }
    }
}

typealias ContactInfoViewModelFactory = (
    navigator: ContactInfoNavigator,
    componentContext: ComponentContext,
) -> ContactInfoViewModel