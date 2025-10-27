# UI Patterns and Permissions Handling

This workflow file covers UI implementation patterns, RecyclerView adapters, ViewBinding, Flow collection, and permissions handling.

## Table of Contents
- [RecyclerView Patterns](#recyclerview-patterns)
- [ViewBinding Setup](#viewbinding-setup)
- [Flow Collection in UI](#flow-collection-in-ui)
- [Dialog Patterns](#dialog-patterns)
- [Permissions Handling](#permissions-handling)
- [Navigation Patterns](#navigation-patterns)

---

## RecyclerView Patterns

### Basic RecyclerView Adapter with ListAdapter

```kotlin
package com.example.myapp.ui.todo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.data.model.Todo
import com.example.myapp.databinding.ItemTodoBinding

class TodoAdapter(
    private val onItemClick: (Todo) -> Unit,
    private val onCheckboxClick: (Todo) -> Unit,
    private val onDeleteClick: (Todo) -> Unit
) : ListAdapter<Todo, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TodoViewHolder(binding, onItemClick, onCheckboxClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TodoViewHolder(
        private val binding: ItemTodoBinding,
        private val onItemClick: (Todo) -> Unit,
        private val onCheckboxClick: (Todo) -> Unit,
        private val onDeleteClick: (Todo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(todo: Todo) {
            binding.apply {
                textViewTitle.text = todo.title
                textViewDescription.text = todo.description
                checkboxComplete.isChecked = todo.isCompleted

                // Strike through completed todos
                textViewTitle.paintFlags = if (todo.isCompleted) {
                    textViewTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    textViewTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

                // Click listeners
                root.setOnClickListener { onItemClick(todo) }
                checkboxComplete.setOnClickListener { onCheckboxClick(todo) }
                buttonDelete.setOnClickListener { onDeleteClick(todo) }
            }
        }
    }

    class TodoDiffCallback : DiffUtil.ItemCallback<Todo>() {
        override fun areItemsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem == newItem
        }
    }
}
```

### Advanced RecyclerView with Multiple View Types

```kotlin
package com.example.myapp.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.databinding.ItemArticleBinding
import com.example.myapp.databinding.ItemHeaderBinding
import com.example.myapp.databinding.ItemAdBinding

sealed class FeedItem {
    data class Header(val title: String) : FeedItem()
    data class Article(val article: ArticleData) : FeedItem()
    data class Ad(val adData: AdData) : FeedItem()
}

class FeedAdapter(
    private val onArticleClick: (ArticleData) -> Unit
) : ListAdapter<FeedItem, RecyclerView.ViewHolder>(FeedDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ARTICLE = 1
        private const val TYPE_AD = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FeedItem.Header -> TYPE_HEADER
            is FeedItem.Article -> TYPE_ARTICLE
            is FeedItem.Ad -> TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            TYPE_ARTICLE -> {
                val binding = ItemArticleBinding.inflate(inflater, parent, false)
                ArticleViewHolder(binding, onArticleClick)
            }
            TYPE_AD -> {
                val binding = ItemAdBinding.inflate(inflater, parent, false)
                AdViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is FeedItem.Header -> (holder as HeaderViewHolder).bind(item)
            is FeedItem.Article -> (holder as ArticleViewHolder).bind(item)
            is FeedItem.Ad -> (holder as AdViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(
        private val binding: ItemHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: FeedItem.Header) {
            binding.textViewHeader.text = header.title
        }
    }

    class ArticleViewHolder(
        private val binding: ItemArticleBinding,
        private val onArticleClick: (ArticleData) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(article: FeedItem.Article) {
            binding.apply {
                textViewTitle.text = article.article.title
                textViewAuthor.text = article.article.author
                textViewDate.text = article.article.formattedDate

                root.setOnClickListener { onArticleClick(article.article) }
            }
        }
    }

    class AdViewHolder(
        private val binding: ItemAdBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ad: FeedItem.Ad) {
            binding.textViewAdContent.text = ad.adData.content
        }
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
        override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            return when {
                oldItem is FeedItem.Header && newItem is FeedItem.Header ->
                    oldItem.title == newItem.title
                oldItem is FeedItem.Article && newItem is FeedItem.Article ->
                    oldItem.article.id == newItem.article.id
                oldItem is FeedItem.Ad && newItem is FeedItem.Ad ->
                    oldItem.adData.id == newItem.adData.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            return oldItem == newItem
        }
    }
}
```

### RecyclerView with Swipe Actions

```kotlin
package com.example.myapp.ui.todo

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class SwipeToDeleteCallback(
    private val onDelete: (Int) -> Unit,
    private val onUndo: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        onDelete(position)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        // Custom swipe animation here
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}

// Usage in Activity/Fragment
private fun setupRecyclerView() {
    val swipeCallback = SwipeToDeleteCallback(
        onDelete = { position ->
            viewModel.deleteTodoAtPosition(position)
            showUndoSnackbar(position)
        },
        onUndo = { position ->
            viewModel.undoDelete(position)
        }
    )

    val itemTouchHelper = ItemTouchHelper(swipeCallback)
    itemTouchHelper.attachToRecyclerView(binding.recyclerView)
}

private fun showUndoSnackbar(position: Int) {
    Snackbar.make(binding.root, "Todo deleted", Snackbar.LENGTH_LONG)
        .setAction("UNDO") {
            viewModel.undoDelete(position)
        }
        .show()
}
```

---

## ViewBinding Setup

### Activity with ViewBinding

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

    private val todoAdapter by lazy {
        TodoAdapter(
            onItemClick = { todo -> viewModel.onTodoClick(todo) },
            onCheckboxClick = { todo -> viewModel.toggleTodoComplete(todo.id) },
            onDeleteClick = { todo -> viewModel.deleteTodo(todo.id) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()

        Timber.d("MainActivity created")
    }

    private fun setupUI() {
        binding.apply {
            // Setup toolbar
            setSupportActionBar(toolbar)

            // Setup RecyclerView
            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = todoAdapter
                setHasFixedSize(true)
            }

            // Setup FAB
            fabAdd.setOnClickListener {
                showAddTodoDialog()
            }

            // Setup SwipeRefresh
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.refreshTodos()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }

        lifecycleScope.launch {
            viewModel.isRefreshing.collect { isRefreshing ->
                binding.swipeRefreshLayout.isRefreshing = isRefreshing
            }
        }
    }

    private fun updateUI(state: UiState<List<Todo>>) {
        when (state) {
            is UiState.Loading -> showLoading()
            is UiState.Success -> showTodos(state.data)
            is UiState.Error -> showError(state.message)
            is UiState.Empty -> showEmpty()
        }
    }

    private fun showLoading() {
        binding.apply {
            progressBar.show()
            recyclerView.hide()
            textViewEmpty.hide()
        }
    }

    private fun showTodos(todos: List<Todo>) {
        binding.apply {
            progressBar.hide()
            recyclerView.show()
            textViewEmpty.hide()
        }
        todoAdapter.submitList(todos)
    }

    private fun showError(message: String) {
        binding.apply {
            progressBar.hide()
            recyclerView.hide()
            textViewEmpty.apply {
                show()
                text = message
            }
        }
    }

    private fun showEmpty() {
        binding.apply {
            progressBar.hide()
            recyclerView.hide()
            textViewEmpty.apply {
                show()
                text = "No todos yet"
            }
        }
    }
}

// Extension functions for visibility
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}
```

### Fragment with ViewBinding

```kotlin
package com.example.myapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.myapp.databinding.FragmentTodoDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TodoDetailFragment : Fragment() {

    private var _binding: FragmentTodoDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TodoDetailViewModel by viewModels()
    private val args: TodoDetailFragmentArgs by navArgs()

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

        viewModel.loadTodo(args.todoId)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.apply {
            buttonSave.setOnClickListener {
                saveTodo()
            }

            buttonDelete.setOnClickListener {
                deleteTodo()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todo.collect { todo ->
                todo?.let { displayTodo(it) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun displayTodo(todo: Todo) {
        binding.apply {
            editTextTitle.setText(todo.title)
            editTextDescription.setText(todo.description)
            checkboxComplete.isChecked = todo.isCompleted
        }
    }

    private fun saveTodo() {
        val title = binding.editTextTitle.text.toString()
        val description = binding.editTextDescription.text.toString()
        val isCompleted = binding.checkboxComplete.isChecked

        viewModel.saveTodo(title, description, isCompleted)
    }

    private fun deleteTodo() {
        viewModel.deleteTodo()
    }

    private fun handleEvent(event: TodoDetailEvent) {
        when (event) {
            is TodoDetailEvent.SaveSuccess -> {
                findNavController().navigateUp()
            }
            is TodoDetailEvent.DeleteSuccess -> {
                findNavController().navigateUp()
            }
            is TodoDetailEvent.Error -> {
                showError(event.message)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

---

## Flow Collection in UI

### Collecting Flows in Activities

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
    }

    private fun observeViewModel() {
        // Method 1: Using repeatOnLifecycle (Recommended)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // This block runs when STARTED and stops when STOPPED
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }

        // Method 2: Collecting multiple flows
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.todos.collect { todos ->
                        todoAdapter.submitList(todos)
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading
                    }
                }

                launch {
                    viewModel.error.collect { error ->
                        error?.let { showError(it) }
                    }
                }
            }
        }

        // Method 3: One-time events with SharedFlow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    handleNavigationEvent(event)
                }
            }
        }
    }
}
```

### Collecting Flows in Fragments

```kotlin
@AndroidEntryPoint
class TodoListFragment : Fragment() {

    private var _binding: FragmentTodoListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TodoListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        // Use viewLifecycleOwner for fragments
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }

        // Collect multiple flows in parallel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.todos.collect { todos ->
                        updateTodos(todos)
                    }
                }

                launch {
                    viewModel.searchQuery.collect { query ->
                        updateSearchQuery(query)
                    }
                }
            }
        }
    }
}
```

### Flow Collection Extensions

```kotlin
package com.example.myapp.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// Extension for collecting flows in fragments
fun <T> Fragment.collectFlow(
    flow: Flow<T>,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(state) {
            flow.collect(action)
        }
    }
}

// Usage
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectFlow(viewModel.uiState) { state ->
            updateUI(state)
        }

        collectFlow(viewModel.todos) { todos ->
            todoAdapter.submitList(todos)
        }
    }
}
```

---

## Dialog Patterns

### Material Dialog with ViewBinding

```kotlin
package com.example.myapp.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.myapp.databinding.DialogAddTodoBinding

class AddTodoDialog(
    private val onSave: (title: String, description: String) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddTodoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddTodoBinding.inflate(layoutInflater)

        return AlertDialog.Builder(requireContext())
            .setTitle("Add Todo")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                saveTodo()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun saveTodo() {
        val title = binding.editTextTitle.text.toString()
        val description = binding.editTextDescription.text.toString()

        if (title.isNotBlank()) {
            onSave(title, description)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddTodoDialog"
    }
}

// Usage in Activity/Fragment
private fun showAddTodoDialog() {
    val dialog = AddTodoDialog { title, description ->
        viewModel.addTodo(title, description)
    }
    dialog.show(supportFragmentManager, AddTodoDialog.TAG)
}
```

### Bottom Sheet Dialog

```kotlin
package com.example.myapp.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myapp.databinding.BottomSheetFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheet(
    private val onFilterApplied: (FilterOptions) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonApply.setOnClickListener {
            applyFilters()
        }
    }

    private fun applyFilters() {
        val showCompleted = binding.checkboxCompleted.isChecked
        val showActive = binding.checkboxActive.isChecked

        onFilterApplied(FilterOptions(showCompleted, showActive))
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class FilterOptions(
    val showCompleted: Boolean,
    val showActive: Boolean
)
```

---

## Permissions Handling

### Runtime Permissions with Activity Result API

```kotlin
package com.example.myapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Single permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.d("Permission granted")
            proceedWithFeature()
        } else {
            Timber.w("Permission denied")
            showPermissionDeniedMessage()
        }
    }

    // Multiple permissions
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Timber.d("All permissions granted")
            proceedWithFeature()
        } else {
            Timber.w("Some permissions denied")
            showPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonRequestPermission.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            hasPermission(Manifest.permission.CAMERA) -> {
                // Permission already granted
                Timber.d("Permission already granted")
                proceedWithFeature()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale
                showPermissionRationale()
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkAndRequestMultiplePermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )

        val permissionsToRequest = permissions.filter { !hasPermission(it) }

        if (permissionsToRequest.isEmpty()) {
            // All permissions already granted
            proceedWithFeature()
        } else {
            // Request missing permissions
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionRationale() {
        Snackbar.make(
            binding.root,
            "Camera permission is required for this feature",
            Snackbar.LENGTH_LONG
        )
            .setAction("Grant") {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .show()
    }

    private fun showPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            "Permission denied. Some features may not work.",
            Snackbar.LENGTH_LONG
        )
            .setAction("Settings") {
                openAppSettings()
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun proceedWithFeature() {
        Timber.d("Proceeding with feature")
        // Implement feature that requires permission
    }
}
```

### Permission Manager Helper

```kotlin
package com.example.myapp.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(it) }
    }

    fun getMissingPermissions(permissions: Array<String>): List<String> {
        return permissions.filter { !hasPermission(it) }
    }

    companion object {
        // Common permission groups
        val LOCATION_PERMISSIONS = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val STORAGE_PERMISSIONS = if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val CAMERA_PERMISSION = arrayOf(
            android.Manifest.permission.CAMERA
        )
    }
}

// Usage in ViewModel
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {

    fun checkCameraPermission(): Boolean {
        return permissionManager.hasPermission(Manifest.permission.CAMERA)
    }

    fun getMissingPermissions(): List<String> {
        return permissionManager.getMissingPermissions(
            PermissionManager.CAMERA_PERMISSION
        )
    }
}
```

### AndroidManifest Permissions

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Normal permissions (auto-granted) -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <!-- Dangerous permissions (require runtime request) -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <!-- Storage permissions -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <!-- Android 13+ media permissions -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

    <!-- Notification permission (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application>
        <!-- Activities, services, etc. -->
    </application>

</manifest>
```

---

## Navigation Patterns

### Navigation Component Setup

```kotlin
// In Activity
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupNavigation()
    }

    private fun setupNavigation() {
        // Setup ActionBar with Navigation
        setupActionBarWithNavController(navController)

        // Setup BottomNavigationView
        binding.bottomNav.setupWithNavController(navController)

        // Listen to destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.mainFragment -> showBottomNav()
                else -> hideBottomNav()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}

// In Fragment
class TodoListFragment : Fragment() {

    private fun navigateToDetail(todoId: String) {
        val action = TodoListFragmentDirections.actionTodoListToDetail(todoId)
        findNavController().navigate(action)
    }
}
```

---

## Tips and Best Practices

### UI Best Practices

1. **Use ViewBinding**: Always prefer ViewBinding over findViewById
2. **Null Safety in Fragments**: Always nullify binding in onDestroyView
3. **Use ListAdapter**: For RecyclerView with automatic diff calculations
4. **Lifecycle-Aware Collection**: Use repeatOnLifecycle for Flow collection
5. **Handle Configuration Changes**: ViewModels survive configuration changes

### Performance Tips

1. **RecyclerView**: Use setHasFixedSize(true) when size doesn't change
2. **ViewHolder**: Keep ViewHolder bind() method lightweight
3. **DiffUtil**: Implement proper areItemsTheSame and areContentsTheSame
4. **Image Loading**: Use libraries like Coil or Glide for efficient loading
5. **Avoid Nested RecyclerViews**: Can cause performance issues

### Common Pitfalls to Avoid

- Don't hold references to Views outside lifecycle
- Don't forget to cancel coroutines in onDestroyView
- Don't use !! for ViewBinding in Fragments
- Don't collect flows without lifecycle awareness
- Don't request permissions repeatedly without rationale

---

## Cross-References

- See [02-architecture.md](./02-architecture.md) for ViewModel patterns
- See [03-testing-and-builds.md](./03-testing-and-builds.md) for UI testing
- See [04-dependency-injection.md](./04-dependency-injection.md) for Activity/Fragment injection
- See [05-database-and-data.md](./05-database-and-data.md) for data Flow patterns
- See [07-optimization.md](./07-optimization.md) for UI performance optimization
