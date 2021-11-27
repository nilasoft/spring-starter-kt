package com.nilasoft.kotlinstarter.core

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

object ErrorConstant {

  const val NOT_FOUND = "not-found"

  const val FORBIDDEN = "forbidden"

  const val INTERNAL = "internal"

}

data class ApiError(val type: String, val message: String)

open class ApiException(val status: HttpStatus, val type: String, throwable: Throwable) : Exception(throwable) {

  companion object {
    private operator fun String.not() = Exception(this)
  }

  constructor(status: HttpStatus, type: String, message: String) : this(status, type, !message)

  operator fun not(): ResponseEntity<ApiError> {
    printStackTrace()
    return ApiError(type, message!!) response status
  }

}

@RestControllerAdvice
class ApiExceptionController {

  @ExceptionHandler(ApiException::class)
  fun handle(exception: ApiException) = !exception

  @ExceptionHandler(NoSuchElementException::class)
  fun handle(exception: NoSuchElementException) =
    !ApiException(HttpStatus.NOT_FOUND, ErrorConstant.NOT_FOUND, exception)

  @ExceptionHandler(Exception::class)
  fun handle(exception: Exception) =
    !ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorConstant.INTERNAL, exception)

}
