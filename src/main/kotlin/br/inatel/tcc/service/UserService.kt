package br.inatel.tcc.service

import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.user.UserRepository
import br.inatel.tcc.dto.UserResponseDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository
) {
    fun getUser(id: UUID): UserResponseDto {
        return userRepository.findUserResponseById(id).orElseThrow { IllegalArgumentException("User not found") }
    }

    fun updateUser(id: UUID, user: User): UserResponseDto {
        val existingUser = userRepository.findById(id).orElseThrow { IllegalArgumentException("User not found") }
        
        val updatedUser = User(
            id = id,
            name = user.name.takeIf { it.isNotBlank() } ?: existingUser.name,
            email = user.email.takeIf { it.isNotBlank() } ?: existingUser.email,
            birthdayDate = user.birthdayDate ?: existingUser.birthdayDate,
            password = user.password.takeIf { it.isNotBlank() } ?: existingUser.password,
            maxHeartRate = user.maxHeartRate ?: existingUser.maxHeartRate,
            height = user.height ?: existingUser.height,
            weight = user.weight ?: existingUser.weight
        )
        
        val savedUser = userRepository.save(updatedUser)
        
        return UserResponseDto(
            id = savedUser.id!!,
            email = savedUser.email,
            name = savedUser.name,
            birthdayDate = savedUser.birthdayDate,
            maxHeartRate = savedUser.maxHeartRate,
            height = savedUser.height,
            weight = savedUser.weight
        )
    }
}