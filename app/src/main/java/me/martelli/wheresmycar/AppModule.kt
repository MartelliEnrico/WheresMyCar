package me.martelli.wheresmycar

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import me.martelli.wheresmycar.data.ConfigsRepo
import me.martelli.wheresmycar.data.DevicesRepo

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun provideConfigsRepo(@ApplicationContext context: Context): ConfigsRepo {
        return ConfigsRepo(context)
    }

    @Provides
    @Singleton
    fun provideDevicesRepo(@ApplicationContext context: Context): DevicesRepo {
        return DevicesRepo(context)
    }
}
