package com.moviles.studentcoursessystem
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moviles.studentcoursessystem.models.Student
import com.moviles.studentcoursessystem.viewmodel.StudentViewModel

/**
 * StudentsActivity displays and manages students enrolled in a specific course.
 * It allows viewing, adding, editing, and deleting students for the selected course.
 */
class StudentsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)

        // Extract course data passed from MainActivity
        val courseId = intent.getIntExtra("COURSE_ID", -1)
        val courseName = intent.getStringExtra("COURSE_NAME") ?: "Unknown Course"

        if (courseId == -1) {
            Log.e("StudentsActivity", "Invalid course ID received")
            Toast.makeText(this, "Error: Invalid course ID", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Debug log to confirm the courseId was received correctly
        Log.d("StudentsActivity", "Started with courseId: $courseId, courseName: $courseName")

        setContent {
            MaterialTheme {
                val viewModel: StudentViewModel = viewModel()
                StudentManagementScreen(
                    viewModel = viewModel,
                    courseId = courseId,
                    courseName = courseName,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

/**
 * Main screen for student management within a course
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentManagementScreen(
    viewModel: StudentViewModel,
    courseId: Int,
    courseName: String,
    onBackPressed: () -> Unit
) {
    val students by viewModel.students.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // UI state
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }

    // Load students for the specific course when entering the screen
    LaunchedEffect(courseId) {
        Log.d("StudentManagementScreen", "Fetching students for course ID: $courseId")
        viewModel.fetchStudentsForCourse(courseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estudiantes de $courseName") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedStudent = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar estudiante")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay estudiantes asociados a este curso")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(students) { student ->
                    StudentCard(
                        student = student,
                        onEditClick = {
                            selectedStudent = student
                            showEditDialog = true
                        },
                        onDeleteClick = {
                            selectedStudent = student
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        // Add Student Dialog
        if (showAddDialog) {
            StudentFormDialog(
                student = null,
                courseId = courseId,  // Pass the courseId explicitly
                onDismiss = { showAddDialog = false },
                onSave = { student ->
                    Log.d("StudentManagementScreen", "Adding student with courseId: $courseId")
                    viewModel.addStudent(
                        name = student.name,
                        email = student.email,
                        phone = student.phone,
                        courseId = courseId,  // Use the courseId from the parent screen
                        onSuccess = {
                            showAddDialog = false
                            // Show success message
                            Toast.makeText(context, "Estudiante agregado correctamente", Toast.LENGTH_SHORT).show()
                            // Refresh the student list after adding
                            viewModel.fetchStudentsForCourse(courseId)
                        },
                        onError = { error ->
                            Log.e("StudentsActivity", "Error adding student: $error")
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        // Edit Student Dialog
        if (showEditDialog && selectedStudent != null) {
            StudentFormDialog(
                student = selectedStudent,
                courseId = courseId,  // Pass the courseId explicitly
                onDismiss = { showEditDialog = false },
                onSave = { student ->
                    // Ensure the updated student keeps the same courseId
                    val studentWithCourseId = student.copy(courseId = courseId)
                    viewModel.updateStudent(
                        student = studentWithCourseId,
                        onSuccess = {
                            showEditDialog = false
                            // Show success message
                            Toast.makeText(context, "Estudiante actualizado correctamente", Toast.LENGTH_SHORT).show()
                            // Refresh the student list after updating
                            viewModel.fetchStudentsForCourse(courseId)
                        },
                        onError = { error ->
                            Log.e("StudentsActivity", "Error updating student: $error")
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        // Delete confirmation dialog
        if (showDeleteDialog && selectedStudent != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Confirmar eliminación") },
                text = { Text("Estás seguro que deseas eliminar a ${selectedStudent?.name} de este curso?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteStudent(selectedStudent?.id)
                            showDeleteDialog = false
                            // Refresh the student list after deletion
                            viewModel.fetchStudentsForCourse(courseId)
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
    }
}

/**
 * Card displaying student information
 */
@Composable
fun StudentCard(
    student: Student,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Email and phone removed as requested
            }

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Dialog form for adding or editing a student
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFormDialog(
    student: Student?,
    courseId: Int,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {
    val isEdit = student != null

    var name by remember { mutableStateOf(student?.name ?: "") }
    var email by remember { mutableStateOf(student?.email ?: "") }
    var phone by remember { mutableStateOf(student?.phone ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // For debugging purposes - keep this but don't display to user
    Log.d("StudentFormDialog", "Dialog opened with courseId: $courseId")
    if (isEdit) {
        Log.d("StudentFormDialog", "Editing student with ID: ${student?.id}, current courseId: ${student?.courseId}")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Editar estudiante" else "Agregar estudiante") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank() && errorMessage != null
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = email.isBlank() && errorMessage != null
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Número telefónico") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phone.isBlank() && errorMessage != null
                )

                // Course ID display removed - keeping the logic but not displaying it
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateStudentInputs(name, email, phone)) {
                        val updatedStudent = Student(
                            id = student?.id,
                            name = name.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            courseId = courseId  // Ensure we're using the passed courseId
                        )

                        // Log the student being saved
                        Log.d("StudentFormDialog", "Saving student with courseId: $courseId")

                        onSave(updatedStudent)
                    } else {
                        errorMessage = "Porfavor llenar todos los campos con datos correctos"
                    }
                }
            ) {
                Text(if (isEdit) "Actualizar" else "Guardar")
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

/**
 * Validates student form inputs
 */
private fun validateStudentInputs(
    name: String,
    email: String,
    phone: String
): Boolean {
    return name.isNotBlank() && email.isNotBlank() && phone.isNotBlank() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "event_reminder_channel"
        val channelName = "Event Reminders"
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Notifies users about upcoming events"
        }

        val notificationManager =
            context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }
}

