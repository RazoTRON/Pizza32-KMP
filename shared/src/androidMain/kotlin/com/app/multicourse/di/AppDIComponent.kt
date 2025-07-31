package com.app.multicourse.di

import android.content.Context
import com.app.multicourse.CommonModule
import com.app.multicourse.core.inject.AppLogger
import com.app.multicourse.core.inject.Singleton
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
@Singleton
abstract class AppDIComponent(
    @get:Provides val context: Context
) : CommonModule, AndroidModule, DataModule {
    abstract val logger: AppLogger
}