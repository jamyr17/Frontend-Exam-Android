package com.moviles.studentcoursessystem.common

object Constants {
    const val API_BASE_URL = "http://10.0.2.2:5000/"
    const val IMAGES_BASE_URL = API_BASE_URL + "uploads/"

    // Course validation rules
    const val COURSE_NAME_MIN_LENGTH = 10
    const val COURSE_NAME_MAX_LENGTH = 63
    const val COURSE_DESCRIPTION_MIN_LENGTH = 10
    const val COURSE_DESCRIPTION_MAX_LENGTH = 500
    const val SCHEDULE_MAX_LENGTH = 100
    const val PROFESSOR_NAME_MAX_LENGTH = 255

    // Student validation rules
    const val STUDENT_NAME_MIN_LENGTH = 2
    const val STUDENT_NAME_MAX_LENGTH = 255
    const val PHONE_MIN_LENGTH = 7
    const val PHONE_MAX_LENGTH = 15

    private val NAME_REGEX = Regex("^[\\p{L}\\s]+$")
    private val PHONE_REGEX = Regex("^[0-9]+$") // Only digits
    private val EMAIL_REGEX = android.util.Patterns.EMAIL_ADDRESS

    fun validateCourseInput(
        name: String,
        description: String,
        schedule: String,
        professor: String
    ): Map<String, String?> {
        val errors = mutableMapOf<String, String?>()

        errors["name"] = when {
            name.isBlank() -> "Course name is required"
            name.length < COURSE_NAME_MIN_LENGTH -> "Course name must be at least $COURSE_NAME_MIN_LENGTH characters"
            name.length > COURSE_NAME_MAX_LENGTH -> "Course name must not exceed $COURSE_NAME_MAX_LENGTH characters"
            else -> null
        }

        errors["description"] = when {
            description.isBlank() -> "Course description is required"
            description.length < COURSE_DESCRIPTION_MIN_LENGTH -> "Description must be at least $COURSE_DESCRIPTION_MIN_LENGTH characters"
            description.length > COURSE_DESCRIPTION_MAX_LENGTH -> "Description must not exceed $COURSE_DESCRIPTION_MAX_LENGTH characters"
            else -> null
        }

        errors["schedule"] = when {
            schedule.isBlank() -> "Schedule is required"
            schedule.length > SCHEDULE_MAX_LENGTH -> "Schedule must not exceed $SCHEDULE_MAX_LENGTH characters"
            else -> null
        }

        errors["professor"] = when {
            professor.isBlank() -> "Professor name is required"
            professor.length > PROFESSOR_NAME_MAX_LENGTH -> "Professor name must not exceed $PROFESSOR_NAME_MAX_LENGTH characters"
            else -> null
        }

        return errors
    }

    fun validateStudentInput(
        name: String,
        email: String,
        phone: String,
        courseId: Int?
    ): Map<String, String?> {
        val errors = mutableMapOf<String, String?>()

        errors["name"] = when {
            name.isBlank() -> "Name is required"
            name.length < STUDENT_NAME_MIN_LENGTH || name.length > STUDENT_NAME_MAX_LENGTH ->
                "Name must be between $STUDENT_NAME_MIN_LENGTH and $STUDENT_NAME_MAX_LENGTH characters"
            !NAME_REGEX.matches(name) -> "Name can only contain letters and spaces"
            else -> null
        }

        errors["email"] = when {
            email.isBlank() -> "Email is required"
            !EMAIL_REGEX.matcher(email).matches() -> "Please enter a valid email address"
            else -> null
        }

        errors["phone"] = when {
            phone.isBlank() -> "Phone is required"
            phone.length < PHONE_MIN_LENGTH || phone.length > PHONE_MAX_LENGTH ->
                "Phone number must be between $PHONE_MIN_LENGTH and $PHONE_MAX_LENGTH digits"
            !PHONE_REGEX.matches(phone) -> "Phone number can only contain digits"
            else -> null
        }

        errors["courseId"] = when {
            courseId == null -> "Course ID is required"
            courseId <= 0 -> "Course ID must be a valid number"
            else -> null
        }

        return errors
    }
}
