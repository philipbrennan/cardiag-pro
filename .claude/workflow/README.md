# Android Development Workflow

## Overview

This is a living template for building high-quality Android applications using modern architecture patterns, test-driven development, and iteration-based delivery.

## Directory Structure

```
.claude/
├── claude.md                           # Your project-specific context
├── implementation-plan.md              # Your project iterations
├── workflow/                           # Generic workflow guides
│   ├── README.md                       # This file
│   ├── 01-project-planning.md          # Planning & documentation
│   ├── 02-architecture.md              # MVVM, patterns, structure
│   ├── 03-testing-and-builds.md        # Testing strategy & Gradle
│   ├── 04-dependency-injection.md      # Hilt setup & patterns
│   ├── 05-database-and-data.md         # Room, Repository, error handling
│   ├── 06-ui-and-patterns.md           # UI components & patterns
│   ├── 07-optimization.md              # Performance & battery
│   └── 08-process-guidelines.md        # Workflow & quality checklist
└── templates/                          # Code templates
    ├── viewmodel-template.kt
    ├── repository-template.kt
    ├── dao-template.kt
    ├── entity-template.kt
    └── hilt-module-template.kt
```

## Quick Start

### For New Projects

1. **Copy this entire `.claude/` directory** into your new Android project root
2. **Customize `claude.md`** with your project specifics:
   - Project overview and goals
   - User requirements
   - Key design decisions
   - Current status
3. **Create `implementation-plan.md`** following the template in `01-project-planning.md`
4. **Reference workflow files** as needed during development
5. **Update `claude.md`** as your project evolves

### For Iteration-Based Development

**Before each iteration**:
1. Review iteration plan in `implementation-plan.md`
2. Ask clarifying questions
3. Create todo list with TodoWrite tool
4. Get user confirmation

**During each iteration**:
1. Reference relevant workflow file (e.g., `02-architecture.md` for structure)
2. Use code templates from `templates/` directory
3. Write production code + tests
4. Update todos as complete

**After each iteration**:
1. Run `./gradlew test` (verify tests pass)
2. Run `./gradlew build` (verify compilation)
3. Commit with descriptive message
4. Get user approval before next iteration

## Technology Stack (Standard)

- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **Build System**: Gradle with Kotlin DSL
- **DI**: Dagger Hilt
- **Async**: Kotlin Coroutines + Flow
- **Database**: Room
- **Testing**: JUnit 5, MockK, Espresso
- **Logging**: Timber

## Workflow Files Quick Reference

| File | When to Use |
|------|-------------|
| `01-project-planning.md` | Starting new project, planning iterations |
| `02-architecture.md` | Setting up project structure, adding features |
| `03-testing-and-builds.md` | Writing tests, configuring Gradle |
| `04-dependency-injection.md` | Setting up Hilt, adding modules |
| `05-database-and-data.md` | Adding Room database, repositories |
| `06-ui-and-patterns.md` | Building UI, RecyclerViews, ViewBinding |
| `07-optimization.md` | Performance tuning, battery optimization |
| `08-process-guidelines.md` | Code review, quality checks, communication |

## Core Principles

1. **Iteration-Based**: Break project into 8-10 meaningful iterations
2. **Test-Driven**: Write tests for every component (80%+ coverage goal)
3. **Quality Gates**: Tests pass + build succeeds before proceeding
4. **Clear Communication**: Ask questions, update todos, get approval
5. **Modern Architecture**: MVVM + Repository + Hilt + Coroutines

## Success Metrics

**Per Iteration**:
- ✅ All tests pass
- ✅ Build succeeds
- ✅ Code meets requirements
- ✅ User approves

**Overall Project**:
- ✅ Meets all functional requirements
- ✅ 80%+ test coverage
- ✅ No crashes or memory leaks
- ✅ Responsive UI (60fps)
- ✅ Battery-efficient

## Getting Help

- See individual workflow files for detailed guidance
- Use code templates in `templates/` directory
- Reference `claude.md` for project-specific context
- Check `implementation-plan.md` for current iteration details

## Customization

This workflow is a living template. Feel free to:
- Add project-specific workflow files
- Customize code templates
- Adjust iteration structure
- Add your own patterns and conventions

## Next Steps

1. Read `claude.md` to understand current project state
2. Review `implementation-plan.md` for iteration plan
3. Reference workflow files as needed during development
4. Keep documentation updated as project evolves
