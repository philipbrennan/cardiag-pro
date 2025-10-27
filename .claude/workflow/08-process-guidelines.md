# Communication & Process Guidelines

This workflow file covers communication protocols, workflow best practices, do's and don'ts, and quality checklists for Android development.

## Table of Contents
- [Communication Guidelines](#communication-guidelines)
- [Development Workflow](#development-workflow)
- [Do's and Don'ts](#dos-and-donts)
- [Quality Checklist](#quality-checklist)
- [Code Review Guidelines](#code-review-guidelines)
- [Git Workflow](#git-workflow)

---

## Communication Guidelines

### Communication Style with Users

#### Be Clear and Concise

**DO:**
- Use clear, direct language
- State what you're about to do
- Explain technical decisions briefly
- Ask questions when requirements are unclear

**DON'T:**
- Use overly technical jargon unnecessarily
- Make assumptions about unclear requirements
- Proceed without user confirmation on major decisions
- Over-explain obvious concepts

#### Example Communication

```
Good:
"I'll implement the Todo feature using MVVM architecture with Room
for local storage. This will allow offline access and automatic UI
updates. Should I proceed?"

Not Good:
"I'm going to leverage a reactive paradigm utilizing the observer
pattern with repository abstractions and dependency injection
containers orchestrating..."
```

### When to Ask Questions

Ask clarifying questions when:

1. **Requirements are ambiguous**
   - "Should completed todos be deletable or archived?"
   - "What's the expected behavior when network is unavailable?"

2. **Technical decisions affect user experience**
   - "Should we cache images or fetch them each time?"
   - "Do you want push notifications for reminders?"

3. **Multiple valid approaches exist**
   - "Should we use Navigation Component or manual fragments?"
   - "Prefer Material 2 or Material 3 design?"

4. **Edge cases aren't specified**
   - "What happens if the user denies location permission?"
   - "Should we sync data when on cellular data?"

### Progress Updates

Provide regular progress updates:

```
"Starting Iteration 3: Data Layer
- Created Todo entity and DAO
- Implemented Room database
- Added repository with Flow support
- Writing unit tests (5/8 complete)
Currently: Testing database operations"
```

---

## Development Workflow

### Before Starting Each Iteration

1. **Review the Iteration Plan**
   - Understand goals and deliverables
   - Identify dependencies
   - Estimate complexity

2. **Ask Clarifying Questions**
   - Resolve any ambiguities
   - Confirm technical approach
   - Verify user expectations

3. **Create Task List**
   - Use TodoWrite tool for complex iterations
   - Break down into manageable tasks
   - Mark dependencies

4. **Get User Confirmation**
   - Summarize the plan
   - Wait for explicit approval
   - Proceed only after confirmation

### During Each Iteration

1. **Write Production Code**
   - Follow established patterns
   - Use consistent naming
   - Add appropriate comments
   - Handle errors gracefully

2. **Write Corresponding Tests**
   - Unit tests for business logic
   - Integration tests for components
   - UI tests for user flows
   - Maintain >80% coverage

3. **Update Task List**
   - Mark tasks complete as you finish
   - Update status in real-time
   - Add new tasks if discovered

4. **Log Progress Clearly**
   - Use Timber for important operations
   - Add meaningful log messages
   - Track errors and warnings

5. **Follow Established Patterns**
   - Maintain consistency
   - Reuse existing patterns
   - Don't reinvent the wheel

### After Each Iteration

1. **Run Tests**
   ```bash
   ./gradlew test
   ```
   - Verify all tests pass
   - Check test coverage
   - Fix any failures

2. **Build Project**
   ```bash
   ./gradlew build
   ```
   - Ensure clean compilation
   - Resolve any warnings
   - Check APK size

3. **Commit Changes**
   - Write descriptive commit message
   - Include iteration summary
   - Reference test counts
   - Mention key decisions

4. **Request User Approval**
   - Summarize what was completed
   - Highlight any changes to plan
   - Wait for approval before next iteration

### Example Iteration Workflow

```
User: "Let's implement the data layer"

Developer: "I'll implement Iteration 3: Data Layer with:
- TodoEntity with Room annotations
- TodoDao with CRUD operations using Flow
- AppDatabase configuration
- DatabaseModule for Hilt
- TodoRepository with local data source
- 8 unit tests for DAO and repository

This will provide offline storage with reactive updates.
Should I proceed?"

User: "Yes, go ahead"

Developer: [Creates TodoWrite task list, implements features]

Developer: "Iteration 3 complete:
✓ TodoEntity created with proper annotations
✓ TodoDao with Flow-based queries
✓ AppDatabase configured with type converters
✓ DatabaseModule integrated with Hilt
✓ TodoRepository with full CRUD operations
✓ 8 unit tests written (100% coverage for data layer)
✓ All tests passing
✓ Build successful

Ready to proceed to Iteration 4?"
```

---

## Do's and Don'ts

### What NOT to Do

❌ **Don't proceed to next iteration without approval**
```
Bad: Automatically starting new work after completing iteration
Good: "Iteration 3 complete. Ready for Iteration 4?"
```

❌ **Don't skip tests to save time**
```
Bad: "I'll add tests later"
Good: Write tests alongside production code
```

❌ **Don't make assumptions about unclear requirements**
```
Bad: Guessing what user wants
Good: "Should we support offline mode?"
```

❌ **Don't hardcode values that should be configurable**
```kotlin
// Bad
val apiUrl = "https://api.example.com"

// Good
val apiUrl = BuildConfig.API_URL
```

❌ **Don't create files unnecessarily**
```
Bad: Creating multiple README files or docs without request
Good: Only create files explicitly requested or required
```

❌ **Don't use magic numbers**
```kotlin
// Bad
if (count > 100) { }

// Good
companion object {
    private const val MAX_ITEMS = 100
}
if (count > MAX_ITEMS) { }
```

❌ **Don't ignore exceptions**
```kotlin
// Bad
try {
    riskyOperation()
} catch (e: Exception) {
    // Silent failure
}

// Good
try {
    riskyOperation()
} catch (e: Exception) {
    Timber.e(e, "Operation failed")
    showErrorToUser()
}
```

### What TO Do

✅ **Always write tests for new components**
```kotlin
// For every ViewModel, Repository, DAO, Manager:
@Test
fun `feature should work correctly`() {
    // Arrange, Act, Assert
}
```

✅ **Handle errors gracefully with logging**
```kotlin
suspend fun loadData(): Result<Data> {
    return try {
        val data = api.fetchData()
        Result.Success(data)
    } catch (e: Exception) {
        Timber.e(e, "Failed to load data")
        Result.Error(e)
    }
}
```

✅ **Use Kotlin idioms and best practices**
```kotlin
// Use data classes
data class User(val id: String, val name: String)

// Use sealed classes for state
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: Data) : UiState()
    data class Error(val message: String) : UiState()
}

// Use extension functions
fun String.isValidEmail(): Boolean =
    Patterns.EMAIL_ADDRESS.matcher(this).matches()
```

✅ **Keep code readable and maintainable**
```kotlin
// Use meaningful names
fun calculateTotalPrice(items: List<Item>): Double {
    return items.sumOf { it.price }
}

// Add comments for complex logic
/**
 * Calculates exponential backoff delay for retry attempts.
 * Uses formula: initialDelay * (factor ^ attempt)
 */
private fun calculateBackoff(attempt: Int): Long {
    return (initialDelay * factor.pow(attempt)).toLong()
}
```

✅ **Update documentation as you go**
```kotlin
/**
 * Repository for managing todo items.
 * Provides offline-first data access with automatic sync.
 *
 * @property dao Local database access
 * @property apiService Remote API access
 */
class TodoRepository @Inject constructor(
    private val dao: TodoDao,
    private val apiService: ApiService
)
```

✅ **Ask clarifying questions**
```
"I notice the design shows a search feature. Should it:
- Search locally in cached data?
- Make API calls for each query?
- Support filters by category/date?"
```

✅ **Use TodoWrite for task tracking**
```
Complex iterations with 5+ tasks should use TodoWrite
to track progress and ensure nothing is missed.
```

✅ **Commit after each iteration**
```bash
git add .
git commit -m "Iteration 3: Data Layer implementation

- Add TodoEntity with Room annotations
- Implement TodoDao with Flow queries
- Configure AppDatabase with migrations
- Create DatabaseModule for Hilt injection
- Implement TodoRepository with offline support
- Add 8 unit tests (100% data layer coverage)

All tests passing, build successful."
```

---

## Quality Checklist

### Before Committing

Run through this checklist before every commit:

- [ ] **All tests pass**
  ```bash
  ./gradlew test
  ```

- [ ] **Build succeeds**
  ```bash
  ./gradlew build
  ```

- [ ] **No compiler warnings**
  - Fix all warnings before committing
  - Use `@Suppress` only when necessary

- [ ] **Code follows established patterns**
  - MVVM architecture
  - Repository pattern
  - Proper dependency injection
  - Consistent naming conventions

- [ ] **Error handling is in place**
  - Try-catch blocks where needed
  - Proper error types returned
  - User-friendly error messages

- [ ] **Timber logging added for important operations**
  ```kotlin
  Timber.d("Loading data")
  Timber.e(exception, "Failed to save")
  ```

- [ ] **Documentation updated**
  - KDoc for public APIs
  - Comments for complex logic
  - README if needed

- [ ] **Todo list updated**
  - Mark completed tasks
  - Add discovered tasks

### Before Next Iteration

Before moving to the next iteration:

- [ ] **Current iteration complete**
  - All planned features implemented
  - No half-finished work

- [ ] **All tasks checked off**
  - TodoWrite list complete
  - No pending items

- [ ] **Tests written and passing**
  - Unit tests for new code
  - Integration tests if needed
  - UI tests for new screens

- [ ] **User approval received**
  - Explicitly asked for approval
  - User confirmed completion
  - Any feedback addressed

- [ ] **Any questions resolved**
  - No ambiguities remaining
  - Technical approach confirmed

### Before Release

Final checklist before releasing to users:

- [ ] **80%+ test coverage**
  ```bash
  ./gradlew testDebugUnitTest jacocoTestReport
  ```

- [ ] **No memory leaks**
  - Run LeakCanary in debug
  - Profile with Android Profiler
  - Check ViewBinding cleanup

- [ ] **Performance targets met**
  - App starts in < 2 seconds
  - Smooth scrolling (60fps)
  - No ANR errors

- [ ] **All features working**
  - Manual testing complete
  - Edge cases handled
  - Error scenarios tested

- [ ] **Error handling comprehensive**
  - Network failures handled
  - Permission denials handled
  - Invalid input handled

- [ ] **UI polished**
  - No placeholder text
  - Proper loading states
  - Empty states implemented
  - Error states implemented

- [ ] **Dark mode supported** (if applicable)
  - Colors adapt to theme
  - Icons visible in both themes
  - Text readable in both themes

- [ ] **Battery usage optimized**
  - WorkManager for background tasks
  - No wake locks held unnecessarily
  - Location updates optimized

- [ ] **ProGuard rules configured**
  - Keep rules for libraries
  - Tested with minification enabled
  - No crashes in release build

---

## Code Review Guidelines

### Self-Review Checklist

Before submitting code for review:

1. **Functionality**
   - Does it work as intended?
   - Are edge cases handled?
   - Are there any bugs?

2. **Code Quality**
   - Is it readable?
   - Are names meaningful?
   - Is it well-structured?

3. **Performance**
   - Any performance issues?
   - Efficient algorithms used?
   - No unnecessary operations?

4. **Testing**
   - Are tests comprehensive?
   - Do tests actually test behavior?
   - Are edge cases covered?

5. **Documentation**
   - Is complex logic explained?
   - Are public APIs documented?
   - Are TODOs addressed?

### Reviewing Other's Code

When reviewing code:

1. **Be Constructive**
   - Suggest improvements, don't criticize
   - Explain the "why" behind suggestions
   - Praise good practices

2. **Focus on**
   - Correctness
   - Readability
   - Maintainability
   - Performance
   - Security

3. **Ask Questions**
   - "Why did you choose this approach?"
   - "Have you considered X?"
   - "How does this handle Y scenario?"

---

## Git Workflow

### Commit Message Format

```
[Iteration X] Brief summary (50 chars or less)

Detailed description of changes:
- Bullet point 1
- Bullet point 2
- Key decisions made
- Number of tests added

Technical notes (if any):
- Migration considerations
- Breaking changes
- Configuration requirements
```

### Example Commit Messages

**Good:**
```
Iteration 3: Data Layer with Room database

- Add TodoEntity with proper Room annotations
- Implement TodoDao with Flow-based reactive queries
- Configure AppDatabase with type converters
- Create DatabaseModule for Hilt integration
- Implement TodoRepository with offline-first approach
- Add 8 unit tests covering all DAO operations
- Test coverage: 100% for data layer

Technical notes:
- Using Flow instead of LiveData for reactive updates
- Configured for destructive migration (dev only)
- Added Converters for Date and List types
```

**Bad:**
```
Updated files
```

### Git Best Practices

1. **Commit Frequency**
   - Commit after each complete iteration
   - Don't commit incomplete work
   - Don't commit broken code

2. **Commit Size**
   - One iteration per commit
   - Don't mix unrelated changes
   - Keep commits focused

3. **Branch Strategy**
   - `main`: Production-ready code
   - `develop`: Integration branch
   - `feature/iteration-X`: Feature branches

4. **Never Do This**
   - Don't commit secrets or API keys
   - Don't commit generated files
   - Don't force push to shared branches
   - Don't commit without testing

---

## Success Metrics

### Per Iteration

Every iteration should meet these criteria:

- ✅ All planned features implemented
- ✅ All tests pass
- ✅ Build succeeds
- ✅ Code meets quality standards
- ✅ User approves progress

### Overall Project

The complete project should achieve:

- **Functionality**: Meets all requirements
- **Quality**: 80%+ test coverage
- **Stability**: No crashes or memory leaks
- **Performance**: Responsive UI (60fps)
- **Compatibility**: Works on target SDK range
- **Efficiency**: Battery-efficient
- **Accessibility**: TalkBack compatible (if applicable)

---

## Tips and Best Practices

### Communication Tips

1. **Be Proactive**: Share progress and blockers early
2. **Be Specific**: Use concrete examples
3. **Be Patient**: Wait for user responses
4. **Be Professional**: Maintain respectful tone
5. **Be Transparent**: Admit when you need clarification

### Development Tips

1. **Start Simple**: Get basic version working first
2. **Test Early**: Write tests as you go
3. **Refactor Often**: Improve code continuously
4. **Document Why**: Explain decisions
5. **Think Ahead**: Consider future maintenance

### Process Tips

1. **Follow Patterns**: Use established patterns
2. **Stay Organized**: Keep clear task lists
3. **Maintain Quality**: Don't compromise on tests
4. **Seek Feedback**: Ask for reviews
5. **Learn Continuously**: Improve with each iteration

---

## Common Scenarios

### Scenario 1: Requirements Change Mid-Iteration

```
Developer: "I'm halfway through Iteration 4, but the new
requirement would require changes to the data layer from Iteration 3.
Should I:
A) Complete Iteration 4 as planned, then add new iteration
B) Pause and refactor Iteration 3 first
C) Modify current approach to accommodate both?"

User: [Provides direction]
```

### Scenario 2: Unexpected Technical Blocker

```
Developer: "I've encountered an issue with the library we're using.
The feature we need isn't supported in the current version.
Options:
1. Use workaround X (less elegant but works)
2. Switch to library Y (more work upfront)
3. Wait for library update (timeline uncertain)

What would you prefer?"
```

### Scenario 3: Scope Creep

```
User: "Can we also add feature X?"

Developer: "Feature X would be significant additional work.
I recommend:
- Complete current iteration as planned
- Add Feature X as a new iteration with proper planning
- Estimate: 2-3 additional iterations

This ensures quality and proper testing. Thoughts?"
```

---

## Cross-References

- See [01-project-planning.md](./01-project-planning.md) for iteration planning
- See [02-architecture.md](./02-architecture.md) for code organization
- See [03-testing-and-builds.md](./03-testing-and-builds.md) for testing workflow
- See [04-dependency-injection.md](./04-dependency-injection.md) for DI patterns
- See [05-database-and-data.md](./05-database-and-data.md) for data layer
- See [06-ui-and-patterns.md](./06-ui-and-patterns.md) for UI implementation
- See [07-optimization.md](./07-optimization.md) for performance optimization

---

## Conclusion

Following these communication and process guidelines ensures:

- **Clear Communication**: Users know what to expect
- **Quality Code**: High standards maintained
- **Predictable Progress**: Iterations complete on schedule
- **User Satisfaction**: Requirements met, feedback incorporated
- **Maintainable Codebase**: Easy to understand and extend

Remember: The goal is to deliver high-quality Android applications through systematic, well-communicated development processes.
