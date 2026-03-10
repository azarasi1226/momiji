package jp.momiji.feature

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ExceptionAdvice {
  @ExceptionHandler(UseCaseException::class)
  fun handleUseCaseException(e: UseCaseException): ResponseEntity<Map<String, String>> {
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(mapOf("error" to (e.error.message)))
  }
}
