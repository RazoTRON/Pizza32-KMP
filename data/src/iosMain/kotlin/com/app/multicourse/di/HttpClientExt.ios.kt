package com.app.multicourse.di

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun <T : HttpClientEngineConfig> getPlatformHttpEngine(): HttpClientEngineFactory<T> = Darwin as HttpClientEngineFactory<T>