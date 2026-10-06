package com.mobilenative.agent.di

import android.content.Context
import com.mobilenative.agent.accessibility.AccessibilityBridge
import com.mobilenative.agent.accessibility.GestureDispatcher
import com.mobilenative.agent.accessibility.ViewTreeExtractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAccessibilityBridge(): AccessibilityBridge {
        return AccessibilityBridge()
    }

    @Provides
    @Singleton
    fun provideViewTreeExtractor(): ViewTreeExtractor {
        return ViewTreeExtractor()
    }

    @Provides
    @Singleton
    fun provideGestureDispatcher(): GestureDispatcher {
        return GestureDispatcher()
    }
}
