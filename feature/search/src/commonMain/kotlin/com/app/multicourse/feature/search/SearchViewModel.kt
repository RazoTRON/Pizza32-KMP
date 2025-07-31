package com.app.multicourse.feature.search

import com.app.multicourse.domain.common.ResultHandler
import com.app.multicourse.domain.usecase.GetMenuItemsByNameUseCase
import com.app.multicourse.domain.usecase.GetMenuListUseCase
import com.app.multicourse.domain.usecase.GetPriceCurrencyUseCase
import com.app.multicourse.feature.search.mapper.toUiModel
import com.app.multicourse.ui.composable.model.CurrencyUi
import com.app.multicourse.ui.composable.model.MenuItemUi
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

data class SearchUiState(
    val menuItemList: List<MenuItemUi> = listOf(),
    val priceCurrency: CurrencyUi = CurrencyUi(""),
)

@Inject
class SearchViewModel(
    private val getMenuListUseCase: GetMenuListUseCase,
    private val getPriceCurrencyUseCase: GetPriceCurrencyUseCase,
    private val getMenuItemsByNameUseCase: GetMenuItemsByNameUseCase,
    private val handler: ResultHandler,
    @Assisted private val navigator: SearchScreenNavigator,
    @Assisted componentContext: ComponentContext,
) : ComponentContext by componentContext {
    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    init {
        loadPriceCurrency()
        findMenuByName(searchText.value)
    }

    init {
        @OptIn(FlowPreview::class)
        searchText.apply {
            viewModelScope.launch {
                shareIn(viewModelScope, SharingStarted.Lazily)
                    .debounce(500)
                    .collect { searchText -> findMenuByName(searchText) }
            }
        }
    }

    fun onSearchTextChange(text: String) {
        viewModelScope.launch {
            _searchText.emit(text)
        }
    }

    private fun loadPriceCurrency() {
        viewModelScope.launch {
            handler.handle {
                getPriceCurrencyUseCase.execute()
            }.onSuccess {
                val result = it.data.toUiModel()

                _state.emit(state.value.copy(priceCurrency = result))
            }
        }
    }

    private fun findMenuByName(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                loadMenuList()
            } else {
                handler.handle {
                    getMenuItemsByNameUseCase.execute(name)
                }.onSuccess { result ->
                    val list = result.data.map { it.toUiModel() }

                    _state.emit(state.value.copy(menuItemList = list))
                }
            }
        }
    }

    private fun loadMenuList() {
        viewModelScope.launch {
            handler.handle {
                getMenuListUseCase.execute()
            }.onSuccess { result ->
                val listUi = result.data.map { it.toUiModel() }

                _state.emit(state.value.copy(menuItemList = listUi))
            }
        }
    }

    fun onDetailsClick(menuItemUi: MenuItemUi) {
        navigator.openDetails(menuItemUi.id)
    }

    fun onBackClick() {
        navigator.moveBack()
    }

    companion object {
        fun create(
            navigator: SearchScreenNavigator,
            componentContext: ComponentContext,
            searchViewModelFactory: SearchViewModelFactory,
        ): SearchViewModel {
            return searchViewModelFactory(navigator, componentContext)
        }
    }
}

typealias SearchViewModelFactory = (
    navigator: SearchScreenNavigator,
    componentContext: ComponentContext
) -> SearchViewModel