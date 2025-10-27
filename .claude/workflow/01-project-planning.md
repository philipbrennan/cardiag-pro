# Project Planning & Documentation

## Overview

Proper planning and documentation are critical for successful Android development. This guide covers how to structure your project documentation and plan iterations.

---

## 1. Core Documentation Files

### 1.1 Project Context (`claude.md`)

This is your **living document** that evolves with your project.

**Required Sections**:

```markdown
# Claude Context - [Project Name]

## Project Overview
Brief description of what the app does and why it exists.

## User Requirements

### Primary Goals
1. Goal 1
2. Goal 2
3. Goal 3

### User Specifications
- Build System: Gradle with Kotlin DSL
- Min SDK: API [XX]
- Testing: JUnit 5 + Espresso
- Key technical requirements

### User Preferences
- Development workflow preferences
- Testing approach
- Communication style

## Current Status

### Iteration X: [Status]
**Status**: [In Progress / Complete / Blocked]

**What Was Built**:
- Component 1
- Component 2
- Tests written

**Deliverable**: [What iteration produces]

**Blockers**: [Any blockers if present]

### Next Up: Iteration Y
Brief preview of what's coming next.

**Awaiting**: What's needed to proceed.

## Key Design Decisions

### [Decision Name]
**Decision**: What was decided
**Rationale**: Why this decision was made
**Implementation**: How it's implemented

### [Another Decision]
...

## Important Context for Future Work

### [Topic 1]
Key information needed when working on this area.

### [Topic 2]
...

## Common Patterns Used

### [Pattern Name]
```kotlin
// Code example
```

## File Structure Reference

### Current Files (Iteration X)
```
project/
├── component1/
└── component2/
```

## Workflow Guidelines

### Before Starting Each Iteration
1. Review iteration plan
2. Ask clarifying questions
3. Create todo list
4. Get user confirmation

### During Each Iteration
1. Write production code
2. Write tests
3. Update todos
4. Log progress

### After Each Iteration
1. Run tests
2. Run build
3. Commit
4. Get approval

### Communication Style with User
- Be concise and direct
- Ask questions when unclear
- Don't make assumptions
- Confirm major decisions
- Provide progress updates

## Important Notes

### What NOT to Do
❌ Don't [anti-pattern 1]
❌ Don't [anti-pattern 2]

### What TO Do
✅ Do [best practice 1]
✅ Do [best practice 2]

## Testing Strategy

### Unit Tests (JUnit 5)
- What to test with unit tests

### Integration Tests
- What to test with integration tests

### UI Tests (Espresso)
- What to test with UI tests

## Known Issues & Limitations

### Current Limitations
1. Limitation 1
2. Limitation 2

### Platform Limitations
1. Platform limitation 1
2. Platform limitation 2

## Resources

### Official Documentation
- Link 1
- Link 2

### Protocol/Library References
- Link 1
- Link 2

## Success Metrics

### Per Iteration
- ✅ All tests pass
- ✅ Build succeeds
- ✅ Code meets requirements
- ✅ User approves

### Overall Project
- Success criterion 1
- Success criterion 2

## Current Blockers

List any blockers preventing progress.

## Next Actions

**Waiting for**:
- Action 1

**Then proceed with**:
- Next iteration
```

### 1.2 Implementation Plan (`implementation-plan.md`)

This defines your **complete roadmap**.

**Template Structure**:

```markdown
# [Project Name] - Complete Implementation Plan

## Overview
One-sentence project description.

## Technology Stack
- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: API [XX]
- **Target SDK**: API [YY]
- **DI**: Dagger Hilt
- **Async**: Kotlin Coroutines + Flow
- **Database**: Room
- **Testing**: JUnit 5, MockK, Espresso
- **Logging**: Timber
- **[Other]**: [Library]

---

## Iteration 1: Project Foundation ✅ or 🔄 or ⏸️

### Goals
- High-level goals for this iteration

### Tasks
- [ ] Task 1
- [ ] Task 2
- [ ] Write unit tests
- [ ] Verify build and tests pass

### Deliverables
- What this iteration produces

### Key Components
```
app/src/main/java/com/domain/app/
├── component1/
└── component2/
```

### Technical Considerations
- Important technical notes
- Decisions to make
- Patterns to follow

---

## Iteration 2: [Feature Name]

[Repeat structure for each iteration]

---

## Iteration 8: Polish & Optimization

### Goals
- Finalize app for production

### Tasks
- [ ] Battery optimization
- [ ] Performance tuning
- [ ] Dark mode
- [ ] App icon
- [ ] Final QA

---

## Project Structure (Final)

```
Complete final structure of the project
```

---

## Dependencies (Complete)

```kotlin
dependencies {
    // All dependencies needed
}
```

---

## Success Criteria

### Functional Requirements
✅ Requirement 1
✅ Requirement 2

### Non-Functional Requirements
✅ 80%+ test coverage
✅ No memory leaks
✅ Battery-efficient

### Performance Targets
- Metric 1: < X seconds
- Metric 2: < Y MB

---

## Risk Mitigation

### Technical Risks
1. **Risk**: Mitigation strategy

### Testing Risks
1. **Risk**: Mitigation strategy

---

## Timeline Estimate

| Iteration | Estimated Time | Cumulative |
|-----------|---------------|------------|
| 1. Foundation | 1 day | 1 day |
| 2. Feature A | 1-2 days | 2-3 days |
| ... | ... | ... |
| 8. Polish | 2-3 days | 10-16 days |

**Total: 10-16 days** (with testing and approval between iterations)

---

## Notes

- Each iteration must pass all tests before proceeding
- User approval required before moving to next iteration
- Focus on defensive programming and error handling
```

---

## 2. Planning Iterations

### 2.1 Standard 8-Iteration Structure

Most Android projects follow this pattern:

**Iteration 1: Foundation**
- Project structure and build setup
- Core architecture (MVVM, Hilt)
- Basic feature detection/initialization
- UI skeleton
- Permission handling
- Initial tests
- Git initialization

**Iteration 2: Core Service/Manager**
- Primary business logic service
- Lifecycle management
- Background service (if needed)
- Basic listeners/callbacks

**Iteration 3: Data Layer**
- Parser/data processing
- Data models
- Room database schema
- Repository implementation
- DAO definitions

**Iteration 4: UI Implementation**
- RecyclerView adapter
- ViewModels with Flow
- Pull-to-refresh
- Real-time updates
- Status indicators

**Iteration 5: Enhanced Features**
- Additional discovery/processing
- Manual input capabilities
- Monitoring/analytics
- Advanced functionality

**Iteration 6: Media/Content Handling**
- Player/viewer integration
- Format handling
- Error management
- Content controls

**Iteration 7: Persistence & Settings**
- Favorites functionality
- Settings screen
- Search/filter
- Preferences

**Iteration 8: Polish & Optimization**
- Battery optimization
- Notifications
- Export/import
- Dark mode
- App icon
- Final QA

### 2.2 Iteration Planning Checklist

For each iteration, define:

- [ ] **Clear goals** (2-3 high-level objectives)
- [ ] **Specific tasks** (6-10 actionable items)
- [ ] **Deliverables** (what the user can see/test)
- [ ] **Key components** (classes/files to create)
- [ ] **Technical considerations** (decisions, patterns, gotchas)
- [ ] **Testing requirements** (unit, integration, UI tests)
- [ ] **Estimated time** (realistic time estimate)

### 2.3 Iteration Dependencies

Map out dependencies between iterations:

```
Iteration 1 (Foundation)
    ↓
Iteration 2 (Core Service) ← depends on architecture
    ↓
Iteration 3 (Data Layer) ← depends on core service
    ↓
Iteration 4 (UI) ← depends on data layer
    ↓
Iterations 5-7 (Features) ← can be reordered
    ↓
Iteration 8 (Polish) ← depends on all features
```

---

## 3. Technology Stack Definition

### 3.1 Standard Stack Template

```markdown
## Technology Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: API 21 (Android 5.0) or higher
- **Target SDK**: API 34 (Android 14)
- **Compile SDK**: API 34
- **DI**: Dagger Hilt
- **Async**: Kotlin Coroutines + Flow
- **Database**: Room (if data persistence needed)
- **HTTP**: Retrofit + OkHttp (if networking needed)
- **Image Loading**: Coil (if image loading needed)
- **Media**: ExoPlayer (if media playback needed)
- **Background Work**: WorkManager (if periodic tasks needed)
- **Navigation**: Navigation Component (if multi-screen)
- **Testing**: JUnit 5, MockK, Espresso
- **Logging**: Timber
```

### 3.2 Choosing Min SDK

| Min SDK | Coverage | Considerations |
|---------|----------|---------------|
| API 21 (5.0) | ~99% | Good choice for most apps |
| API 23 (6.0) | ~98% | Runtime permissions standard |
| API 24 (7.0) | ~95% | Multi-window, better notifications |
| API 26 (8.0) | ~90% | Notification channels required |

**Recommendation**: API 21 for maximum compatibility, API 23 if runtime permissions are critical.

---

## 4. Requirements Gathering

### 4.1 Questions to Ask

**Functional Requirements**:
- What is the core purpose of the app?
- Who is the target user?
- What are the must-have features?
- What are the nice-to-have features?
- Are there any offline requirements?
- What data needs to be persisted?

**Technical Requirements**:
- What Android versions should be supported?
- Are there any specific libraries to use or avoid?
- What testing framework is preferred?
- Are there performance requirements?
- What about accessibility requirements?

**Workflow Preferences**:
- How should iterations be structured?
- How often should approval be requested?
- What level of detail is expected in updates?
- How should questions be handled?

### 4.2 Documenting User Preferences

Always document:
- Phased vs. continuous delivery
- Approval gates (per iteration, per feature, etc.)
- Testing approach (TDD, test-after, etc.)
- Communication style (verbose, concise, etc.)
- Question handling (ask immediately, batch questions, etc.)

---

## 5. Project Initialization Checklist

### 5.1 Before Writing Code

- [ ] Create `.claude/` directory
- [ ] Write initial `claude.md`
- [ ] Create `implementation-plan.md`
- [ ] Define technology stack
- [ ] Plan all 8 iterations
- [ ] Get user approval on plan

### 5.2 After Project Setup

- [ ] Initialize git repository
- [ ] Create `.gitignore`
- [ ] Add README.md
- [ ] Create initial commit
- [ ] Update `claude.md` with "Iteration 1: Complete"

---

## 6. Documentation Maintenance

### 6.1 Update Frequency

**After each iteration**:
- Update current status in `claude.md`
- Mark iteration as complete in `implementation-plan.md`
- Document any design decisions made
- Add new patterns discovered
- Update file structure

**When blockers occur**:
- Document in "Current Blockers" section
- Explain what's needed to proceed

**When requirements change**:
- Update implementation plan
- Document why change was made
- Adjust future iterations

### 6.2 What to Track

Track in `claude.md`:
- ✅ **Current iteration status**
- ✅ **Key design decisions** (with rationale)
- ✅ **Patterns established** (with code examples)
- ✅ **Blockers and limitations**
- ✅ **Important context for future work**

Don't track in `claude.md`:
- ❌ Detailed code implementations (that's in the code)
- ❌ Every single file (that's in git)
- ❌ Step-by-step instructions (that's in workflow files)

---

## 7. Example: Starting a New Project

### Step 1: Create Documentation

```bash
mkdir -p my-app/.claude/workflow
mkdir -p my-app/.claude/templates
cp -r android-workflow-template/.claude/* my-app/.claude/
```

### Step 2: Customize `claude.md`

```markdown
# Claude Context - My Awesome App

## Project Overview
An Android app that helps users track their daily water intake.

## User Requirements

### Primary Goals
1. Track water consumption throughout the day
2. Send reminders to drink water
3. View historical data and trends

### User Specifications
- Build System: Gradle with Kotlin DSL
- Min SDK: API 23 (Android 6.0)
- Testing: JUnit 5 + Espresso
- Must work offline

...
```

### Step 3: Create Implementation Plan

Break down into 8 iterations based on the template.

### Step 4: Get Approval

Share plan with user and get approval to proceed.

### Step 5: Start Iteration 1

Begin development following the workflow.

---

## Summary

Proper planning involves:
1. Creating comprehensive documentation (`claude.md` + `implementation-plan.md`)
2. Defining clear iterations (8 standard iterations)
3. Documenting technology stack and requirements
4. Getting user approval before coding
5. Maintaining documentation throughout development

**Remember**: Time spent planning saves time debugging later!
