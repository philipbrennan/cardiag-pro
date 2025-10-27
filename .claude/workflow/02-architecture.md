# Architecture Patterns and Code Organization

This workflow file covers Android architecture patterns and code organization principles for building robust, maintainable applications.

## Table of Contents
- [Standard Architecture](#standard-architecture)
- [Core Patterns](#core-patterns)
- [Code Organization Principles](#code-organization-principles)
- [Kotlin Idioms](#kotlin-idioms)

---

## Standard Architecture

### MVVM with Repository Pattern

The recommended architecture follows the MVVM (Model-View-ViewModel) pattern combined with the Repository pattern for data access.

#### Project Structure

```
app/src/main/java/com/[domain]/[app]/
├── [AppName]App.kt              # Application class
├── di/                           # Dependency Injection
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── [Feature]Module.kt
├── data/                         # Data Layer
│   ├── model/                    # Data models
│   │   └── [Entity].kt
│   ├── local/                    # Local data source
│   │   ├── [App]Database.kt
│   │   ├── [Entity]Dao.kt
│   │   └── [Entity]Entity.kt
│   ├── remote/                   # Remote data source (if needed)
│   ├── preferences/              # Preferences
│   │   ├── UserPreferences.kt
│   │   └── PreferencesManager.kt
│   └── repository/               # Repository pattern
│       └── [Feature]Repository.kt
├── domain/                       # Business Logic (optional)
│   ├── usecase/
│   └── model/
├── [feature]/                    # Feature modules
│   ├── [Feature]Manager.kt
│   ├── [Feature]Service.kt
│   └── [Feature]Config.kt
├── service/                      # Android Services
│   └── [Feature]Service.kt
├── workers/                      # WorkManager workers
│   └── [Feature]Worker.kt
├── notifications/                # Notification handling
│   └── [Feature]NotificationManager.kt
└── ui/                          # Presentation Layer
    ├── MainActivity.kt
    ├── MainViewModel.kt
    ├── [feature]/
    │   ├── [Feature]Adapter.kt
    │   ├── [Feature]ViewHolder.kt
    │   └── [Feature]ViewModel.kt
    ├── dialogs/
    ├── common/
    │   └── LoadingState.kt
    └── theme/
```

---

## Core Patterns

### ViewModel Pattern

ViewModels manage UI-related data in a lifecycle-conscious way and survive configuration changes.

#### Basic ViewModel Template

```kotlin
@HiltViewModel
class [Feature]ViewModel @Inject constructor(
    private val repository: [Feature]Repository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            repository.getData()
                .catch { error ->
                    _state.value = UiState.Error(error.message)
                }
                .collect { data ->
                    _state.value = UiState.Success(data)
                }
        }
    }
}
```

#### Practical Example: Todo List ViewModel

```kotlin
@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val todoRepository: TodoRepository
) : ViewModel() {

    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadTodos()
    }

    fun loadTodos() {
        viewModelScope.launch {
            _isLoading.value = true
            todoRepository.getAllTodos()
                .catch { error ->
                    Timber.e(error, "Failed to load todos")
                    _error.value = error.message ?: "Unknown error"
                    _isLoading.value = false
                }
                .collect { todoList ->
                    _todos.value = todoList
                    _isLoading.value = false
                }
        }
    }

    fun addTodo(title: String, description: String) {
        viewModelScope.launch {
            try {
                val todo = Todo(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis()
                )
                todoRepository.insertTodo(todo)
                Timber.d("Todo added: $title")
            } catch (e: Exception) {
                Timber.e(e, "Failed to add todo")
                _error.value = "Failed to add todo: ${e.message}"
            }
        }
    }

    fun toggleTodoComplete(todoId: String) {
        viewModelScope.launch {
            try {
                todoRepository.toggleComplete(todoId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle todo")
                _error.value = "Failed to update todo"
            }
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            try {
                todoRepository.deleteTodo(todoId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete todo")
                _error.value = "Failed to delete todo"
            }
        }
    }
}
```

### Repository Pattern

Repositories abstract data sources and provide a clean API for data access.

#### Basic Repository Template

```kotlin
class [Feature]Repository @Inject constructor(
    private val dao: [Entity]Dao,
    private val [dataSource]: [DataSource]
) {
    fun getAll(): Flow<List<[Entity]>> = dao.getAll()

    suspend fun insert(item: [Entity]) = dao.insert(item)

    suspend fun update(item: [Entity]) = dao.update(item)

    suspend fun delete(item: [Entity]) = dao.delete(item)
}
```

#### Practical Example: User Repository with Remote and Local Sources

```kotlin
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val userApi: UserApiService,
    private val preferencesManager: PreferencesManager
) {
    // Get users from local database
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
        .map { entities -> entities.map { it.toDomain() } }

    // Get user by ID (local first, then remote)
    suspend fun getUserById(userId: String): Result<User> {
        return try {
            // Try local first
            val localUser = userDao.getUserById(userId)
            if (localUser != null) {
                Result.Success(localUser.toDomain())
            } else {
                // Fetch from remote and cache
                val remoteUser = userApi.getUserById(userId)
                userDao.insertUser(remoteUser.toEntity())
                Result.Success(remoteUser.toDomain())
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user: $userId")
            Result.Error(e)
        }
    }

    // Sync users from remote
    suspend fun syncUsers(): Result<Unit> {
        return try {
            val remoteUsers = userApi.getAllUsers()
            userDao.deleteAll()
            userDao.insertAll(remoteUsers.map { it.toEntity() })
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync users")
            Result.Error(e)
        }
    }

    // Update user profile
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            // Update remote first
            userApi.updateUser(user.toDto())
            // Then update local
            userDao.updateUser(user.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update user")
            Result.Error(e)
        }
    }

    // Get current logged-in user
    fun getCurrentUser(): Flow<User?> = preferencesManager.currentUserId
        .flatMapLatest { userId ->
            userId?.let { userDao.getUserByIdFlow(it) }
                ?.map { it?.toDomain() }
                ?: flowOf(null)
        }
}
```

### Flow Usage Guidelines

#### When to Use Each Flow Type

- **Flow**: For continuous data streams (database queries, network updates)
- **StateFlow**: For UI state management (always has a value, replays latest)
- **SharedFlow**: For one-time events (navigation, toasts, errors)

#### Flow Best Practices

```kotlin
// 1. Always collect in lifecycle-aware scope
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { state ->
            updateUI(state)
        }
    }
}

// 2. Use appropriate operators
repository.getData()
    .map { data -> data.toUiModel() }  // Transform data
    .filter { it.isValid() }            // Filter items
    .catch { e -> emit(emptyList()) }   // Handle errors
    .collect { updateUI(it) }

// 3. Combine multiple flows
combine(
    userFlow,
    settingsFlow,
    preferencesFlow
) { user, settings, prefs ->
    UiState(user, settings, prefs)
}.collect { state ->
    updateUI(state)
}

// 4. Use SharedFlow for events
private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

fun navigateToDetail(itemId: String) {
    viewModelScope.launch {
        _navigationEvents.emit(NavigationEvent.ToDetail(itemId))
    }
}
```

#### Practical Example: Real-time Search with Debounce

```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<SearchResult>> = searchQuery
        .debounce(300) // Wait 300ms after user stops typing
        .filter { it.length >= 3 } // Only search if 3+ characters
        .distinctUntilChanged() // Don't search if query hasn't changed
        .flatMapLatest { query ->
            searchRepository.search(query)
                .catch { e ->
                    Timber.e(e, "Search failed")
                    emit(emptyList())
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
```

---

## Code Organization Principles

### Core Principles

1. **Single Responsibility**: Each class does one thing
   - ViewModels handle UI logic
   - Repositories handle data access
   - DAOs handle database operations
   - Managers handle specific features

2. **Testability**: All business logic must be unit testable
   - Inject dependencies via constructor
   - Use interfaces for external dependencies
   - Avoid static methods and singletons

3. **Null Safety**: Use Kotlin null safety features extensively
   - Prefer non-null types
   - Use `?.` and `?:` for safe access
   - Use `!!` sparingly and with caution

4. **Structured Concurrency**: Use lifecycle-aware scopes
   - `viewModelScope` for ViewModels
   - `lifecycleScope` for Activities/Fragments
   - Cancel jobs properly to avoid leaks

5. **Error Handling**: Never crash, always log and handle gracefully
   - Use try-catch for operations that can fail
   - Log errors with Timber
   - Provide user-friendly error messages

6. **Defensive Programming**: Validate inputs, handle edge cases
   - Check for null/empty before processing
   - Validate data before database insertion
   - Handle network failures gracefully

### Practical Example: Feature Manager

```kotlin
/**
 * Manages device discovery and connection lifecycle.
 * Follows single responsibility and testability principles.
 */
class DeviceManager @Inject constructor(
    private val context: Context,
    private val deviceRepository: DeviceRepository,
    private val connectivityChecker: ConnectivityChecker
) {
    private val _deviceState = MutableStateFlow<DeviceState>(DeviceState.Idle)
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    private var discoveryJob: Job? = null

    /**
     * Start device discovery.
     * Validates preconditions before starting.
     */
    suspend fun startDiscovery() {
        // Defensive programming: validate state
        if (_deviceState.value is DeviceState.Discovering) {
            Timber.w("Discovery already in progress")
            return
        }

        // Check connectivity
        if (!connectivityChecker.isConnected()) {
            Timber.e("No network connection")
            _deviceState.value = DeviceState.Error("No network connection")
            return
        }

        // Start discovery
        discoveryJob = coroutineScope.launch {
            _deviceState.value = DeviceState.Discovering
            try {
                deviceRepository.discoverDevices()
                    .catch { e ->
                        Timber.e(e, "Discovery failed")
                        _deviceState.value = DeviceState.Error(e.message ?: "Unknown error")
                    }
                    .collect { devices ->
                        _deviceState.value = DeviceState.Success(devices)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected discovery error")
                _deviceState.value = DeviceState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Stop device discovery.
     * Properly cancels coroutines to prevent leaks.
     */
    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _deviceState.value = DeviceState.Idle
        Timber.d("Discovery stopped")
    }

    /**
     * Connect to a specific device.
     * Validates device before connecting.
     */
    suspend fun connectToDevice(deviceId: String): Result<Unit> {
        // Defensive: validate input
        if (deviceId.isBlank()) {
            return Result.Error(IllegalArgumentException("Device ID cannot be blank"))
        }

        return try {
            _deviceState.value = DeviceState.Connecting(deviceId)
            deviceRepository.connect(deviceId)
            _deviceState.value = DeviceState.Connected(deviceId)
            Timber.i("Connected to device: $deviceId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to device: $deviceId")
            _deviceState.value = DeviceState.Error("Connection failed: ${e.message}")
            Result.Error(e)
        }
    }
}

// State sealed class for type-safe state management
sealed class DeviceState {
    object Idle : DeviceState()
    object Discovering : DeviceState()
    data class Connecting(val deviceId: String) : DeviceState()
    data class Connected(val deviceId: String) : DeviceState()
    data class Success(val devices: List<Device>) : DeviceState()
    data class Error(val message: String) : DeviceState()
}
```

---

## Kotlin Idioms

### Use These Kotlin Features

#### 1. Sealed Classes for State Management

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// Usage in ViewModel
when (val state = uiState.value) {
    is UiState.Loading -> showLoading()
    is UiState.Success -> showData(state.data)
    is UiState.Error -> showError(state.message)
}
```

#### 2. Data Classes for Models

```kotlin
data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Add business logic if needed
    fun isEmailVerified(): Boolean = email.isNotBlank()
}
```

#### 3. Extension Functions for Utilities

```kotlin
// String extensions
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

// Date formatting
fun Long.toFormattedDate(): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(this))
}

// View extensions
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

// Usage
if (email.isValidEmail()) {
    submitForm()
}

textView.text = timestamp.toFormattedDate()
progressBar.hide()
```

#### 4. Scope Functions

```kotlin
// apply: configure object
val user = User().apply {
    name = "John Doe"
    email = "john@example.com"
}

// let: null safety and transformation
user?.let { u ->
    displayUser(u)
}

// also: perform side effects
val result = processData()
    .also { Timber.d("Result: $it") }

// run: execute code block with context
val text = binding.run {
    val title = titleTextView.text
    val subtitle = subtitleTextView.text
    "$title - $subtitle"
}

// with: operate on object
with(binding) {
    titleTextView.text = "Title"
    subtitleTextView.text = "Subtitle"
    button.setOnClickListener { /* ... */ }
}
```

#### 5. Null Safety Operators

```kotlin
// Safe call operator
val length = user?.name?.length

// Elvis operator (default value)
val name = user?.name ?: "Unknown"

// Not-null assertion (use sparingly!)
val id = user!!.id  // Only when you're 100% sure it's not null

// Safe cast
val textView = view as? TextView
textView?.text = "Hello"

// let with null check
user?.email?.let { email ->
    sendEmail(email)
}
```

#### 6. Delegated Properties

```kotlin
// Lazy initialization
val database: AppDatabase by lazy {
    Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "app-database"
    ).build()
}

// ViewModel delegation
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
}

// Fragment ViewModel
class MyFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()
}

// Custom delegation
class Preferences(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var username: String by StringPreference(prefs, "username", "")
    var isDarkMode: Boolean by BooleanPreference(prefs, "dark_mode", false)
}
```

### Complete Example: Well-Organized Feature

```kotlin
/**
 * Complete example showing architecture principles and Kotlin idioms
 */

// Domain Model
data class Article(
    val id: String,
    val title: String,
    val content: String,
    val author: String,
    val publishedAt: Long,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false
) {
    fun getFormattedDate(): String = publishedAt.toFormattedDate()
}

// UI State
sealed class ArticleUiState {
    object Loading : ArticleUiState()
    data class Success(val articles: List<Article>) : ArticleUiState()
    data class Error(val message: String) : ArticleUiState()
}

// Repository
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val articleApi: ArticleApiService
) {
    fun getArticles(): Flow<List<Article>> = articleDao.getAllArticles()
        .map { entities -> entities.map { it.toDomain() } }

    suspend fun refreshArticles(): Result<Unit> = try {
        val articles = articleApi.fetchArticles()
        articleDao.deleteAll()
        articleDao.insertAll(articles.map { it.toEntity() })
        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to refresh articles")
        Result.Error(e)
    }

    suspend fun toggleFavorite(articleId: String) {
        articleDao.toggleFavorite(articleId)
    }
}

// ViewModel
@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArticleUiState>(ArticleUiState.Loading)
    val uiState: StateFlow<ArticleUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        loadArticles()
    }

    private fun loadArticles() {
        viewModelScope.launch {
            repository.getArticles()
                .catch { e ->
                    Timber.e(e, "Failed to load articles")
                    _uiState.value = ArticleUiState.Error(e.message ?: "Unknown error")
                }
                .collect { articles ->
                    _uiState.value = ArticleUiState.Success(articles)
                }
        }
    }

    fun refreshArticles() {
        viewModelScope.launch {
            _uiState.value = ArticleUiState.Loading
            when (val result = repository.refreshArticles()) {
                is Result.Success -> loadArticles()
                is Result.Error -> _uiState.value = ArticleUiState.Error(
                    result.exception.message ?: "Failed to refresh"
                )
            }
        }
    }

    fun onArticleClick(articleId: String) {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.ToArticleDetail(articleId))
        }
    }

    fun toggleFavorite(articleId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(articleId)
        }
    }
}

sealed class NavigationEvent {
    data class ToArticleDetail(val articleId: String) : NavigationEvent()
}
```

---

## Tips and Best Practices

### Architecture Tips

1. **Keep ViewModels pure**: No Android framework dependencies (Context, View, etc.)
2. **One ViewModel per screen**: Don't share ViewModels unless data needs to be shared
3. **Use Repository as single source of truth**: All data access goes through repositories
4. **Separate concerns**: UI layer, domain layer, data layer should be independent
5. **Use interfaces for testing**: Create interfaces for managers and services

### Code Organization Tips

1. **Group by feature, not layer**: Organize files by feature module when possible
2. **Keep files small**: One class per file, max 300-400 lines
3. **Use meaningful names**: `UserRepository`, not `UserRepo` or `UR`
4. **Add KDoc comments**: Document public APIs and complex logic
5. **Follow package naming**: `com.company.app.feature.component`

### Common Pitfalls to Avoid

- Don't access database from UI thread
- Don't hold references to Activities/Fragments in ViewModels
- Don't use GlobalScope for coroutines
- Don't expose mutable state from ViewModels
- Don't skip error handling
- Don't hardcode strings/values

---

## Cross-References

- See [03-testing-and-builds.md](./03-testing-and-builds.md) for testing these patterns
- See [04-dependency-injection.md](./04-dependency-injection.md) for Hilt setup
- See [05-database-and-data.md](./05-database-and-data.md) for Room implementation
- See [06-ui-and-patterns.md](./06-ui-and-patterns.md) for UI layer patterns
- See [01-project-planning.md](./01-project-planning.md) for project structure
