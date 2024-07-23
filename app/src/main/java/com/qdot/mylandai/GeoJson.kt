package com.qdot.mylandai

import org.json.JSONObject

data class GeoJson(
    val geometry: Geometry,
    val type: String,
    val properties: JSONObject
)