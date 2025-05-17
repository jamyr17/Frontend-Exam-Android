package com.moviles.studentcoursessystem.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moviles.studentcoursessystem.common.Constants.IMAGES_BASE_URL
import com.moviles.studentcoursessystem.common.Constants.validateCourseInput
import com.moviles.studentcoursessystem.model.Course
import com.moviles.studentcoursessystem.viewmodel.CourseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val viewModel: CourseViewModel = viewModel()
                CourseManagementApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseManagementApp(viewModel: CourseViewModel) {
    val courses by viewModel.courses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // UI states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Detectar cambios de fuente de datos (CACHE o API)
    LaunchedEffect(Unit) {
        viewModel.dataSource.collect { source ->
            source?.let {
                snackbarHostState.showSnackbar("Datos cargados desde: $it")
                viewModel.clearDataSource()
            }
        }
    }

    // Fetch courses when entering the screen
    LaunchedEffect(Unit) {
        viewModel.fetchCourses()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cursos Disponibles") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedCourse = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar Curso")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando cursos...")
                }
            }
        } else if (courses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay cursos disponibles")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(courses) { course ->
                    CourseCard(
                        course = course,
                        onCourseClick = {
                            try {
                                // Debugging logs
                                Log.d("MainActivity", "Navigating to StudentsActivity with courseId: ${course.id}")

                                // Navigate to StudentsActivity when a course is clicked
                                val intent = Intent(context, StudentsActivity::class.java).apply {
                                    putExtra("COURSE_ID", course.id ?: -1) // Add null safety
                                    putExtra("COURSE_NAME", course.name)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error navigating to StudentsActivity", e)
                            }
                        },
                        onEditClick = {
                            selectedCourse = course
                            showEditDialog = true
                        },
                        onDeleteClick = {
                            selectedCourse = course
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog && selectedCourse != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Confirmar eliminación") },
                text = { Text("¿Estás seguro de que deseas eliminar el curso '${selectedCourse?.name}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCourse(selectedCourse?.id)
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Add/Edit course dialog
        if ((showAddDialog || showEditDialog) && (showEditDialog && selectedCourse != null || showAddDialog)) {
            CourseFormDialog(
                course = if (showEditDialog) selectedCourse else null,
                onDismiss = {
                    showAddDialog = false
                    showEditDialog = false
                },
                onSave = { course, imageUri ->
                    if (showAddDialog) {
                        // Add new course
                        if (imageUri != null) {
                            viewModel.addCourse(
                                name = course.name,
                                description = course.description,
                                schedule = course.schedule,
                                professor = course.professor,
                                imageUri = imageUri,
                                context = context,
                                onSuccess = {
                                    showAddDialog = false
                                },
                                onError = { error ->
                                    Log.e("MainActivity", "Error adding course: $error")
                                }
                            )
                        }
                    } else {
                        // Update existing course
                        if (imageUri != null) {
                            viewModel.updateCourse(
                                course = course,
                                imageUri = imageUri,
                                context = context,
                                onSuccess = {
                                    showEditDialog = false
                                },
                                onError = { error ->
                                    Log.e("MainActivity", "Error updating course: $error")
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun CourseCard(
    course: Course,
    onCourseClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onCourseClick() },  // Make the entire card clickable
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Image section
            RemoteImage(IMAGES_BASE_URL + course.imageUrl)
            Spacer(modifier = Modifier.height(12.dp))

            // Course details
            Text(
                text = course.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = course.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "📅 ${course.schedule}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "👨‍🏫 ${course.professor}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteImage(imageUrl: String) {
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp), // Set fixed height
        contentScale = ContentScale.Fit // Crop to fit the box
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseFormDialog(
    course: Course?,
    onDismiss: () -> Unit,
    onSave: (Course, Uri?) -> Unit
) {
    val isEditMode = course != null

    var name by remember { mutableStateOf(course?.name ?: "") }
    var description by remember { mutableStateOf(course?.description ?: "") }
    var schedule by remember { mutableStateOf(course?.schedule ?: "") }
    var professor by remember { mutableStateOf(course?.professor ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var currentImageUrl by remember { mutableStateOf(course?.imageUrl) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    var errors by remember { mutableStateOf(mapOf<String, String?>()) }

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            imageUri = it
            currentImageUrl = null // Clear the current URL since we have a new image
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "Editar Curso" else "Agregar Nuevo Curso") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Error message display
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Form fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Curso") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errors["name"] != null
                )
                if (errors["name"] != null) {
                    Text(text = errors["name"]!!, color = Color.Red)
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    isError = errors["description"] != null
                )
                if (errors["description"] != null) {
                    Text(text = errors["description"]!!, color = Color.Red)
                }

                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("Horario") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errors["schedule"] != null
                )
                if (errors["schedule"] != null) {
                    Text(text = errors["schedule"]!!, color = Color.Red)
                }

                OutlinedTextField(
                    value = professor,
                    onValueChange = { professor = it },
                    label = { Text("Profesor") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errors["professor"] != null
                )
                if (errors["professor"] != null) {
                    Text(text = errors["professor"]!!, color = Color.Red)
                }

                // Image section
                Text(
                    text = "Imagen del Curso:",
                    style = MaterialTheme.typography.labelMedium
                )

                // Show existing or new image
                if (imageUri != null) {
                    // New selected image from device
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Vista previa de la imagen",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop
                    )
                } else if (currentImageUrl != null) {
                    // Existing image from server
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagen actual del curso",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // No image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay imagen seleccionada")
                    }
                }

                Button(
                    onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isEditMode) "Cambiar Imagen" else "Seleccionar Imagen")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validation = validateCourseInput(name, description, schedule, professor)
                    if (validation.values.any { it != null }) {
                        errors = validation
                    } else {
                        errorMessage = null

                        val newCourse = Course(
                            id = course?.id,
                            name = name.trim(),
                            description = description.trim(),
                            imageUrl = currentImageUrl,
                            schedule = schedule.trim(),
                            professor = professor.trim()
                        )

                        onSave(newCourse, imageUri)
                    }
                }
            ) {
                Text(if (isEditMode) "Actualizar" else "Guardar")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Cancelar")
            }
        }
    )
}
