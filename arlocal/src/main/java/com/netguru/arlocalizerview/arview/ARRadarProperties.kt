package com.netguru.arlocalizerview.arview

internal data class ARRadarProperties(
        val distance: Int,
        val pointName: String,
        val positionX: Float,
        val positionY: Float,
        val rotation: Float,
        val id: Int = 0
)
