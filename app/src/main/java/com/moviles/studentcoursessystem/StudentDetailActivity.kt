package com.moviles.studentcoursessystem

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moviles.studentcoursessystem.models.Student
import com.moviles.studentcoursessystem.viewmodel.StudentViewModel

class StudentDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener ID del estudiante del intent de manera más segura
        val studentId = intent?.extras?.getInt("STUDENT_ID", -1) ?: -1

        if (studentId == -1) {
            Log.e("StudentDetail", "No se recibió un ID de estudiante válido")
            showErrorAndFinish()
            return
        }

        setContent {
            MaterialTheme {
                val studentViewModel: StudentViewModel = viewModel()
                StudentDetailScreen(
                    studentId = studentId,
                    onBackPressed = { finish() },
                    viewModel = studentViewModel
                )
            }
        }
    }

    private fun showErrorAndFinish() {
        Toast.makeText(this, "Error: Estudiante no válido", Toast.LENGTH_LONG).show()
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    studentId: Int,
    viewModel: StudentViewModel,
    onBackPressed: () -> Unit
) {
    // Cargar los datos del estudiante
    LaunchedEffect(studentId) {
        viewModel.fetchStudent(studentId)
    }

    val student by viewModel.selectedStudent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val courseName by viewModel.courseName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Estudiante") },
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
        } else if (student == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No se encontró el estudiante")
            }
        } else {
            StudentDetailContent(
                student = student!!,
                courseName = courseName,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun StudentDetailContent(
    student: Student,
    courseName: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tarjeta con la información del estudiante
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Información del Estudiante",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Divider()

                DetailItem("Nombre", student.name)
                DetailItem("Correo Electrónico", student.email)
                DetailItem("Teléfono", student.phone)
                DetailItem("Curso Matriculado", courseName ?: "No asignado")
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}