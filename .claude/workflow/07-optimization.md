# Performance Optimization and Logging

This workflow file covers performance optimization strategies, battery efficiency, memory management, network optimization, and logging with Timber.

## Table of Contents
- [Battery Optimization](#battery-optimization)
- [Memory Management](#memory-management)
- [Network Optimization](#network-optimization)
- [UI Performance](#ui-performance)
- [Logging with Timber](#logging-with-timber)
- [Profiling and Monitoring](#profiling-and-monitoring)

---

## Battery Optimization

### WorkManager for Background Tasks

WorkManager is the recommended solution for deferrable background work that needs guaranteed execution.

#### Basic Worker Implementation

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
class DataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting data sync")

            // Get input data
            val syncType = inputData.getString(KEY_SYNC_TYPE) ?: "full"

            // Perform sync
            when (syncType) {
                "full" -> syncRepository.fullSync()
                "incremental" -> syncRepository.incrementalSync()
                else -> syncRepository.fullSync()
            }

            Timber.d("Data sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Data sync failed")

            // Retry if it's a network error
            if (e is IOException) {
                Timber.d("Network error, will retry")
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_SYNC_TYPE = "sync_type"
        const val WORK_NAME = "data_sync"
    }
}
```

#### Scheduling Periodic Work

```kotlin
package com.example.myapp.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object WorkScheduler {

    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("sync")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DataSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        Timber.d("Periodic sync scheduled")
    }

    fun scheduleOneTimeSync(context: Context, syncType: String = "full") {
        val inputData = workDataOf(
            DataSyncWorker.KEY_SYNC_TYPE to syncType
        )

        val workRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Timber.d("One-time sync scheduled")
    }

    fun cancelSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DataSyncWorker.WORK_NAME)
        Timber.d("Sync cancelled")
    }

    fun observeSyncStatus(context: Context): Flow<WorkInfo> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(DataSyncWorker.WORK_NAME)
            .map { workInfos -> workInfos.firstOrNull() ?: WorkInfo() }
    }
}
```

#### Chaining Work

```kotlin
fun scheduleComplexWorkChain(context: Context) {
    // First work: Download data
    val downloadWork = OneTimeWorkRequestBuilder<DownloadWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    // Second work: Process data
    val processWork = OneTimeWorkRequestBuilder<ProcessWorker>()
        .build()

    // Third work: Upload results
    val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    // Chain the work
    WorkManager.getInstance(context)
        .beginWith(downloadWork)
        .then(processWork)
        .then(uploadWork)
        .enqueue()

    Timber.d("Work chain scheduled")
}
```

### Foreground Service for Ongoing Tasks

```kotlin
package com.example.myapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myapp.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DataSyncService : Service() {

    @Inject
    lateinit var syncManager: SyncManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Timber.d("Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SYNC -> startSync()
            ACTION_STOP_SYNC -> stopSync()
        }

        return START_STICKY
    }

    private fun startSync() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Syncing Data")
            .setContentText("Sync in progress...")
            .setSmallIcon(R.drawable.ic_sync)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            try {
                syncManager.performSync()
                Timber.d("Sync completed")
                stopSelf()
            } catch (e: Exception) {
                Timber.e(e, "Sync failed")
                stopSelf()
            }
        }
    }

    private fun stopSync() {
        serviceScope.cancel()
        stopForeground(true)
        stopSelf()
        Timber.d("Sync stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing sync operations"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        Timber.d("Service destroyed")
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "sync_service"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_SYNC = "com.example.myapp.START_SYNC"
        const val ACTION_STOP_SYNC = "com.example.myapp.STOP_SYNC"
    }
}
```

### Battery Optimization Best Practices

```kotlin
package com.example.myapp.util

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryOptimizationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestBatteryOptimizationExemption() {
        if (!isIgnoringBatteryOptimizations()) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Timber.d("Requested battery optimization exemption")
        }
    }

    fun shouldShowBatteryOptimizationDialog(): Boolean {
        // Show dialog if app requires background work and is not exempted
        return !isIgnoringBatteryOptimizations() && requiresBackgroundWork()
    }

    private fun requiresBackgroundWork(): Boolean {
        // Determine if app needs background work
        // e.g., check if sync is enabled in preferences
        return true
    }
}
```

---

## Memory Management

### Avoiding Memory Leaks

#### Proper Lifecycle Management

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // DON'T: Static reference to Activity (Memory leak!)
    // companion object {
    //     var activity: MainActivity? = null
    // }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Properly scoped coroutine
        lifecycleScope.launch {
            viewModel.data.collect { data ->
                updateUI(data)
            }
        }
    }

    override fun onDestroy() {
        // Clean up resources
        super.onDestroy()
    }
}
```

#### ViewModel Cleanup

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private var updateJob: Job? = null

    fun startUpdates() {
        updateJob = viewModelScope.launch {
            repository.updates.collect { update ->
                processUpdate(update)
            }
        }
    }

    fun stopUpdates() {
        updateJob?.cancel()
        updateJob = null
    }

    override fun onCleared() {
        // Cleanup when ViewModel is destroyed
        stopUpdates()
        Timber.d("ViewModel cleared")
        super.onCleared()
    }
}
```

#### Fragment ViewBinding Cleanup

```kotlin
class MyFragment : Fragment() {

    // Correct way: nullable binding
    private var _binding: FragmentMyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // IMPORTANT: Nullify binding to avoid memory leak
        _binding = null
    }
}
```

### Memory Efficient Data Handling

#### Pagination with Paging 3

```kotlin
@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY created_at DESC")
    fun getArticlesPaginated(): PagingSource<Int, ArticleEntity>
}

@Singleton
class ArticleRepository @Inject constructor(
    private val dao: ArticleDao,
    private val apiService: ApiService
) {
    fun getArticles(): Flow<PagingData<Article>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = { dao.getArticlesPaginated() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }
}
```

#### Efficient Bitmap Loading

```kotlin
package com.example.myapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import timber.log.Timber
import java.io.File

object BitmapUtils {

    fun decodeSampledBitmapFromFile(
        file: File,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode bitmap")
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
```

### LeakCanary Integration

```kotlin
// In app-level build.gradle.kts
dependencies {
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}

// LeakCanary automatically detects memory leaks in debug builds
// No additional setup required!
```

---

## Network Optimization

### Caching Strategy

```kotlin
package com.example.myapp.data.remote

import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object NetworkConfig {

    fun createOkHttpClient(cacheDir: File): OkHttpClient {
        // 10 MB cache
        val cacheSize = 10 * 1024 * 1024L
        val cache = Cache(cacheDir, cacheSize)

        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)

                // Cache for 5 minutes
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=300")
                    .removeHeader("Pragma")
                    .build()
            }
            .build()
    }
}
```

### Retry Logic with Exponential Backoff

```kotlin
package com.example.myapp.util

import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.pow

suspend fun <T> retryWithExponentialBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) {
                Timber.e(e, "Max retries reached")
                throw e
            }

            Timber.w(e, "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms")
            delay(currentDelay)

            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
    }
    throw IllegalStateException("Should not reach here")
}

// Usage
suspend fun fetchData(): Result<Data> {
    return try {
        val data = retryWithExponentialBackoff {
            apiService.getData()
        }
        Result.Success(data)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
```

### Network Connectivity Monitoring

```kotlin
package com.example.myapp.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun observeNetworkState(): Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.d("Network available")
                trySend(NetworkState.Available)
            }

            override fun onLost(network: Network) {
                Timber.d("Network lost")
                trySend(NetworkState.Unavailable)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasInternet = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val isValidated = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

                if (hasInternet && isValidated) {
                    trySend(NetworkState.Available)
                } else {
                    trySend(NetworkState.Unavailable)
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        trySend(if (isNetworkAvailable()) NetworkState.Available else NetworkState.Unavailable)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

sealed class NetworkState {
    object Available : NetworkState()
    object Unavailable : NetworkState()
}
```

---

## UI Performance

### RecyclerView Optimization

```kotlin
class OptimizedAdapter : ListAdapter<Item, ItemViewHolder>(ItemDiffCallback()) {

    // Enable stable IDs for better performance
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder with efficient binding
    class ItemViewHolder(
        private val binding: ItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.apply {
                // Use Coil for efficient image loading
                imageView.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.error)
                }

                textViewTitle.text = item.title
                textViewDescription.text = item.description
            }
        }
    }
}

// In Activity/Fragment
private fun setupRecyclerView() {
    binding.recyclerView.apply {
        layoutManager = LinearLayoutManager(context)
        adapter = optimizedAdapter

        // Performance optimizations
        setHasFixedSize(true)
        setItemViewCacheSize(20)

        // Prefetch
        (layoutManager as? LinearLayoutManager)?.apply {
            isItemPrefetchEnabled = true
            initialPrefetchItemCount = 4
        }
    }
}
```

### LazyColumn Optimization (Compose)

```kotlin
// For Jetpack Compose projects
@Composable
fun OptimizedList(items: List<Item>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = items,
            key = { it.id }  // Stable keys for better performance
        ) { item ->
            ItemCard(
                item = item,
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}
```

### Reducing Overdraw

```kotlin
// Enable ViewBinding to avoid unnecessary findViewById calls
// Use ConstraintLayout to flatten view hierarchy
// Set background only where needed

class OptimizedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOptimizedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOptimizedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Remove window background if activity has its own background
        window.setBackgroundDrawable(null)
    }
}
```

---

## Logging with Timber

### Timber Setup

```kotlin
package com.example.myapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        initializeTimber()
    }

    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            // Debug tree for development
            Timber.plant(Timber.DebugTree())
        } else {
            // Custom tree for production (e.g., Crashlytics)
            Timber.plant(ReleaseTree())
        }

        Timber.d("Timber initialized")
    }
}
```

### Custom Release Tree

```kotlin
package com.example.myapp.util

import android.util.Log
import timber.log.Timber

class ReleaseTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Only log warnings and errors in release builds
        if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
            return
        }

        // Log to crash reporting service
        // FirebaseCrashlytics.getInstance().log(message)

        // Log exception if present
        t?.let {
            // FirebaseCrashlytics.getInstance().recordException(it)
        }
    }
}
```

### Timber Usage Examples

```kotlin
class ExampleClass {

    fun demonstrateLogging() {
        // Debug logging
        Timber.d("Debug message")

        // Info logging
        Timber.i("Info message")

        // Warning
        Timber.w("Warning message")

        // Error with exception
        try {
            riskyOperation()
        } catch (e: Exception) {
            Timber.e(e, "Operation failed")
        }

        // Formatted logging
        val userId = "123"
        val userName = "John"
        Timber.d("User logged in: id=%s, name=%s", userId, userName)

        // Tagged logging
        Timber.tag("CustomTag").d("Custom tagged message")

        // JSON logging
        val json = """{"userId": "123", "action": "login"}"""
        Timber.d("Event: %s", json)
    }

    private fun riskyOperation() {
        throw Exception("Something went wrong")
    }
}
```

### Structured Logging

```kotlin
package com.example.myapp.util

import timber.log.Timber

object Logger {

    fun logUserAction(action: String, details: Map<String, Any>) {
        val message = buildString {
            append("USER_ACTION: $action")
            details.forEach { (key, value) ->
                append(" | $key=$value")
            }
        }
        Timber.i(message)
    }

    fun logNetworkRequest(method: String, url: String, duration: Long) {
        Timber.d("NETWORK: %s %s (took %dms)", method, url, duration)
    }

    fun logDatabaseOperation(operation: String, table: String, count: Int) {
        Timber.d("DATABASE: %s on %s (%d rows)", operation, table, count)
    }

    fun logError(tag: String, message: String, error: Throwable) {
        Timber.tag(tag).e(error, message)
    }
}

// Usage
Logger.logUserAction("login", mapOf(
    "userId" to "123",
    "method" to "email",
    "timestamp" to System.currentTimeMillis()
))

Logger.logNetworkRequest("GET", "/api/users", 234)

Logger.logDatabaseOperation("INSERT", "todos", 1)
```

---

## Profiling and Monitoring

### Android Profiler Usage

```kotlin
// Use Android Studio Profiler for:
// 1. CPU profiling - identify slow methods
// 2. Memory profiling - detect memory leaks
// 3. Network profiling - analyze network calls
// 4. Energy profiling - optimize battery usage

// Add profiling to critical sections
class ProfiledRepository {

    suspend fun loadData(): List<Data> {
        val startTime = System.currentTimeMillis()

        val data = try {
            fetchDataFromNetwork()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            Timber.d("Data fetch took ${duration}ms")
        }

        return data
    }
}
```

### Performance Monitoring

```kotlin
package com.example.myapp.util

import timber.log.Timber

class PerformanceMonitor {

    private val measurements = mutableMapOf<String, Long>()

    fun start(tag: String) {
        measurements[tag] = System.currentTimeMillis()
    }

    fun stop(tag: String) {
        val startTime = measurements.remove(tag)
        if (startTime != null) {
            val duration = System.currentTimeMillis() - startTime
            Timber.d("[$tag] took ${duration}ms")

            // Alert if operation takes too long
            if (duration > SLOW_OPERATION_THRESHOLD) {
                Timber.w("[$tag] SLOW OPERATION: ${duration}ms")
            }
        }
    }

    inline fun <T> measure(tag: String, block: () -> T): T {
        start(tag)
        return try {
            block()
        } finally {
            stop(tag)
        }
    }

    companion object {
        private const val SLOW_OPERATION_THRESHOLD = 1000L // 1 second
    }
}

// Usage
val monitor = PerformanceMonitor()

val result = monitor.measure("database_query") {
    database.complexQuery()
}
```

---

## Tips and Best Practices

### Battery Optimization Tips

1. **Use WorkManager**: For deferrable background work
2. **Respect Doze Mode**: Handle restrictions appropriately
3. **Batch Operations**: Group network/database operations
4. **Optimize Location Updates**: Use appropriate accuracy and intervals
5. **Wake Locks**: Use sparingly and release promptly

### Memory Management Tips

1. **Avoid Memory Leaks**: Always clean up references
2. **Use Weak References**: For listeners and callbacks
3. **Cache Efficiently**: Implement size-limited caches
4. **Load Images Efficiently**: Use sampling and caching libraries
5. **Profile Regularly**: Use Android Profiler to detect issues

### Network Optimization Tips

1. **Cache Responses**: Implement HTTP caching
2. **Compress Data**: Use GZIP compression
3. **Batch Requests**: Reduce number of network calls
4. **Handle Failures**: Implement retry logic
5. **Monitor Connectivity**: Adapt behavior based on network state

### Logging Tips

1. **Use Appropriate Levels**: Debug for dev, Error for issues
2. **Structured Logging**: Use consistent formats
3. **Don't Log Sensitive Data**: PII, passwords, tokens
4. **Remove Debug Logs**: In release builds
5. **Tag Appropriately**: Use meaningful tags

---

## Cross-References

- See [02-architecture.md](./02-architecture.md) for efficient architecture patterns
- See [03-testing-and-builds.md](./03-testing-and-builds.md) for performance testing
- See [04-dependency-injection.md](./04-dependency-injection.md) for efficient DI
- See [05-database-and-data.md](./05-database-and-data.md) for database optimization
- See [06-ui-and-patterns.md](./06-ui-and-patterns.md) for UI performance
