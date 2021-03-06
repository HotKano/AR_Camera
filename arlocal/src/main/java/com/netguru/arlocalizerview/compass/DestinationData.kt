package com.netguru.arlocalizerview.compass

import com.netguru.arlocalizerview.location.LocationData

data class DestinationData(
    val currentDestinationAzimuth: Float,
    val distanceToDestination: Int,
    val destinationLocation: LocationData
)
