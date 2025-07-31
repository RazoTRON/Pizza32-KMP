package com.app.multicourse.android.ksp

import com.app.multicourse.domain.common.ResultHandler
import com.app.multicourse.domain.usecase.AddToCartUseCase
import com.app.multicourse.domain.usecase.AddToFavouriteUseCase
import com.app.multicourse.domain.usecase.CheckIsFavouriteUseCase
import com.app.multicourse.domain.usecase.GetMenuItemByIdUseCase
import com.app.multicourse.domain.usecase.GetPriceCurrencyUseCase
import com.app.multicourse.domain.usecase.RemoveFromFavouriteUseCase
import com.app.multicourse.feature.details.ui.DetailsNavigator
import com.arkivanov.decompose.ComponentContext
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class TestComponent(
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
    companion object
}