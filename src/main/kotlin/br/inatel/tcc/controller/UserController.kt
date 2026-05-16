package br.inatel.tcc.controller

import br.inatel.tcc.domain.user.User
import br.inatel.tcc.dto.UserResponseDto
import br.inatel.tcc.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@Tag(name = "User", description = "User API")
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {

    @Operation(
        summary = "Get a user",
        responses = [
            ApiResponse(
                description = "User",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = UserResponseDto::class)
                    )
                ]
            )
        ]
    )
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID): ResponseEntity<UserResponseDto> {
        return ResponseEntity.ok(userService.getUser(id))
    }

    @Operation(
        summary = "Update a user",
        responses = [
            ApiResponse(
                description = "User updated",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = UserResponseDto::class)
                    )
                ]
            )
        ]
    )
    @PatchMapping("/{id}")
    fun updateUser(@PathVariable id: UUID, @RequestBody user: User): ResponseEntity<UserResponseDto> {
        return ResponseEntity.ok(userService.updateUser(id, user))
    }
}