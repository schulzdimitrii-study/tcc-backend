package br.inatel.tcc.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun handleIllegalArgument_shouldReturn409Conflict() {
        val ex = IllegalArgumentException("Test conflict error")
        val response = handler.handleIllegalArgument(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body).containsEntry("error", "Test conflict error")
    }

    @Test
    fun handleBadCredentials_shouldReturn401Unauthorized() {
        val ex = BadCredentialsException("Test bad credentials")
        val response = handler.handleBadCredentials(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body).containsEntry("error", "Test bad credentials")
    }

    @Test
    fun handleValidation_shouldReturn400BadRequest_withFieldErrors() {
        // Mocks for BindingResult and MethodArgumentNotValidException
        val bindingResult = mock(BindingResult::class.java)
        val fieldError = FieldError("objectName", "email", "Must be a valid email")
        whenever(bindingResult.fieldErrors).thenReturn(listOf(fieldError))
        
        val methodParameter = mock(MethodParameter::class.java)
        val ex = MethodArgumentNotValidException(methodParameter, bindingResult)

        val response = handler.handleValidation(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).containsEntry("email", "Must be a valid email")
    }

    @Test
    fun handleMessageNotReadable_shouldReturn400BadRequest() {
        val ex = HttpMessageNotReadableException("Required request body is missing", mock(org.springframework.http.HttpInputMessage::class.java))
        val response = handler.handleMessageNotReadable(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).containsEntry("error", "Invalid request body")
    }

    @Test
    fun handleGenericException_shouldReturn500InternalServerError() {
        val ex = Exception("Generic unhandled exception")
        val response = handler.handleGenericException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body).containsEntry("error", "Generic unhandled exception")
    }
}
