package br.inatel.tcc.service

fun calculateRaceProgressPercent(playerPositionKm: Double, goalDistanceKm: Double): Double {
    if (!playerPositionKm.isFinite() || !goalDistanceKm.isFinite() || goalDistanceKm <= 0.0) {
        return 0.0
    }

    return ((playerPositionKm.coerceAtLeast(0.0) / goalDistanceKm) * 100.0)
        .coerceIn(0.0, 100.0)
}
