package com.[domain].[app].di

import android.content.Context
import androidx.room.Room
import com.[domain].[app].data.local.[App]Database
import com.[domain].[app].data.local.[Entity]Dao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provide application context
     * (Usually not needed as Hilt provides this automatically)
     */
    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context = context
}

/**
 * Hilt module for database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provide Room database instance
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): [App]Database {
        return Room.databaseBuilder(
            context,
            [App]Database::class.java,
            "[database-name]"
        )
            .fallbackToDestructiveMigration() // Remove in production
            .build()
    }

    /**
     * Provide [Entity] DAO
     */
    @Provides
    fun provide[Entity]Dao(database: [App]Database): [Entity]Dao {
        return database.[entity]Dao()
    }
}

/**
 * Hilt module for network dependencies (if needed).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provide OkHttpClient
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    /**
     * Provide Retrofit instance
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: okhttp3.OkHttpClient): retrofit2.Retrofit {
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
    }

    /**
     * Provide API service
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: retrofit2.Retrofit): [Feature]ApiService {
        return retrofit.create([Feature]ApiService::class.java)
    }
}

/**
 * Hilt module for repository dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // Repositories are typically provided via @Inject constructor
    // Only add explicit @Provides here if you need custom configuration

    /**
     * Example: Provide repository with custom configuration
     */
    /*
    @Provides
    @Singleton
    fun provide[Feature]Repository(
        dao: [Entity]Dao,
        apiService: [Feature]ApiService
    ): [Feature]Repository {
        return [Feature]Repository(dao, apiService)
    }
    */
}

/**
 * Hilt module for worker dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    // WorkManager workers use HiltWorkerFactory automatically
    // No explicit providers needed unless using qualifiers
}

/**
 * Example: Using qualifiers for multiple implementations
 */
@Module
@InstallIn(SingletonComponent::class)
object QualifierExampleModule {

    @Provides
    @Singleton
    @LocalDataSource
    fun provideLocalDataSource(dao: [Entity]Dao): [Feature]DataSource {
        return Local[Feature]DataSource(dao)
    }

    @Provides
    @Singleton
    @RemoteDataSource
    fun provideRemoteDataSource(apiService: [Feature]ApiService): [Feature]DataSource {
        return Remote[Feature]DataSource(apiService)
    }
}

// Qualifier annotations
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalDataSource

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteDataSource
