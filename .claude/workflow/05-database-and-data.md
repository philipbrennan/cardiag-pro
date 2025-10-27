# Database with Room and Data Handling Patterns

This workflow file covers Room database implementation, data layer patterns, and error handling strategies.

## Table of Contents
- [Room Database Overview](#room-database-overview)
- [Entity Definition](#entity-definition)
- [DAO Patterns](#dao-patterns)
- [Database Configuration](#database-configuration)
- [Repository Patterns](#repository-patterns)
- [Error Handling](#error-handling)
- [Data Mapping](#data-mapping)

---

## Room Database Overview

Room is an abstraction layer over SQLite that provides:
- Compile-time verification of SQL queries
- Convenience annotations for common database operations
- Integration with LiveData and Flow
- Database migration support

### Room Components

1. **Entity**: Represents a table in the database
2. **DAO**: Contains methods for accessing the database
3. **Database**: Database holder with version and entity list

---

## Entity Definition

### Basic Entity

```kotlin
package com.example.myapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey
    val id: String,

    val title: String,

    val description: String,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

### Entity with Relationships

```kotlin
package com.example.myapp.data.local

import androidx.room.*

// Parent Entity
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null
)

// Child Entity with Foreign Key
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ownerId"])]
)
data class TaskEntity(
    @PrimaryKey
    val taskId: String,
    val title: String,
    val description: String,
    val ownerId: String,
    val isCompleted: Boolean = false
)

// Relation object
data class UserWithTasks(
    @Embedded val user: UserEntity,

    @Relation(
        parentColumn = "userId",
        entityColumn = "ownerId"
    )
    val tasks: List<TaskEntity>
)
```

### Entity with Type Converters

```kotlin
package com.example.myapp.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {

    private val gson = Gson()

    // Date converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // List converters
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return gson.toJson(list)
    }

    // Enum converters
    @TypeConverter
    fun fromPriority(value: String?): TaskPriority? {
        return value?.let { TaskPriority.valueOf(it) }
    }

    @TypeConverter
    fun toPriority(priority: TaskPriority?): String? {
        return priority?.name
    }
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

@Entity(tableName = "advanced_tasks")
data class AdvancedTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val tags: List<String>,
    val priority: TaskPriority,
    val dueDate: Date?,
    val createdAt: Date = Date()
)
```

---

## DAO Patterns

### Basic DAO Operations

```kotlin
package com.example.myapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    // Query - returns Flow for reactive updates
    @Query("SELECT * FROM todos ORDER BY created_at DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: String): TodoEntity?

    @Query("SELECT * FROM todos WHERE id = :id")
    fun getTodoByIdFlow(id: String): Flow<TodoEntity?>

    @Query("SELECT * FROM todos WHERE is_completed = :isCompleted")
    fun getTodosByStatus(isCompleted: Boolean): Flow<List<TodoEntity>>

    // Insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<TodoEntity>)

    // Update
    @Update
    suspend fun update(todo: TodoEntity)

    @Query("UPDATE todos SET is_completed = :isCompleted WHERE id = :id")
    suspend fun updateCompletionStatus(id: String, isCompleted: Boolean)

    // Delete
    @Delete
    suspend fun delete(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM todos")
    suspend fun deleteAll()

    @Query("DELETE FROM todos WHERE is_completed = 1")
    suspend fun deleteCompleted()

    // Count
    @Query("SELECT COUNT(*) FROM todos")
    fun getTodoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE is_completed = 0")
    fun getActiveTodoCount(): Flow<Int>
}
```

### Advanced DAO Queries

```kotlin
@Dao
interface AdvancedTodoDao {

    // Search with LIKE
    @Query("""
        SELECT * FROM todos
        WHERE title LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        ORDER BY created_at DESC
    """)
    fun searchTodos(query: String): Flow<List<TodoEntity>>

    // Multiple conditions
    @Query("""
        SELECT * FROM todos
        WHERE is_completed = :isCompleted
        AND created_at >= :fromDate
        AND created_at <= :toDate
        ORDER BY created_at DESC
    """)
    fun getTodosInDateRange(
        isCompleted: Boolean,
        fromDate: Long,
        toDate: Long
    ): Flow<List<TodoEntity>>

    // Aggregation
    @Query("""
        SELECT
            COUNT(*) as total,
            SUM(CASE WHEN is_completed = 1 THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN is_completed = 0 THEN 1 ELSE 0 END) as active
        FROM todos
    """)
    suspend fun getTodoStats(): TodoStats

    // Join query
    @Query("""
        SELECT tasks.* FROM tasks
        INNER JOIN users ON tasks.ownerId = users.userId
        WHERE users.email = :userEmail
        ORDER BY tasks.created_at DESC
    """)
    fun getTasksForUserEmail(userEmail: String): Flow<List<TaskEntity>>

    // Transaction
    @Transaction
    @Query("SELECT * FROM users")
    fun getUsersWithTasks(): Flow<List<UserWithTasks>>

    // Pagination
    @Query("SELECT * FROM todos ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getTodosPaginated(limit: Int, offset: Int): List<TodoEntity>

    // Bulk operations
    @Transaction
    suspend fun replaceAll(todos: List<TodoEntity>) {
        deleteAll()
        insertAll(todos)
    }
}

data class TodoStats(
    val total: Int,
    val completed: Int,
    val active: Int
)
```

### DAO with Raw Queries

```kotlin
@Dao
interface RawQueryDao {

    // RawQuery for dynamic queries
    @RawQuery
    suspend fun getTodosViaQuery(query: SupportSQLiteQuery): List<TodoEntity>

    // Usage
    suspend fun getDynamicTodos(sortBy: String, ascending: Boolean): List<TodoEntity> {
        val order = if (ascending) "ASC" else "DESC"
        val query = SimpleSQLiteQuery(
            "SELECT * FROM todos ORDER BY $sortBy $order"
        )
        return getTodosViaQuery(query)
    }
}
```

---

## Database Configuration

### Basic Database Setup

```kotlin
package com.example.myapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TodoEntity::class,
        UserEntity::class,
        TaskEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao
    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
}
```

### Database Module with Hilt

```kotlin
package com.example.myapp.di

import android.content.Context
import androidx.room.Room
import com.example.myapp.data.local.*
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
            // .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // For production
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

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }
}
```

### Database Migrations

```kotlin
package com.example.myapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Migration from version 1 to 2
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new column
        database.execSQL(
            "ALTER TABLE todos ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM'"
        )
    }
}

// Migration from version 2 to 3
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create new table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS categories (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL
            )
        """)

        // Add foreign key to existing table
        database.execSQL("""
            CREATE TABLE todos_new (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                is_completed INTEGER NOT NULL,
                priority TEXT NOT NULL,
                category_id TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
            )
        """)

        // Copy data
        database.execSQL("""
            INSERT INTO todos_new (id, title, description, is_completed, priority, created_at, updated_at)
            SELECT id, title, description, is_completed, priority, created_at, updated_at
            FROM todos
        """)

        // Drop old table
        database.execSQL("DROP TABLE todos")

        // Rename new table
        database.execSQL("ALTER TABLE todos_new RENAME TO todos")
    }
}

// Apply migrations in DatabaseModule
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "app_database"
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
}
```

---

## Repository Patterns

### Complete Repository Implementation

```kotlin
package com.example.myapp.data.repository

import com.example.myapp.data.local.TodoDao
import com.example.myapp.data.local.TodoEntity
import com.example.myapp.data.model.Todo
import com.example.myapp.data.remote.ApiService
import com.example.myapp.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface TodoRepository {
    fun getAllTodos(): Flow<List<Todo>>
    fun getTodoById(id: String): Flow<Todo?>
    fun getActiveTodos(): Flow<List<Todo>>
    fun getCompletedTodos(): Flow<List<Todo>>
    suspend fun addTodo(title: String, description: String): Result<Todo>
    suspend fun updateTodo(todo: Todo): Result<Unit>
    suspend fun toggleTodoCompletion(todoId: String): Result<Unit>
    suspend fun deleteTodo(todoId: String): Result<Unit>
    suspend fun deleteCompletedTodos(): Result<Unit>
    suspend fun syncWithRemote(): Result<Unit>
}

@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val todoDao: TodoDao,
    private val apiService: ApiService
) : TodoRepository {

    override fun getAllTodos(): Flow<List<Todo>> {
        return todoDao.getAllTodos()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { e ->
                Timber.e(e, "Error getting all todos")
                emit(emptyList())
            }
    }

    override fun getTodoById(id: String): Flow<Todo?> {
        return todoDao.getTodoByIdFlow(id)
            .map { it?.toDomain() }
            .catch { e ->
                Timber.e(e, "Error getting todo: $id")
                emit(null)
            }
    }

    override fun getActiveTodos(): Flow<List<Todo>> {
        return todoDao.getTodosByStatus(false)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getCompletedTodos(): Flow<List<Todo>> {
        return todoDao.getTodosByStatus(true)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun addTodo(title: String, description: String): Result<Todo> {
        return try {
            val todo = Todo(
                id = generateId(),
                title = title,
                description = description,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )

            // Insert locally
            todoDao.insert(todo.toEntity())
            Timber.d("Todo added: ${todo.id}")

            // Sync to remote
            try {
                apiService.createTodo(todo.toDto())
                Timber.d("Todo synced to remote: ${todo.id}")
            } catch (e: Exception) {
                Timber.w(e, "Failed to sync todo to remote")
                // Continue even if remote sync fails
            }

            Result.Success(todo)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add todo")
            Result.Error(e)
        }
    }

    override suspend fun updateTodo(todo: Todo): Result<Unit> {
        return try {
            todoDao.update(todo.toEntity())
            Timber.d("Todo updated: ${todo.id}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update todo")
            Result.Error(e)
        }
    }

    override suspend fun toggleTodoCompletion(todoId: String): Result<Unit> {
        return try {
            val todo = todoDao.getTodoById(todoId)
            if (todo != null) {
                todoDao.updateCompletionStatus(todoId, !todo.isCompleted)
                Timber.d("Todo completion toggled: $todoId")
                Result.Success(Unit)
            } else {
                Result.Error(IllegalArgumentException("Todo not found: $todoId"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle todo completion")
            Result.Error(e)
        }
    }

    override suspend fun deleteTodo(todoId: String): Result<Unit> {
        return try {
            todoDao.deleteById(todoId)
            Timber.d("Todo deleted: $todoId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete todo")
            Result.Error(e)
        }
    }

    override suspend fun deleteCompletedTodos(): Result<Unit> {
        return try {
            todoDao.deleteCompleted()
            Timber.d("Completed todos deleted")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete completed todos")
            Result.Error(e)
        }
    }

    override suspend fun syncWithRemote(): Result<Unit> {
        return try {
            Timber.d("Starting remote sync")
            val remoteTodos = apiService.getAllTodos()

            // Replace local data with remote data
            todoDao.replaceAll(remoteTodos.map { it.toEntity() })

            Timber.d("Synced ${remoteTodos.size} todos from remote")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync with remote")
            Result.Error(e)
        }
    }

    private fun generateId(): String = java.util.UUID.randomUUID().toString()
}
```

### Repository with Caching Strategy

```kotlin
@Singleton
class ArticleRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val apiService: ApiService
) : ArticleRepository {

    private val refreshInterval = 5 * 60 * 1000L // 5 minutes
    private var lastRefreshTime = 0L

    override fun getArticles(): Flow<List<Article>> {
        // Return local data first
        return articleDao.getAllArticles()
            .map { entities -> entities.map { it.toDomain() } }
            .onStart {
                // Refresh from remote if stale
                if (shouldRefresh()) {
                    refreshFromRemote()
                }
            }
    }

    private fun shouldRefresh(): Boolean {
        return System.currentTimeMillis() - lastRefreshTime > refreshInterval
    }

    private suspend fun refreshFromRemote() {
        try {
            val articles = apiService.getArticles()
            articleDao.replaceAll(articles.map { it.toEntity() })
            lastRefreshTime = System.currentTimeMillis()
            Timber.d("Articles refreshed from remote")
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh articles")
            // Don't throw - use cached data
        }
    }

    override suspend fun forceRefresh(): Result<Unit> {
        return try {
            refreshFromRemote()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

---

## Error Handling

### Result Wrapper Pattern

```kotlin
package com.example.myapp.util

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Result is still loading")
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Throwable) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }
}
```

### UI State Pattern

```kotlin
package com.example.myapp.ui.common

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val exception: Throwable? = null) : UiState<Nothing>()
    object Empty : UiState<Nothing>()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isEmpty: Boolean get() = this is Empty
}

// Extension for mapping
fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> {
    return when (this) {
        is UiState.Success -> UiState.Success(transform(data))
        is UiState.Error -> this
        is UiState.Loading -> UiState.Loading
        is UiState.Empty -> UiState.Empty
    }
}
```

### Error Handling in ViewModels

```kotlin
@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val repository: TodoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Todo>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Todo>>> = _uiState.asStateFlow()

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            repository.getAllTodos()
                .catch { error ->
                    Timber.e(error, "Failed to load todos")
                    _uiState.value = UiState.Error(
                        message = error.localizedMessage ?: "Failed to load todos",
                        exception = error
                    )
                }
                .collect { todos ->
                    _uiState.value = if (todos.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(todos)
                    }
                }
        }
    }

    fun addTodo(title: String, description: String) {
        viewModelScope.launch {
            when (val result = repository.addTodo(title, description)) {
                is Result.Success -> {
                    Timber.d("Todo added successfully")
                    // UI is updated automatically via Flow
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to add todo")
                    _error.value = "Failed to add todo: ${result.exception.message}"
                }
                is Result.Loading -> { /* Not used in this case */ }
            }
        }
    }
}
```

---

## Data Mapping

### Mapping Extensions

```kotlin
package com.example.myapp.data.mapper

import com.example.myapp.data.local.TodoEntity
import com.example.myapp.data.model.Todo
import com.example.myapp.data.remote.TodoDto

// Entity to Domain
fun TodoEntity.toDomain(): Todo {
    return Todo(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// Domain to Entity
fun Todo.toEntity(): TodoEntity {
    return TodoEntity(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// DTO to Entity
fun TodoDto.toEntity(): TodoEntity {
    return TodoEntity(
        id = id,
        title = title,
        description = description ?: "",
        isCompleted = completed ?: false,
        createdAt = createdAt ?: System.currentTimeMillis(),
        updatedAt = updatedAt ?: System.currentTimeMillis()
    )
}

// Domain to DTO
fun Todo.toDto(): TodoDto {
    return TodoDto(
        id = id,
        title = title,
        description = description,
        completed = isCompleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// List extensions
fun List<TodoEntity>.toDomain(): List<Todo> = map { it.toDomain() }
fun List<Todo>.toEntity(): List<TodoEntity> = map { it.toEntity() }
fun List<TodoDto>.toEntity(): List<TodoEntity> = map { it.toEntity() }
```

---

## Tips and Best Practices

### Database Best Practices

1. **Use Flow for Reactive Updates**: Always return Flow from DAOs for real-time updates
2. **Suspend Functions for One-Shot Operations**: Use suspend for insert, update, delete
3. **Index Foreign Keys**: Always add indices on foreign key columns
4. **Use Transactions**: Wrap related operations in `@Transaction`
5. **Handle Migrations**: Plan migrations for production apps
6. **Export Schema**: Enable exportSchema for tracking database versions

### Repository Best Practices

1. **Single Source of Truth**: Database is the source of truth
2. **Offline-First**: Return local data first, sync in background
3. **Error Recovery**: Handle errors gracefully, don't crash
4. **Cache Smartly**: Implement appropriate caching strategies
5. **Log Everything**: Use Timber for tracking data operations

### Common Pitfalls to Avoid

- Don't access database on main thread
- Don't ignore migration errors
- Don't expose entities to UI layer
- Don't forget to handle null cases
- Don't skip error handling in repositories
- Don't hardcode database name

---

## Cross-References

- See [02-architecture.md](./02-architecture.md) for repository pattern architecture
- See [03-testing-and-builds.md](./03-testing-and-builds.md) for Room testing strategies
- See [04-dependency-injection.md](./04-dependency-injection.md) for Room + Hilt integration
- See [06-ui-and-patterns.md](./06-ui-and-patterns.md) for UI state management with Room
- See [07-optimization.md](./07-optimization.md) for database performance optimization
