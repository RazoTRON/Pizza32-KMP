package com.app.multicourse.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.db.SqlDriver
import com.app.multicourse.common.DataExceptionHandler
import com.app.multicourse.common.RestaurantConfig
import com.app.multicourse.core.inject.AppLogger
import com.app.multicourse.core.inject.Singleton
import com.app.multicourse.db.AppDb
import com.app.multicourse.domain.common.BaseUrl
import com.app.multicourse.domain.repository.CartRepository
import com.app.multicourse.domain.repository.DeliveryRepository
import com.app.multicourse.domain.repository.FavouriteRepository
import com.app.multicourse.domain.repository.MenuRepository
import com.app.multicourse.domain.repository.OrderRepository
import com.app.multicourse.domain.repository.Repository
import com.app.multicourse.domain.repository.RestaurantRepository
import com.app.multicourse.domain.sync.RestaurantDataSynchronizer
import com.app.multicourse.network.AppConfig
import com.app.multicourse.network.KtorApi
import com.app.multicourse.network.OrderApi
import com.app.multicourse.network.util.ApiExceptionHandlerImpl
import com.app.multicourse.repository.CartRepositoryImpl
import com.app.multicourse.repository.DeliveryRepositoryImpl
import com.app.multicourse.repository.FavouriteRepositoryImpl
import com.app.multicourse.repository.MenuRepositoryImpl
import com.app.multicourse.repository.OrderLocalDataSource
import com.app.multicourse.repository.OrderRemoteDataSource
import com.app.multicourse.repository.OrderRepositoryImpl
import com.app.multicourse.repository.RepositoryImpl
import com.app.multicourse.repository.RestaurantDataSynchronizerImpl
import com.app.multicourse.repository.RestaurantLocalDataSource
import com.app.multicourse.repository.RestaurantRemoteDao
import com.app.multicourse.repository.RestaurantRemoteDataSource
import com.app.multicourse.repository.RestaurantRepositoryImpl
import com.app.multicourse.storage.dao.AboutDao
import com.app.multicourse.storage.dao.CategoryDao
import com.app.multicourse.storage.dao.FavouriteDao
import com.app.multicourse.storage.dao.InfoDao
import com.app.multicourse.storage.dao.MenuImagesDao
import com.app.multicourse.storage.dao.MenusDao
import com.app.multicourse.storage.dao.OrderItemsDao
import com.app.multicourse.storage.dao.RestaurantInfoDao
import com.app.multicourse.storage.data_store.OrderStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Provides
import kotlin.coroutines.CoroutineContext

interface DataModule {

    @Provides
    @Singleton
    fun provideAppDb(sqlDriver: SqlDriver): AppDb = AppDb(sqlDriver)

    @Provides
    fun provideCoroutineContext(): CoroutineContext = Dispatchers.Default + SupervisorJob()

    @Provides
    fun provideDao(db: AppDb, coroutineContext: CoroutineContext): InfoDao =
        InfoDao(db.infoEntityQueries, coroutineContext)

    @Provides
    fun provideRepository(infoDao: InfoDao, api: KtorApi): Repository = RepositoryImpl(infoDao, api)

    @Provides
    fun provideKtorApi(httpClient: HttpClient) = KtorApi(httpClient, AppConfig)

    @Provides
    @Singleton
    fun provideHttpClient(json: Json, appLogger: AppLogger): HttpClient = HttpClient(
        getPlatformHttpEngine()
    ) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    appLogger.d(message)
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideJson() = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
        prettyPrint = true
    }

    @Provides
    fun provideCategoryDao(db: AppDb): CategoryDao {
        return CategoryDao(db.categoryEntityQueries)
    }

    @Provides
    fun provideMenusDao(db: AppDb): MenusDao {
        return MenusDao(db.menusEntityQueries, db.menusEntityFTSQueries)
    }

    @Provides
    fun provideAboutDao(db: AppDb): AboutDao {
        return AboutDao(db.aboutEntityQueries)
    }

    @Provides
    fun provideMenuImagesDao(db: AppDb): MenuImagesDao {
        return MenuImagesDao(db.itemImagesEntityQueries)
    }

    @Provides
    fun provideRestaurantInfoDao(db: AppDb): RestaurantInfoDao {
        return RestaurantInfoDao(db.restaurantInfoEntityQueries)
    }

    @Provides
    fun provideRestaurantRepository(
        restaurantLocalDataSource: RestaurantLocalDataSource,
        restaurantRemoteDataSource: RestaurantRemoteDataSource,
        apiHandler: DataExceptionHandler,
    ): RestaurantRepository {
        return RestaurantRepositoryImpl(
            restaurantLocalDataSource,
            restaurantRemoteDataSource,
            apiHandler
        )
    }

    @Provides
    fun provideFavouriteDao(db: AppDb, coroutineContext: CoroutineContext): FavouriteDao {
        return FavouriteDao(db.favouriteEntityQueries, coroutineContext)
    }

    @Provides
    fun provideFavouriteRepository(favouriteDao: FavouriteDao): FavouriteRepository {
        return FavouriteRepositoryImpl(favouriteDao)
    }

    @Provides
    fun provideOrderItemsDao(db: AppDb, coroutineContext: CoroutineContext): OrderItemsDao {
        return OrderItemsDao(db.orderItemEntityQueries, coroutineContext)
    }

    @Provides
    fun provideCartRepository(orderItemDao: OrderItemsDao): CartRepository {
        return CartRepositoryImpl(orderItemDao)
    }

    @Provides
    fun provideOrderRepository(
        localDataSource: OrderLocalDataSource,
        remoteDataSource: OrderRemoteDataSource,
        apiHandler: DataExceptionHandler
    ): OrderRepository {
        return OrderRepositoryImpl(localDataSource, remoteDataSource, apiHandler)
    }

    @Provides
    fun provideOrderRemoteDataSource(
        baseUrl: BaseUrl,
        orderApi: OrderApi,
    ): OrderRemoteDataSource {
        return OrderRemoteDataSource(baseUrl, orderApi)
    }

    @Provides
    fun provideOrderLocalDataSource(
        orderStorage: OrderStorage,
    ): OrderLocalDataSource {
        return OrderLocalDataSource(orderStorage)
    }

    @Provides
    fun provideOrderApi(
        httpClient: HttpClient,
        baseUrl: BaseUrl,
    ): OrderApi {
        return OrderApi(httpClient, baseUrl)
    }

    @Provides
    @Singleton
    fun provideOrderStorage(
        orderDataStore: DataStore<Preferences>
    ): OrderStorage {
        return OrderStorage(orderDataStore)
    }

    @Provides
    fun provideMenuRepository(
        menusDao: MenusDao,
        imagesDao: MenuImagesDao,
        categoryDao: CategoryDao,
    ): MenuRepository {
        return MenuRepositoryImpl(menusDao, imagesDao, categoryDao)
    }

    @Provides
    fun provideDeliveryRepository(
        orderStorage: OrderStorage,
        orderApi: OrderApi,
        apiHandler: DataExceptionHandler,
        restaurantConfig: RestaurantConfig,
    ): DeliveryRepository {
        return DeliveryRepositoryImpl(orderStorage, orderApi, apiHandler, restaurantConfig)
    }
    @Provides
    fun provideRestaurantConfig(): RestaurantConfig {
        return RestaurantConfig()
    }

    @Provides
    fun provideRestaurantLocalDataSource(aboutDao: AboutDao, restaurantInfoDao: RestaurantInfoDao): RestaurantLocalDataSource {
        return RestaurantLocalDataSource(aboutDao, restaurantInfoDao)
    }

    @Provides
    fun provideRestaurantRemoteDataSource(restaurantRemoteDao: RestaurantRemoteDao): RestaurantRemoteDataSource {
        return RestaurantRemoteDataSource(restaurantRemoteDao)
    }

    @Provides
    fun provideRestaurantRemoteDao(baseUrl: BaseUrl, httpClient: HttpClient): RestaurantRemoteDao {
        return RestaurantRemoteDao(baseUrl, httpClient)
    }

    @Provides
    fun provideBaseUrl(): BaseUrl {
        return BaseUrl("https://pizza32cm.com.ua")
    }

    @Provides
    fun provideDataExceptionHandler(): DataExceptionHandler {
        return ApiExceptionHandlerImpl()
    }

    @Provides
    fun provideRestaurantDataSynchronizer(
        restaurantRemoteDao: RestaurantRemoteDao,
        menusDao: MenusDao,
        imagesDao: MenuImagesDao,
        aboutDao: AboutDao,
        categoryDao: CategoryDao,
        restaurantInfoDao: RestaurantInfoDao,
        apiHandler: DataExceptionHandler,
    ): RestaurantDataSynchronizer {
        return RestaurantDataSynchronizerImpl(
            restaurantRemoteDao,
            menusDao,
            imagesDao,
            categoryDao,
            aboutDao,
            restaurantInfoDao,
            apiHandler
        )
    }
}