# Testing Strategy and Gradle Configuration

This workflow file covers comprehensive testing strategies and Gradle build configuration for Android projects.

## Table of Contents
- [Testing Strategy](#testing-strategy)
- [Test Structure](#test-structure)
- [Testing Framework Setup](#testing-framework-setup)
- [Testing Examples](#testing-examples)
- [Gradle Configuration](#gradle-configuration)
- [Build Best Practices](#build-best-practices)

---

## Testing Strategy

### Test Coverage Goals

- **Minimum 80% overall coverage**
- **100% coverage for critical paths**
- Unit tests for all business logic
- Integration tests for component interactions
- UI tests for user flows

### Testing Pyramid

```
        /\
       /  \        E2E Tests (Few)
      /----\       - Critical user flows
     /      \      - Key scenarios
    /--------\     Integration Tests (Some)
   /          \    - Component interactions
  /------------\   - Repository + DAO
 /______________\  Unit Tests (Many)
                   - ViewModels
                   - Repositories
                   - Utilities
                   - Parsers
```

### Testing Approach per Iteration

1. Write unit tests for each new component
2. Write integration tests for component interactions
3. Write UI tests for new user-facing features
4. Verify all tests pass before committing
5. Never skip tests to save time

---

## Test Structure

### Directory Organization

```
app/src/
├── test/                         # Unit tests (JUnit 5)
│   └── java/com/[domain]/[app]/
│       ├── [feature]/
│       │   ├── [Manager]Test.kt
│       │   └── [Service]Test.kt
│       ├── data/
│       │   └── repository/
│       │       └── [Repository]Test.kt
│       ├── ui/
│       │   └── viewmodel/
│       │       └── [ViewModel]Test.kt
│       ├── parser/
│       │   └── [Parser]Test.kt
│       └── util/
│           └── [Utility]Test.kt
└── androidTest/                  # Instrumentation tests (Espresso)
    └── java/com/[domain]/[app]/
        ├── ui/
        │   ├── [Activity]Test.kt
        │   └── [Fragment]Test.kt
        ├── database/
        │   └── [Dao]Test.kt
        └── integration/
            └── [Feature]IntegrationTest.kt
```

---

## Testing Framework Setup

### Dependencies in `build.gradle.kts`

```kotlin
dependencies {
    // Unit Testing - JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")

    // Coroutines Testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // MockK for mocking
    testImplementation("io.mockk:mockk:1.13.8")

    // Architecture Components Testing
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Truth for assertions
    testImplementation("com.google.truth:truth:1.1.5")

    // Turbine for Flow testing
    testImplementation("app.cash.turbine:turbine:1.0.0")

    // Android Testing - Espresso
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")

    // Hilt Testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.48")

    // Room Testing
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
```

### JUnit 5 Configuration in `build.gradle.kts`

```kotlin
android {
    // ... other config

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }

        // Return default values for non-mocked methods
        unitTests.isReturnDefaultValues = true
    }
}
```

---

## Testing Examples

### Unit Testing ViewModels

#### Basic ViewModel Test

```kotlin
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class TodoListViewModelTest {

    private lateinit var viewModel: TodoListViewModel
    private lateinit var repository: TodoRepository

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk()
        viewModel = TodoListViewModel(repository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadTodos should update state with todos from repository`() = runTest {
        // Given
        val expectedTodos = listOf(
            Todo(id = "1", title = "Test Todo 1", description = "", isCompleted = false),
            Todo(id = "2", title = "Test Todo 2", description = "", isCompleted = true)
        )
        every { repository.getAllTodos() } returns flowOf(expectedTodos)

        // When
        viewModel.loadTodos()
        advanceUntilIdle()

        // Then
        assertEquals(expectedTodos, viewModel.todos.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `addTodo should insert todo through repository`() = runTest {
        // Given
        val title = "New Todo"
        val description = "Description"
        coEvery { repository.insertTodo(any()) } just Runs

        // When
        viewModel.addTodo(title, description)
        advanceUntilIdle()

        // Then
        coVerify { repository.insertTodo(match {
            it.title == title && it.description == description
        }) }
    }

    @Test
    fun `loadTodos should handle error from repository`() = runTest {
        // Given
        val errorMessage = "Database error"
        every { repository.getAllTodos() } throws RuntimeException(errorMessage)

        // When
        viewModel.loadTodos()
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }
}
```

#### Testing with Turbine (Flow Testing)

```kotlin
import app.cash.turbine.test
import kotlin.test.assertEquals

@Test
fun `search results should update when query changes`() = runTest {
    // Given
    val query = "test"
    val expectedResults = listOf(
        SearchResult(id = "1", title = "Test Result")
    )
    coEvery { repository.search(query) } returns flowOf(expectedResults)

    // When & Then
    viewModel.searchResults.test {
        // Initial empty state
        assertEquals(emptyList(), awaitItem())

        // Update query
        viewModel.updateSearchQuery(query)
        advanceTimeBy(301) // Debounce delay

        // Verify results
        assertEquals(expectedResults, awaitItem())
    }
}
```

### Unit Testing Repositories

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryTest {

    private lateinit var repository: UserRepository
    private lateinit var userDao: UserDao
    private lateinit var userApi: UserApiService
    private lateinit var preferencesManager: PreferencesManager

    @BeforeEach
    fun setup() {
        userDao = mockk()
        userApi = mockk()
        preferencesManager = mockk()
        repository = UserRepository(userDao, userApi, preferencesManager)
    }

    @Test
    fun `getAllUsers should return users from dao`() = runTest {
        // Given
        val userEntities = listOf(
            UserEntity(id = "1", name = "John", email = "john@example.com"),
            UserEntity(id = "2", name = "Jane", email = "jane@example.com")
        )
        every { userDao.getAllUsers() } returns flowOf(userEntities)

        // When
        val result = repository.getAllUsers().first()

        // Then
        assertEquals(2, result.size)
        assertEquals("John", result[0].name)
        assertEquals("Jane", result[1].name)
    }

    @Test
    fun `getUserById should fetch from remote when not in local`() = runTest {
        // Given
        val userId = "123"
        val remoteUser = UserDto(id = userId, name = "John", email = "john@example.com")

        coEvery { userDao.getUserById(userId) } returns null
        coEvery { userApi.getUserById(userId) } returns remoteUser
        coEvery { userDao.insertUser(any()) } just Runs

        // When
        val result = repository.getUserById(userId)

        // Then
        assertTrue(result is Result.Success)
        assertEquals("John", (result as Result.Success).data.name)
        coVerify { userDao.insertUser(any()) } // Verify caching
    }

    @Test
    fun `syncUsers should handle api errors gracefully`() = runTest {
        // Given
        coEvery { userApi.getAllUsers() } throws IOException("Network error")

        // When
        val result = repository.syncUsers()

        // Then
        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { userDao.deleteAll() } // Verify no data was deleted
    }
}
```

### Unit Testing Utilities and Parsers

```kotlin
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class DataParserTest {

    private val parser = DataParser()

    @Test
    fun `parseJsonData should parse valid json`() {
        // Given
        val json = """{"id": "123", "name": "Test"}"""

        // When
        val result = parser.parseJsonData(json)

        // Then
        assertNotNull(result)
        assertEquals("123", result.id)
        assertEquals("Test", result.name)
    }

    @Test
    fun `parseJsonData should return null for invalid json`() {
        // Given
        val invalidJson = "not valid json"

        // When
        val result = parser.parseJsonData(invalidJson)

        // Then
        assertNull(result)
    }

    @ParameterizedTest
    @CsvSource(
        "john@example.com, true",
        "invalid-email, false",
        "test@test, false",
        "user@domain.co.uk, true"
    )
    fun `isValidEmail should validate email formats`(email: String, expected: Boolean) {
        assertEquals(expected, email.isValidEmail())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "  ", "\n", "\t"])
    fun `parseData should handle empty or whitespace input`(input: String) {
        val result = parser.parseData(input)
        assertNull(result)
    }
}
```

### Integration Testing with Room

```kotlin
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var todoDao: TodoDao

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries() // For testing only
         .build()

        todoDao = database.todoDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTodo() = runTest {
        // Given
        val todo = TodoEntity(
            id = "1",
            title = "Test Todo",
            description = "Test Description",
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )

        // When
        todoDao.insert(todo)
        val todos = todoDao.getAll().first()

        // Then
        assertEquals(1, todos.size)
        assertEquals("Test Todo", todos[0].title)
        assertFalse(todos[0].isCompleted)
    }

    @Test
    fun updateTodo() = runTest {
        // Given
        val todo = TodoEntity(
            id = "1",
            title = "Original Title",
            description = "Description",
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        todoDao.insert(todo)

        // When
        val updatedTodo = todo.copy(title = "Updated Title", isCompleted = true)
        todoDao.update(updatedTodo)
        val result = todoDao.getById("1")

        // Then
        assertNotNull(result)
        assertEquals("Updated Title", result?.title)
        assertTrue(result?.isCompleted ?: false)
    }

    @Test
    fun deleteTodo() = runTest {
        // Given
        val todo = TodoEntity(
            id = "1",
            title = "Test Todo",
            description = "Description",
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        todoDao.insert(todo)

        // When
        todoDao.delete(todo)
        val todos = todoDao.getAll().first()

        // Then
        assertTrue(todos.isEmpty())
    }

    @Test
    fun queryCompletedTodos() = runTest {
        // Given
        todoDao.insert(TodoEntity("1", "Todo 1", "", true, 0))
        todoDao.insert(TodoEntity("2", "Todo 2", "", false, 0))
        todoDao.insert(TodoEntity("3", "Todo 3", "", true, 0))

        // When
        val completedTodos = todoDao.getCompleted().first()

        // Then
        assertEquals(2, completedTodos.size)
        assertTrue(completedTodos.all { it.isCompleted })
    }
}
```

### UI Testing with Espresso

```kotlin
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun displaysTodoList() {
        // Verify RecyclerView is displayed
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun addNewTodo() {
        // Click FAB to open add dialog
        onView(withId(R.id.fabAdd))
            .perform(click())

        // Enter todo details
        onView(withId(R.id.editTextTitle))
            .perform(typeText("New Todo"), closeSoftKeyboard())

        onView(withId(R.id.editTextDescription))
            .perform(typeText("Description"), closeSoftKeyboard())

        // Click save
        onView(withId(R.id.buttonSave))
            .perform(click())

        // Verify todo appears in list
        onView(withText("New Todo"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun toggleTodoCompletion() {
        // Click on checkbox
        onView(withId(R.id.checkboxComplete))
            .perform(click())

        // Verify checkbox is checked
        onView(withId(R.id.checkboxComplete))
            .check(matches(isChecked()))
    }

    @Test
    fun searchTodos() {
        // Click search icon
        onView(withId(R.id.action_search))
            .perform(click())

        // Enter search query
        onView(withId(R.id.search_src_text))
            .perform(typeText("test"), closeSoftKeyboard())

        // Verify filtered results
        onView(withText("Test Todo"))
            .check(matches(isDisplayed()))
    }
}
```

### Testing with Hilt

```kotlin
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject

@HiltAndroidTest
class RepositoryIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: TodoRepository

    @Inject
    lateinit var database: AppDatabase

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun repositoryUsesDatabase() = runTest {
        // Given
        val todo = Todo(
            id = "1",
            title = "Test",
            description = "",
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )

        // When
        repository.insertTodo(todo)
        val todos = repository.getAllTodos().first()

        // Then
        assertEquals(1, todos.size)
        assertEquals("Test", todos[0].title)
    }
}
```

---

## Gradle Configuration

### Project-Level `build.gradle.kts`

```kotlin
// Top-level build file
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
```

### App-Level `build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.[domain].[app]"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.[domain].[app]"
        minSdk = 21  // Adjust based on requirements
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Enable multidex if needed
        multiDexEnabled = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Signing config for release builds
            // signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        // Enable desugaring for newer Java APIs on older devices
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"

        // Enable explicit API mode for libraries
        // freeCompilerArgs += "-Xexplicit-api=strict"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        // dataBinding = true  // If needed
    }

    // JUnit 5 configuration
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true

            all {
                it.useJUnitPlatform()
            }
        }

        // Manage test execution
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    // Packaging options to avoid conflicts
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
        }
    }

    // Lint options
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        warningsAsErrors = false
    }
}

dependencies {
    // Core AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Unit Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("app.cash.turbine:turbine:1.0.0")

    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.48")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
```

### `gradle.properties`

```properties
# Gradle Settings
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true

# Kotlin
kotlin.code.style=official

# AndroidX
android.useAndroidX=true
android.enableJetifier=false

# Kapt (if using)
kapt.incremental.apt=true
kapt.use.worker.api=true

# Build Features
android.defaults.buildfeatures.buildconfig=true
android.nonTransitiveRClass=true
android.nonFinalResIds=true
```

---

## Build Best Practices

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run tests for specific variant
./gradlew testDebugUnitTest

# Run instrumentation tests
./gradlew connectedAndroidTest

# Run tests with coverage
./gradlew testDebugUnitTest jacocoTestReport

# Run specific test class
./gradlew test --tests "com.example.TodoListViewModelTest"

# Run tests in parallel
./gradlew test --parallel --max-workers=4
```

### Building the App

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build and install debug
./gradlew installDebug

# Clean and build
./gradlew clean build

# Build with specific variant
./gradlew assembleProdRelease
```

### Gradle Task Management

```bash
# List all tasks
./gradlew tasks

# Dependency tree
./gradlew app:dependencies

# Check for dependency updates
./gradlew dependencyUpdates

# Analyze build
./gradlew --scan build
```

### Optimization Tips

1. **Enable Build Cache**: Already enabled in `gradle.properties`
2. **Use Parallel Execution**: Add `--parallel` flag
3. **Increase Heap Size**: Configure in `gradle.properties`
4. **Use KSP Instead of Kapt**: Already configured for Hilt and Room
5. **Enable Configuration Cache**: `./gradlew --configuration-cache build`

### CI/CD Configuration Example

```yaml
# .github/workflows/android.yml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Run unit tests
      run: ./gradlew test

    - name: Build with Gradle
      run: ./gradlew build

    - name: Upload test reports
      if: failure()
      uses: actions/upload-artifact@v3
      with:
        name: test-reports
        path: app/build/reports/tests/
```

---

## Tips and Best Practices

### Testing Best Practices

1. **Follow AAA Pattern**: Arrange, Act, Assert
2. **Test One Thing**: Each test should verify one behavior
3. **Use Descriptive Names**: `should_doSomething_when_condition()`
4. **Mock External Dependencies**: Don't hit real databases or networks
5. **Test Edge Cases**: Empty lists, null values, errors
6. **Keep Tests Fast**: Unit tests should run in milliseconds
7. **Avoid Test Interdependence**: Tests should run in any order
8. **Use Test Fixtures**: Setup common test data in `@BeforeEach`

### Build Best Practices

1. **Version Catalogs**: Consider using version catalogs for dependency management
2. **Build Variants**: Use flavors for different environments (dev, staging, prod)
3. **ProGuard Rules**: Keep proper ProGuard rules for libraries
4. **APK Signing**: Configure proper signing for release builds
5. **Resource Shrinking**: Enable in release builds to reduce APK size

### Common Testing Pitfalls to Avoid

- Don't test Android framework classes
- Don't write integration tests when unit tests suffice
- Don't ignore flaky tests - fix them
- Don't skip tests before committing
- Don't mock everything - test real objects when possible

---

## Cross-References

- See [02-architecture.md](./02-architecture.md) for architecture patterns to test
- See [04-dependency-injection.md](./04-dependency-injection.md) for Hilt testing setup
- See [05-database-and-data.md](./05-database-and-data.md) for Room testing examples
- See [06-ui-and-patterns.md](./06-ui-and-patterns.md) for UI testing patterns
- See [01-project-planning.md](./01-project-planning.md) for iteration testing approach
