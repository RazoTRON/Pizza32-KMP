package com.app.multicourse.feature.about.di

import android.content.Context
import com.app.multicourse.feature.about.util.DialerUtil
import me.tatarka.inject.annotations.Provides

interface AboutModule {

    @Provides
    fun provideDialerUtil(context: Context): DialerUtil {
        return DialerUtil(context)
    }
}