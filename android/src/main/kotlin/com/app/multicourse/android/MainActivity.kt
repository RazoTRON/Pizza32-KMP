package com.app.multicourse.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.app.multicourse.navigation.NavHost

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appComponent = (application as App).appDIComponent

        val activityDIComponent by lazy(LazyThreadSafetyMode.NONE) {
            ActivityDIComponent::class.create(this, appComponent)
        }

        activityDIComponent.appDIComponent.logger.init()

        setContent {
            NavHost(rootComponent = activityDIComponent.rootComponent)
        }
    }
}