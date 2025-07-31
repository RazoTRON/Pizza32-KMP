package com.app.multicourse.navigation

import com.app.multicourse.Configuration
import com.app.multicourse.feature.about.AboutConfiguration
import com.app.multicourse.feature.about.AboutViewModel
import com.app.multicourse.feature.about.AboutViewModelFactory
import com.app.multicourse.feature.bottomBar.BottomBarConfiguration
import com.app.multicourse.feature.bottomBar.BottomBarViewModel
import com.app.multicourse.feature.cart.ui.CartNavConfiguration
import com.app.multicourse.feature.details.DetailsConfiguration
import com.app.multicourse.feature.details.ui.DetailsViewModel
import com.app.multicourse.feature.details.ui.DetailsViewModelFactory
import com.app.multicourse.feature.details.ui.create
import com.app.multicourse.feature.favourites.FavouriteViewModel
import com.app.multicourse.feature.favourites.FavouriteViewModelFactory
import com.app.multicourse.feature.favourites.FavouritesConfiguration
import com.app.multicourse.feature.home.HomeScreenConfiguration
import com.app.multicourse.feature.home.ui.HomeViewModel
import com.app.multicourse.feature.home.ui.HomeViewModelComponentFactory
import com.app.multicourse.feature.search.SearchConfiguration
import com.app.multicourse.feature.search.SearchViewModel
import com.app.multicourse.feature.search.SearchViewModelFactory
import com.app.multicourse.navigation.cart.CartNavComponent
import com.app.multicourse.navigation.cart.CartNavComponentFactory
import com.app.multicourse.navigation.navigator.AboutDialogNavigatorImpl
import com.app.multicourse.navigation.navigator.BottomBarNavigatorImpl
import com.app.multicourse.navigation.navigator.DetailsNavigator
import com.app.multicourse.navigation.navigator.FavouriteNavigatorImpl
import com.app.multicourse.navigation.navigator.HomeNavigator
import com.app.multicourse.navigation.navigator.SearchNavigatorImpl
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.statekeeper.polymorphicSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import me.tatarka.inject.annotations.Inject

//@ExperimentalEssentyApi
object ScreenSerializer : KSerializer<Configuration> by polymorphicSerializer(
    SerializersModule {
        polymorphic(Configuration::class) {
            subclass(HomeScreenConfiguration::class, HomeScreenConfiguration.serializer())
            subclass(DetailsConfiguration::class, DetailsConfiguration.serializer())
            subclass(BottomBarConfiguration::class, BottomBarConfiguration.serializer())
            subclass(AboutConfiguration::class, AboutConfiguration.serializer())
            subclass(FavouritesConfiguration::class, FavouritesConfiguration.serializer())
            subclass(SearchConfiguration::class, SearchConfiguration.serializer())
            subclass(CartNavConfiguration::class, CartNavConfiguration.serializer())
        }
    }
)

@Inject
class RootComponent(
    componentContext: ComponentContext,
    private val homeViewModelComponentFactory: HomeViewModelComponentFactory,
    private val aboutViewModelFactory: AboutViewModelFactory,
    private val detailsVMFactory: DetailsViewModelFactory,
    private val favouriteViewModelFactory: FavouriteViewModelFactory,
    private val searchViewModelFactory: SearchViewModelFactory,
    private val cartNavComponentFactory: CartNavComponentFactory,
) : ComponentContext by componentContext {

    private val stack = StackNavigation<Configuration>()

    private val dialogNavigation = SlotNavigation<Configuration>()

    val dialog: Value<ChildSlot<*, DialogChild>> =
        childSlot(
            source = dialogNavigation,
            serializer = ScreenSerializer,
            handleBackButton = true,
            childFactory = ::createChildSlotFactory
        )

    val childStack = childStack(
        source = stack,
        serializer = ScreenSerializer,
        initialConfiguration = HomeScreenConfiguration,
        handleBackButton = true,
        key = "ScreenStack",
        childFactory = ::createChildStackFactory
    )

    private fun createChildStackFactory(
        configuration: Configuration,
        componentContext: ComponentContext,
    ): Child {

        return when (configuration) {
            is HomeScreenConfiguration -> Child.HomeScreen(
                component = HomeViewModel.create(
                    componentContext = componentContext,
                    homeNavigator = HomeNavigator(dialogNavigation),
                    homeViewModelComponentFactory = homeViewModelComponentFactory
                )
            )

            is FavouritesConfiguration -> Child.FavouritesScreen(
                component = FavouriteViewModel.create(
                    componentContext = componentContext,
                    favouriteViewModelFactory = favouriteViewModelFactory,
                    navigator = FavouriteNavigatorImpl(stack, dialogNavigation)
                )
            )

            is SearchConfiguration -> Child.SearchScreen(
                component = SearchViewModel.create(
                    componentContext = componentContext,
                    searchViewModelFactory = searchViewModelFactory,
                    navigator = SearchNavigatorImpl(stack, dialogNavigation)
                )
            )

            is CartNavConfiguration -> Child.CartNavigation(
                component = CartNavComponent.create(
                    componentContext = componentContext,
                    dialogNavigation = dialogNavigation,
                    parentStack = stack,
                    factory = cartNavComponentFactory,
                )
            )

            else -> throw UnsupportedOperationException()
        }
    }

    private fun createChildSlotFactory(
        config: Configuration,
        childComponentContext: ComponentContext
    ): DialogChild {
        return when (config) {
            is DetailsConfiguration -> {
                DialogChild.DetailsDialog(
                    component = DetailsViewModel.create(
                        componentContext = childComponentContext,
                        itemId = config.id,
                        factory = detailsVMFactory,
                        navigator = DetailsNavigator(dialogNavigation)
                    )
                )
            }

            is AboutConfiguration -> {
                DialogChild.AboutDialog(
                    component = AboutViewModel.create(
                        componentContext = childComponentContext,
                        navigator = AboutDialogNavigatorImpl(dialogNavigation),
                        aboutViewModelFactory = aboutViewModelFactory,
                    )
                )
            }

            else -> throw UnsupportedOperationException()
        }
    }

    val bottomBarViewModel = BottomBarViewModel(
        componentContext = componentContext,
        stack = childStack.map { it.active.configuration },
        navigator = BottomBarNavigatorImpl(stack)
    )

    sealed class Child {
        data class HomeScreen(val component: HomeViewModel) : Child()
        data class FavouritesScreen(val component: FavouriteViewModel) : Child()
        data class SearchScreen(val component: SearchViewModel) : Child()
        data class CartNavigation(val component: CartNavComponent) : Child()
    }

    sealed class DialogChild {
        data class DetailsDialog(val component: DetailsViewModel) : DialogChild()
        data class AboutDialog(val component: AboutViewModel) : DialogChild()
    }
}