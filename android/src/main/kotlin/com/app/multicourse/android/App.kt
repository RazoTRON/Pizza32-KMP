package com.app.multicourse.android

import android.app.Application
import com.app.multicourse.di.AppDIComponent
import com.app.multicourse.di.create

class App : Application() {
    val appDIComponent: AppDIComponent by lazy(LazyThreadSafetyMode.NONE) {
        AppDIComponent::class.create(applicationContext)
    }
}