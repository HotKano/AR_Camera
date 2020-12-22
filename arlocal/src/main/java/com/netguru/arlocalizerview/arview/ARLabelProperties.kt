package com.netguru.arlocalizerview.arview

internal data class ARLabelProperties(
        val distance: Int,
        val pointName: String,
        val positionX: Float,
        val positionY: Float,
        val alpha: Int,
        val id: Int = 0
)
