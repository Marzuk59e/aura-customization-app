package com.aura.launcher.core.network

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val code: Int = -1, val message: String, val errorBody: ApiError? = null) : NetworkResult<Nothing>
    data object Loading : NetworkResult<Nothing>
}

data class ApiError(
    val status: Int = 0,
    val code: String = "error",
    val message: String = "An unexpected error occurred",
    val fieldErrors: Map<String, String>? = null,
    val requestId: String? = null
)

data class PaginatedResponse<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalPages: Int = 1
)
