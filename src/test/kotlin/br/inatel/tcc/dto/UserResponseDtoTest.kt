package br.inatel.tcc.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class UserResponseDtoTest {

    @Test
    fun shouldRoundHeightAndWeightToTwoDecimalPlaces() {
        val dto = UserResponseDto(
            id = UUID.randomUUID(),
            email = "user@example.com",
            name = "Test User",
            birthdayDate = LocalDate.now(),
            maxHeartRate = 190,
            height = 1.7583,
            weight = 70.1234
        )

        assertEquals(1.76, dto.height)
        assertEquals(70.12, dto.weight)
    }

    @Test
    fun shouldHandleNullHeightAndWeight() {
        val dto = UserResponseDto(
            id = UUID.randomUUID(),
            email = "user@example.com",
            name = "Test User",
            birthdayDate = LocalDate.now(),
            maxHeartRate = 190,
            height = null,
            weight = null
        )

        assertEquals(null, dto.height)
        assertEquals(null, dto.weight)
    }
}
