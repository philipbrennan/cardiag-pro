# Dependency Injection with Hilt

This workflow file covers comprehensive Hilt dependency injection setup and patterns for Android applications.

## Table of Contents
- [Hilt Overview](#hilt-overview)
- [Application Setup](#application-setup)
- [Module Structure](#module-structure)
- [Injection Patterns](#injection-patterns)
- [Complete Examples](#complete-examples)
- [Testing with Hilt](#testing-with-hilt)

---

## Hilt Overview

Hilt is a dependency injection library for Android built on top of Dagger. It reduces the boilerplate of doing manual dependency injection and provides a standard way to incorporate Dagger into an Android application.

### Key Benefits

- Compile-time correctness
- Runtime performance
- Automatic lifecycle management
- Standard Android components support
- Easy testing with test modules

### Hilt Components Hierarchy

```
SingletonComponent (Application lifecycle)
    ↓
ActivityRetainedComponent (Survives configuration changes)
    ↓
ViewModelComponent (ViewModel lifecycle)
    ↓
ActivityComponent (Activity lifecycle)
    ↓
FragmentComponent (Fragment lifecycle)
    ↓
ViewComponent (View lifecycle)
```

---

## Application Setup

### 1. Add Hilt Dependencies

In project-level `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
```

In app-level `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    // For testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.48")
}
```

### 2. Create Application Class

```kotlin
package com.example.myapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("Application started")
    }
}
```

### 3. Update AndroidManifest.xml

```xml
<application
    android:name=".MyApp"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/Theme.MyApp">

    <!-- Activities and services -->

</application>
```

---

## Module Structure

### Application Module

Provides application-wide dependencies.

```kotlin
package com.example.myapp.di

import android.content.Context
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
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
    }
}
```

### Database Module

Provides Room database and DAOs.

```kotlin
package com.example.myapp.di

import android.content.Context
import androidx.room.Room
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.TodoDao
import com.example.myapp.data.local.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        )
            .fallbackToDestructiveMigration() // For development only
            .build()
    }

    @Provides
    @Singleton
    fun provideTodoDao(database: AppDatabase): TodoDao {
        return database.todoDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}
```

### Network Module

Provides networking dependencies.

```kotlin
package com.example.myapp.di

import com.example.myapp.BuildConfig
import com.example.myapp.data.remote.ApiService
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
```

### Repository Module

Binds repository implementations to interfaces.

```kotlin
package com.example.myapp.di

import com.example.myapp.data.repository.TodoRepository
import com.example.myapp.data.repository.TodoRepositoryImpl
import com.example.myapp.data.repository.UserRepository
import com.example.myapp.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTodoRepository(
        impl: TodoRepositoryImpl
    ): TodoRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository
}
```

### Feature-Specific Module

```kotlin
package com.example.myapp.di

import com.example.myapp.feature.bluetooth.BluetoothManager
import com.example.myapp.feature.location.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeatureModule {

    @Provides
    @Singleton
    fun provideBluetoothManager(
        @ApplicationContext context: Context
    ): BluetoothManager {
        return BluetoothManager(context)
    }

    @Provides
    @Singleton
    fun provideLocationManager(
        @ApplicationContext context: Context
    ): LocationManager {
        return LocationManager(context)
    }
}
```

---

## Injection Patterns

### ViewModel Injection

ViewModels are automatically scoped to their owner's lifecycle.

```kotlin
package com.example.myapp.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val todoRepository: TodoRepository
) : ViewModel() {

    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadTodos()
    }

    fun loadTodos() {
        viewModelScope.launch {
            _isLoading.value = true
            todoRepository.getAllTodos()
                .catch { e ->
                    Timber.e(e, "Failed to load todos")
                }
                .collect { todoList ->
                    _todos.value = todoList
                    _isLoading.value = false
                }
        }
    }

    fun addTodo(title: String, description: String) {
        viewModelScope.launch {
            todoRepository.addTodo(title, description)
        }
    }
}
```

### Activity Injection

Activities must be annotated with `@AndroidEntryPoint`.

```kotlin
package com.example.myapp.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // Field injection (use constructor injection in ViewModels instead)
    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()

        // Use injected dependencies
        analyticsManager.logScreenView("MainActivity")
    }

    private fun setupUI() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = todoAdapter
        }

        binding.fabAdd.setOnClickListener {
            showAddTodoDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.todos.collect { todos ->
                updateUI(todos)
            }
        }
    }
}
```

### Fragment Injection

Fragments must also be annotated with `@AndroidEntryPoint`.

```kotlin
package com.example.myapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.myapp.databinding.FragmentTodoDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TodoDetailFragment : Fragment() {

    private var _binding: FragmentTodoDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TodoDetailViewModel by viewModels()

    // Field injection for dependencies not suitable for ViewModel
    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todo.collect { todo ->
                todo?.let { displayTodo(it) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### Service Injection

Android Services require special handling with `@AndroidEntryPoint`.

```kotlin
package com.example.myapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DataSyncService : Service() {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        Timber.d("Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("Service started")

        // Use injected dependencies
        syncManager.startSync()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        syncManager.stopSync()
        Timber.d("Service destroyed")
        super.onDestroy()
    }
}
```

### Worker Injection

WorkManager workers use `HiltWorker` annotation.

```kotlin
package com.example.myapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapp.data.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting sync work")
            syncRepository.syncData()
            Timber.d("Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Sync failed")
            Result.retry()
        }
    }
}
```

---

## Complete Examples

### Example 1: Repository with Multiple Dependencies

```kotlin
package com.example.myapp.data.repository

import com.example.myapp.data.local.TodoDao
import com.example.myapp.data.model.Todo
import com.example.myapp.data.preferences.PreferencesManager
import com.example.myapp.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface TodoRepository {
    fun getAllTodos(): Flow<List<Todo>>
    suspend fun addTodo(title: String, description: String)
    suspend fun updateTodo(todo: Todo)
    suspend fun deleteTodo(todoId: String)
    suspend fun syncTodos(): Result<Unit>
}

@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val todoDao: TodoDao,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) : TodoRepository {

    override fun getAllTodos(): Flow<List<Todo>> {
        return todoDao.getAllTodos()
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun addTodo(title: String, description: String) {
        val todo = Todo(
            id = generateId(),
            title = title,
            description = description,
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )

        todoDao.insert(todo.toEntity())
        Timber.d("Todo added: $title")

        // Sync to remote if user is logged in
        if (preferencesManager.isLoggedIn()) {
            try {
                apiService.createTodo(todo.toDto())
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync todo to remote")
            }
        }
    }

    override suspend fun updateTodo(todo: Todo) {
        todoDao.update(todo.toEntity())
        Timber.d("Todo updated: ${todo.id}")
    }

    override suspend fun deleteTodo(todoId: String) {
        todoDao.deleteById(todoId)
        Timber.d("Todo deleted: $todoId")
    }

    override suspend fun syncTodos(): Result<Unit> {
        return try {
            val remoteTodos = apiService.getTodos()
            todoDao.deleteAll()
            todoDao.insertAll(remoteTodos.map { it.toEntity() })
            Timber.d("Synced ${remoteTodos.size} todos")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync todos")
            Result.Error(e)
        }
    }

    private fun generateId(): String = UUID.randomUUID().toString()
}
```

### Example 2: Manager Class with Context

```kotlin
package com.example.myapp.feature.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General app notifications"
                },
                NotificationChannel(
                    CHANNEL_ID_IMPORTANT,
                    "Important Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Important notifications that require attention"
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }

            Timber.d("Notification channels created")
        }
    }

    fun showNotification(
        title: String,
        message: String,
        channelId: String = CHANNEL_ID_GENERAL
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Timber.d("Notification shown: $title")
    }

    companion object {
        private const val CHANNEL_ID_GENERAL = "general"
        private const val CHANNEL_ID_IMPORTANT = "important"
    }
}
```

### Example 3: ViewModel with Multiple Repositories

```kotlin
package com.example.myapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.repository.TodoRepository
import com.example.myapp.data.repository.UserRepository
import com.example.myapp.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val userRepository: UserRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            combine(
                todoRepository.getAllTodos(),
                userRepository.getCurrentUser(),
                statsRepository.getStats()
            ) { todos, user, stats ->
                DashboardData(
                    todos = todos,
                    user = user,
                    stats = stats
                )
            }
                .catch { e ->
                    Timber.e(e, "Failed to load dashboard")
                    _uiState.value = DashboardUiState.Error(
                        e.message ?: "Failed to load dashboard"
                    )
                }
                .collect { data ->
                    _uiState.value = DashboardUiState.Success(data)
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            loadDashboard()
        }
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val data: DashboardData) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

data class DashboardData(
    val todos: List<Todo>,
    val user: User?,
    val stats: Stats
)
```

### Example 4: Qualifiers for Multiple Implementations

```kotlin
// Define qualifiers
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalDataSource

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteDataSource

// Module providing qualified dependencies
@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    @LocalDataSource
    fun provideLocalDataSource(
        dao: DataDao
    ): DataSource {
        return LocalDataSourceImpl(dao)
    }

    @Provides
    @Singleton
    @RemoteDataSource
    fun provideRemoteDataSource(
        apiService: ApiService
    ): DataSource {
        return RemoteDataSourceImpl(apiService)
    }
}

// Using qualified dependencies
@Singleton
class DataRepository @Inject constructor(
    @LocalDataSource private val localDataSource: DataSource,
    @RemoteDataSource private val remoteDataSource: DataSource
) {
    suspend fun syncData() {
        val remoteData = remoteDataSource.getData()
        localDataSource.saveData(remoteData)
    }
}
```

---

## Testing with Hilt

### Test Module

Create test-specific modules that override production modules.

```kotlin
package com.example.myapp

import com.example.myapp.data.repository.TodoRepository
import com.example.myapp.data.repository.FakeTodoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
object TestRepositoryModule {

    @Provides
    @Singleton
    fun provideTodoRepository(): TodoRepository {
        return FakeTodoRepository()
    }
}
```

### Instrumentation Test with Hilt

```kotlin
package com.example.myapp.ui

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapp.data.repository.TodoRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Inject
    lateinit var repository: TodoRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testTodoList() {
        // Test implementation using injected repository
    }
}
```

---

## Tips and Best Practices

### Injection Best Practices

1. **Prefer Constructor Injection**: Use constructor injection over field injection when possible
2. **Use Interfaces**: Define interfaces for repositories and managers for easier testing
3. **Scope Appropriately**: Use `@Singleton` only when needed
4. **Avoid Context Leaks**: Use `@ApplicationContext` qualifier for Context
5. **Document Dependencies**: Add comments explaining complex dependency graphs

### Module Organization

1. **Separate by Concern**: Create separate modules for different layers (Database, Network, Repository)
2. **Use `@Binds` for Interfaces**: More efficient than `@Provides`
3. **Keep Modules Focused**: Each module should have a single responsibility
4. **Avoid Circular Dependencies**: Restructure code if circular dependencies occur

### Common Pitfalls to Avoid

- Don't inject Android framework types directly (use wrappers)
- Don't use field injection in ViewModels
- Don't forget `@AndroidEntryPoint` annotation on Activities/Fragments
- Don't create circular dependencies
- Don't over-use `@Singleton` scope

---

## Cross-References

- See [02-architecture.md](./02-architecture.md) for architecture patterns using Hilt
- See [03-testing-and-builds.md](./03-testing-and-builds.md) for testing with Hilt
- See [05-database-and-data.md](./05-database-and-data.md) for Room + Hilt integration
- See [06-ui-and-patterns.md](./06-ui-and-patterns.md) for UI component injection
- See [01-project-planning.md](./01-project-planning.md) for DI setup in project structure
