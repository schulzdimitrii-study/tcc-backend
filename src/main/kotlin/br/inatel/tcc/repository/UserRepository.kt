package br.inatel.tcc.domain.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import br.inatel.tcc.dto.UserResponseDto
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean

    @Query("SELECT new br.inatel.tcc.dto.UserResponseDto(u.id, u.email, u.name, u.birthdayDate, u.maxHeartRate, u.height, u.weight) FROM User u WHERE u.id = :id")
    fun findUserResponseById(id: UUID): Optional<UserResponseDto>
}
