package com.moviles.studentcoursessystem
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.messaging.FirebaseMessaging
import com.moviles.studentcoursessystem.models.Course
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
        subscribeToTopic()

        // Extract course data passed from MainActivity
        val courseId = intent.getIntExtra("COURSE_ID", -1)
        val courseName = intent.getStringExtra("COURSE_NAME") ?: "Unknown Course"

        if (courseId == -1) {
            Log.e("StudentsActivity", "Invalid course ID received")
            finish()
            return
        }

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

    // UI state
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    val context = LocalContext.current

    // Load students for the specific course when entering the screen
    LaunchedEffect(courseId) {
        viewModel.fetchStudentsForCourse(courseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estudiantes en $courseName") },
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
                Text("No hy estudiantes matriculados a este curso")
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
                items(students.filter { it.courseId == courseId }) { student ->
                    StudentCard(
                        student = student,
                        onClick = {
                            try {
                                // Debugging logs
                                Log.d("StudentsActivity", "Navigating to StudentDetailActivity with student Id: ${student.id}")

                                // Navigate to StudentDetailActivity when a student is clicked
                                val intent = Intent(context, StudentDetailActivity::class.java).apply {
                                    putExtra("STUDENT_ID", student.id ?: -1) // Add null safety
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error navigating to StudentsActivity", e)
                            }
                        },
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

        // Add/Edit Student Dialog
        if ((showAddDialog || showEditDialog) && (showEditDialog && selectedStudent != null || showAddDialog)) {
            StudentFormDialog(
                student = selectedStudent,
                courseId = courseId,
                onDismiss = {
                    showAddDialog = false
                    showEditDialog = false
                },
                onSave = { student ->
                    if (showAddDialog) {
                        viewModel.addStudent(
                            name = student.name,
                            email = student.email,
                            phone = student.phone,
                            courseId = courseId,
                            onSuccess = { showAddDialog = false },
                            onError = { error -> Log.e("StudentsActivity", "Error adding student: $error") }
                        )
                    } else {
                        viewModel.updateStudent(
                            student = student,
                            onSuccess = { showEditDialog = false },
                            onError = { error -> Log.e("StudentsActivity", "Error updating student: $error") }
                        )
                    }
                }
            )
        }

        // Delete confirmation dialog
        if (showDeleteDialog && selectedStudent != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Estás seguro que deseas eliminar a ${selectedStudent?.name} de este curso?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteStudent(selectedStudent?.id)
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Delete")
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
                        Text("Cancel")
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
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Editar Estudiante" else "Agregar Estudiante") },
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
                            courseId = courseId
                        )
                        onSave(updatedStudent)
                    } else {
                        errorMessage = "Por favor llenar todos los campos de manera correcta"
                    }
                }
            ) {
                Text(if (isEdit) "Update" else "Save")
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
                Text("Cancel")
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


// Functions to create the notification after create an student


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

fun subscribeToTopic() {
    FirebaseMessaging.getInstance().subscribeToTopic("event_notifications")
        .addOnCompleteListener { task ->
            var msg = "Subscription successful"
            if (!task.isSuccessful) {
                msg = "Subscription failed"
            }
            Log.d("FCM", msg)
        }
}