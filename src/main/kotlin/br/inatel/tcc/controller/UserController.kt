package br.inatel.tcc.controller

import br.inatel.tcc.domain.user.User
import br.inatel.tcc.dto.UserResponseDto
import br.inatel.tcc.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID): ResponseEntity<UserResponseDto> {
        return ResponseEntity.ok(userService.getUser(id))
    }

    @PatchMapping("/{id}")
    fun updateUser(@PathVariable id: UUID, @RequestBody user: User): ResponseEntity<UserResponseDto> {
        return ResponseEntity.ok(userService.updateUser(id, user))
    }
}