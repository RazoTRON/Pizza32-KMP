package com.app.multicourse.domain.usecase

import com.app.multicourse.domain.common.AppCoroutineDispatcher
import com.app.multicourse.domain.common.AppResult
import com.app.multicourse.domain.model.CreateOrderParams
import com.app.multicourse.domain.model.CreatedOrderInfo
import com.app.multicourse.domain.model.OrderComment
import com.app.multicourse.domain.model.OrderInfo
import com.app.multicourse.domain.model.PaymentMethod
import com.app.multicourse.domain.repository.CartRepository
import com.app.multicourse.domain.repository.DeliveryRepository
import com.app.multicourse.domain.repository.OrderRepository
import com.app.multicourse.domain.repository.RestaurantRepository
import com.app.multicourse.domain.util.CreatingOrderException
import com.app.multicourse.domain.util.DeliveryIsUnavailableException
import com.app.multicourse.domain.util.DomainExceptionHandler
import com.app.multicourse.domain.util.MakeOrderIsUnavailableException
import com.app.multicourse.domain.util.UnavailableDeliveryAddressException

class CreateOrderUseCase(
    private val orderRepository: OrderRepository,
    private val deliveryRepository: DeliveryRepository,
    private val cartRepository: CartRepository,
    private val restaurantRepository: RestaurantRepository,
    private val dispatcher: AppCoroutineDispatcher,
    private val handler: DomainExceptionHandler
) {
    suspend fun execute(
        orderInfo: OrderInfo,
        paymentMethod: PaymentMethod,
    ): AppResult<CreatedOrderInfo> {
        return handler.handle(dispatcher.IO) {

            if (!checkCanOrder()) {
                throw MakeOrderIsUnavailableException()
            }

            if (!checkIsDeliveryAvailable()) {
                throw DeliveryIsUnavailableException()
            }

            if (!checkDeliveryAddress(orderInfo)) {
                throw UnavailableDeliveryAddressException()
            }

            val customerComment = orderRepository.getCustomerComment()?.text ?: ""

            val orderComment = OrderComment.Builder()
                .setCustomerComment(customerComment)
                .setDontCallMeNote(orderRepository.getDontCallMeStatus())
                .build()

            val response = createOrder(orderInfo, paymentMethod, orderComment)

            val isCreated = response.result

            if (!isCreated) {
                throw CreatingOrderException()
            }

            cartRepository.clear()
            orderRepository.clear()

            return@handle response
        }
    }


    private suspend fun checkCanOrder(): Boolean {
        val response = restaurantRepository.getRestaurantInfo()

        return response.canOrder
    }

    private suspend fun checkIsDeliveryAvailable(): Boolean {
        val response = deliveryRepository.isDeliveryAvailable()

        return response.isAvailable
    }

    private suspend fun checkDeliveryAddress(orderInfo: OrderInfo): Boolean {
        val response = deliveryRepository.getDeliveryAddressInfo(orderInfo.address)

        return response.result
    }

    private suspend fun createOrder(orderInfo: OrderInfo, paymentMethod: PaymentMethod, comment: OrderComment): CreatedOrderInfo {

        val guid = orderRepository.getGuid()

        return orderRepository.create(
            CreateOrderParams(guid, orderInfo, paymentMethod, comment)
        )
    }
}