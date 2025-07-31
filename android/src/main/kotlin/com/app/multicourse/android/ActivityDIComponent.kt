package com.app.multicourse.android

import androidx.activity.ComponentActivity
import com.app.multicourse.di.AppDIComponent
import com.app.multicourse.navigation.RootComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.defaultComponentContext
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
abstract class ActivityDIComponent(
    @get:Provides val activity: ComponentActivity,
    @Component val appDIComponent: AppDIComponent,
    @get:Provides val componentContext: ComponentContext = activity.defaultComponentContext(),
) {
    abstract val rootComponent: RootComponent
}