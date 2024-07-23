package com.qdot.mylandai

data class FieldInfoModel(
    val lat : Double,
    val lon : Double,
    val area : Double,
    val temp : Double,
    val humidity : Double,
    val pressure: Double,
    val windSpeed : Double,
    val windDirection : Double,
    val cloud : Double,
    val soilTemp10 : Double,
    val soilTemp : Double,
    val soilMoisture : Double,
    val aiResponse : String
)
