package com.app.multicourse.feature.home.ui

import com.app.multicourse.domain.common.ResultHandler
import com.app.multicourse.domain.common.isError
import com.app.multicourse.domain.common.isSuccess
import com.app.multicourse.domain.usecase.CheckIsOrderAvailableUseCase
import com.app.multicourse.domain.usecase.GetMenuByCategoryIdUseCase
import com.app.multicourse.domain.usecase.GetMenuCategoriesUseCase
import com.app.multicourse.domain.usecase.GetPriceCurrencyUseCase
import com.app.multicourse.domain.usecase.SyncDataUseCase
import com.app.multicourse.feature.home.ui.res.string.delivery_unavailable
import com.app.multicourse.feature.home.ui.res.string.unknown_error_message
import com.app.multicourse.feature.home.util.mapper.toUiModel
import com.app.multicourse.ui.composable.model.CategoryUi
import com.app.multicourse.ui.composable.model.CurrencyUi
import com.app.multicourse.ui.composable.model.MenuItemUi
import com.app.multicourse.ui.res.StringRes
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

data class HomeUiState(
    val menuItemList: List<MenuItemUi> = listOf(),
    val categories: List<CategoryUi> = listOf(),
    val priceCurrency: CurrencyUi = CurrencyUi(""),
    val errorMessage: String? = null,
    val isLoading: Boolean = true,
)

@Inject
class HomeViewModel(
    @Assisted componentContext: ComponentContext,
    private val checkIsOrderAvailableUseCase: CheckIsOrderAvailableUseCase,
    private val getPriceCurrencyUseCase: GetPriceCurrencyUseCase,
    private val getMenuCategoriesUseCase: GetMenuCategoriesUseCase,
    private val getMenuByCategoryIdUseCase: GetMenuByCategoryIdUseCase,
    private val syncDataUseCase: SyncDataUseCase,
    private val handler: ResultHandler,
    @Assisted private val navigator: HomeNavigator,
) : ComponentContext by componentContext {

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()


    private val componentScope = CoroutineScope(
        context = Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, _ -> }
    )

    init {
        loadData()
    }

    private fun loadData() {
        componentScope.launch {
            val result = syncDataUseCase.execute()
            if (result.isSuccess()) {
                fetchData()
            } else if (result.isError()) {
                _state.update {
                    it.copy(
                        errorMessage = it.errorMessage
                            ?: "При завантаженні виникла помилка. Перевірте зʼєднання з мережею."
                    )
                }
            }
        }
    }

    private fun fetchData() {
        loadPriceCurrency()
        loadCategories()

        componentScope.launch {
            if (!checkIsDeliveryAvailable()) {
                showError(StringRes.delivery_unavailable)
            }
        }
    }

    fun selectCategory(categoryUi: CategoryUi) {
        componentScope.launch {
            val categories = state.value.categories.map { category ->
                if (category.id == categoryUi.id) {
                    category.copy(selected = true)
                } else {
                    category.copy(selected = false)
                }
            }

            _state.emit(state.value.copy(categories = categories))

            loadMenuByCategory(categoryUi)
        }
    }

    fun onListItemClick(menuItemUi: MenuItemUi) {
        navigator.openDetails(menuItemUi.id)
    }

    fun hideError() {
        componentScope.launch {
            _state.emit(state.value.copy(errorMessage = null))
        }
    }

    private fun showError(errorMessage: String) {
        componentScope.launch {
            _state.emit(state.value.copy(errorMessage = errorMessage))
        }
    }

    private suspend fun checkIsDeliveryAvailable(): Boolean {
        return componentScope.async {
            var result = false

            handler.handle {
                checkIsOrderAvailableUseCase.execute()
            }.onSuccess {
                result = it.data.canOrder
            }.onError {
                showError(StringRes.unknown_error_message)
            }

            result
        }.await()
    }

    private fun loadPriceCurrency() {
        componentScope.launch {
            handler.handle {
                getPriceCurrencyUseCase.execute()
            }.onSuccess {
                val result = it.data.toUiModel()

                _state.emit(state.value.copy(priceCurrency = result))
            }
        }
    }

    private fun loadCategories() {
        componentScope.launch {
            handler.handle {
                getMenuCategoriesUseCase.execute()
            }.onSuccess { response ->
                val result = response.data.map { it.toUiModel() }

                val notEmptyCategories = result.filter {
                    val menuItems = getMenuByCategoryIdUseCase.execute(it.id)

                    if (menuItems.isSuccess()) {
                        menuItems.data.isNotEmpty()
                    } else {
                        false
                    }
                }

                _state.emit(state.value.copy(categories = notEmptyCategories))

                state.value.categories.firstOrNull()?.let {
                    selectCategory(it)
                }
            }
        }
    }

    private fun loadMenuByCategory(categoryUi: CategoryUi) {
        componentScope.launch {
            handler.handle {
                getMenuByCategoryIdUseCase.execute(categoryUi.id)
            }.onSuccess { response ->
                val list = response.data.map { it.toUiModel() }

                _state.emit(state.value.copy(menuItemList = list, isLoading = false))
            }
        }
    }

    fun onLocationClick() {
        navigator.openAboutScreen()
    }

    companion object {
        fun create(
            componentContext: ComponentContext,
            homeNavigator: HomeNavigator,
            homeViewModelComponentFactory: HomeViewModelComponentFactory
        ): HomeViewModel {
            return homeViewModelComponentFactory(
                componentContext,
                homeNavigator
            )// { stack.navigate(it) }
        }
    }
}

typealias HomeViewModelComponentFactory = (
    componentContext: ComponentContext,
    navigator: HomeNavigator,
) -> HomeViewModel
