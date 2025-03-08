// ApiModule.kt
package com.example.zim.di

import com.example.zim.api.ActiveUserManager
import com.example.zim.api.Post
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                    // This is important for handling top-level JSON arrays
                    useArrayPolymorphism = true
                })
            }
        }
    }

    @Provides
    @Singleton
    fun provideActiveUserManager(): ActiveUserManager {
        return ActiveUserManager()
    }
}