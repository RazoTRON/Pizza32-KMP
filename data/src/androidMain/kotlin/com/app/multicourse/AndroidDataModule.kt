package com.app.multicourse

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.app.multicourse.core.inject.Singleton
import com.app.multicourse.db.AppDb
import me.tatarka.inject.annotations.Provides
import okio.Path.Companion.toPath

private const val dataStoreFileName = "data.preferences_pb"

interface AndroidDataModule {

    @Provides
    @Singleton
    fun provideSqlDriver(context: Context): SqlDriver {
        return AndroidSqliteDriver(AppDb.Schema, context, "AppDb")
    }

    @Provides
    @Singleton
    fun provideDataStore(context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { context.filesDir.resolve(dataStoreFileName).absolutePath.toPath() }
        )
    }
}