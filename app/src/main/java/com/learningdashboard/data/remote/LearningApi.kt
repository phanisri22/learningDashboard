package com.learningdashboard.data.remote

import com.learningdashboard.data.remote.dto.CourseDetailDto
import com.learningdashboard.data.remote.dto.CourseDto
import com.learningdashboard.data.remote.dto.LoginRequestDto
import com.learningdashboard.data.remote.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LearningApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @GET("courses")
    suspend fun getCourses(): List<CourseDto>

    @GET("courses/{courseId}")
    suspend fun getCourseDetail(@Path("courseId") courseId: Int): CourseDetailDto

    @POST("courses/{courseId}/lessons/{lessonId}/complete")
    suspend fun markLessonCompleted(
        @Path("courseId") courseId: Int,
        @Path("lessonId") lessonId: Int,
    )

    companion object {
        const val BASE_URL = "https://api.learningdashboard.mock/"
    }
}
