# 📱 App Android - Gestión de Cursos y Estudiantes

Esta es la aplicación móvil del sistema de gestión de cursos y estudiantes. Desarrollada en Kotlin utilizando Jetpack Compose y MVVM, permite consumir la API REST desarrollada en .NET. Recibe notificaciones push al registrar nuevos estudiantes.

---

## 🧰 Tecnologías y herramientas

- Jetpack Compose (UI)
- MVVM (Arquitectura)
- Retrofit + OkHttp (consumo de API con caché)
- Room (almacenamiento local)
- Firebase Cloud Messaging (notificaciones push)
- Kotlin

---

## ⚙️ Requisitos

- Android Studio
- Emulador o dispositivo físico con Android 8+
- API REST corriendo local o remotamente

---

## 🚀 Configuración del proyecto

1. Clona el repositorio:

   ```bash
   git clone https://github.com/jamyr17/Frontend-Exam-Android
   ```

2. Abre el proyecto en Android Studio.

3. Ejecuta el proyecto en un emulador o dispositivo físico.

---

## ✨ Funcionalidades

### 📋 Lista de Cursos (MainActivity)

- Mostrar cursos con nombre, descripción, imagen, horario y profesor.
- Crear, editar y eliminar cursos.
- Funciona offline gracias a Room.
- Alerta visual si los datos vienen de Room o caché (OkHttp).

### 👨‍🎓 Estudiantes por Curso (StudentsActivity)

- Mostrar estudiantes inscritos en un curso.
- Crear, editar y eliminar estudiantes.
- Recibir notificación push (FCM) al registrar un nuevo estudiante.
- Funcionalidad offline con Room y caché.

### 🧑 Perfil del Estudiante (StudentDetailActivity)

- Mostrar todos los datos del estudiante y el curso asociado.
- Funciona sin conexión y alerta la fuente de datos (local/caché).

---

## 🔔 Notificaciones Push

La app recibe notificaciones FCM al registrar un nuevo estudiante:

```
Estudiante: [nombre del estudiante], se ha inscrito al curso: [nombre del curso]
```

---

## 🧪 Validaciones

- Las reglas de validación del cliente están sincronizadas con las del backend.
- Se evita enviar datos inválidos a la API.

---

## 🧑‍💻 Autor

- [Jamyr González] – [GitHub](https://github.com/jamyr17)
- [Jamel Sandí] – [GitHub](https://github.com/Jamel-sanderson)
