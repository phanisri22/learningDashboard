package com.learningdashboard.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class LoginResponseDto(val token: String, val user: UserDto)

@Serializable
data class UserDto(val id: String, val email: String)

@Serializable
data class ErrorDto(val message: String)

@Serializable
data class CourseDto(
    val id: Int,
    val title: String,
    val instructor: String,
    val progress: Int,
    val lessons: Int,
)

@Serializable
data class CourseDetailDto(
    val id: Int,
    val title: String,
    val instructor: String,
    val lessons: List<LessonDto>,
)

@Serializable
data class LessonDto(
    val id: Int,
    val title: String,
    val completed: Boolean,
)
