package com.app.multicourse.navigation.cart

import com.app.multicourse.Configuration
import com.app.multicourse.feature.cart.ui.CartConfiguration
import com.app.multicourse.feature.cart.ui.CartViewModel
import com.app.multicourse.feature.cart.ui.CartViewModelFactory
import com.app.multicourse.feature.contactInfo.ContactInfoViewModel
import com.app.multicourse.feature.contactInfo.ContactInfoViewModelFactory
import com.app.multicourse.feature.contactInfo.configuration.ContactInfoConfiguration
import com.app.multicourse.feature.contactInfo.configuration.PayOrderConfiguration
import com.app.multicourse.feature.contactInfo.configuration.SuccessOrderConfiguration
import com.app.multicourse.navigation.navigator.CartNavigatorImpl
import com.app.multicourse.navigation.navigator.ContactInfoNavigatorImpl
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.statekeeper.polymorphicSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

object CartSubSerializer : KSerializer<Configuration> by polymorphicSerializer(
    SerializersModule {
        polymorphic(Configuration::class) {
            subclass(CartConfiguration::class, CartConfiguration.serializer())
            subclass(ContactInfoConfiguration::class, ContactInfoConfiguration.serializer())
            subclass(SuccessOrderConfiguration::class, SuccessOrderConfiguration.serializer())
            subclass(PayOrderConfiguration::class, PayOrderConfiguration.serializer())
        }
    }
)

@Inject
class CartNavComponent(
    @Assisted componentContext: ComponentContext,
    @Assisted private val dialogNavigation: SlotNavigation<Configuration>,
    @Assisted internal val parentStack: StackNavigation<Configuration>,
    private val cartViewModelFactory: CartViewModelFactory,
    private val contactInfoViewModelFactory: ContactInfoViewModelFactory,
) : ComponentContext by componentContext {

    internal val stack = StackNavigation<Configuration>()

    val childStack = childStack(
        source = stack,
        serializer = CartSubSerializer,
        initialConfiguration = CartConfiguration,
        handleBackButton = true,
        key = "CartStack",
        childFactory = ::createChildStackFactory
    )

    private fun createChildStackFactory(
        configuration: Configuration,
        componentContext: ComponentContext,
    ): Child {

        return when (configuration) {
            is CartConfiguration -> Child.CartScreen(
                component = CartViewModel.create(
                    componentContext = componentContext,
                    cartViewModelFactory = cartViewModelFactory,
                    navigator = CartNavigatorImpl(parentStack, stack, dialogNavigation)
                )
            )

            is ContactInfoConfiguration -> Child.ContactInfoScreen(
                component = ContactInfoViewModel.create(
                    componentContext = componentContext,
                    contactInfoViewModelFactory = contactInfoViewModelFactory,
                    navigator = ContactInfoNavigatorImpl(stack)
                )
            )

            is PayOrderConfiguration -> Child.PayOrderScreen(configuration.redirectUrl)

            is SuccessOrderConfiguration -> Child.SuccessOrderScreen()

            else -> throw UnsupportedOperationException()
        }
    }

    sealed class Child {

        data class CartScreen(val component: CartViewModel) : Child()
        data class ContactInfoScreen(val component: ContactInfoViewModel) : Child()
        class SuccessOrderScreen() : Child()
        class PayOrderScreen(val redirectUrl: String) : Child()
    }

    companion object {
        fun create(
            componentContext: ComponentContext,
            dialogNavigation: SlotNavigation<Configuration>,
            parentStack: StackNavigation<Configuration>,
            factory: CartNavComponentFactory,
        ): CartNavComponent {
            return factory(componentContext, dialogNavigation, parentStack)
        }
    }
}

typealias CartNavComponentFactory = (
    componentContext: ComponentContext,
    dialogNavigation: SlotNavigation<Configuration>,
    parentStack: StackNavigation<Configuration>,
) -> CartNavComponent