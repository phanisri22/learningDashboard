package com.learningdashboard.di

import android.content.Context
import androidx.room.Room
import com.learningdashboard.core.network.AndroidConnectivityObserver
import com.learningdashboard.core.network.ConnectivityObserver
import com.learningdashboard.data.auth.KeystoreSessionStore
import com.learningdashboard.data.auth.SessionStore
import com.learningdashboard.data.local.AppDatabase
import com.learningdashboard.data.local.CourseDao
import com.learningdashboard.data.remote.AuthInterceptor
import com.learningdashboard.data.remote.LearningApi
import com.learningdashboard.data.remote.mock.MockApiInterceptor
import com.learningdashboard.data.repository.DefaultAuthRepository
import com.learningdashboard.data.repository.OfflineFirstCourseRepository
import com.learningdashboard.domain.repository.AuthRepository
import com.learningdashboard.domain.repository.CourseRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        sessionStore: SessionStore,
        connectivityObserver: ConnectivityObserver,
        json: Json,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(sessionStore))
        .addInterceptor(MockApiInterceptor(context.assets, connectivityObserver, json))
        .build()

    @Provides
    @Singleton
    fun provideLearningApi(client: OkHttpClient, json: Json): LearningApi =
        Retrofit.Builder()
            .baseUrl(LearningApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "learning_dashboard.db")
            // v1 rows had no owner, so they can't be assigned to a user; it's only a cache.
            .fallbackToDestructiveMigrationFrom(1)
            .build()

    @Provides
    fun provideCourseDao(database: AppDatabase): CourseDao = database.courseDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    abstract fun bindCourseRepository(impl: OfflineFirstCourseRepository): CourseRepository

    @Binds
    abstract fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository

    @Binds
    abstract fun bindSessionStore(impl: KeystoreSessionStore): SessionStore

    @Binds
    abstract fun bindConnectivityObserver(impl: AndroidConnectivityObserver): ConnectivityObserver
}
