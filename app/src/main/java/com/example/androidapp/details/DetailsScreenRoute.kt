package com.example.androidapp.details

import com.example.androidapp.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data class DetailsScreenRoute(
    val id: Long
) : Route
