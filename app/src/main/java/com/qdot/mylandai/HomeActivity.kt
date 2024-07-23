package com.qdot.mylandai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import coil.load
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.annotations.PolygonOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.permissionx.guolindev.PermissionX
import com.qdot.mylandai.databinding.ActivityHomeBinding
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var databases : Databases
    private lateinit var account : Account
    private lateinit var generativeModel: GenerativeModel
    private val polygonPoints: ArrayList<LatLng> = ArrayList()
    private val gson : Gson = Gson()
    private lateinit var okHttpClient : OkHttpClient
    private lateinit var styleUrl : String
    private val jsonType = "application/json".toMediaType()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val key = BuildConfig.MAPTILER_API_KEY
        val mapId = "satellite"
        styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$key"
        val client = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("PROJECT_ID")
            .setSelfSigned(true)

        okHttpClient = OkHttpClient()
        account = Account(client)
        databases = Databases(client)

        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "API_KEY"
        )

        PermissionX.init(this)
            .permissions(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList, "Core fundamental are based on these permissions", "OK", "Cancel")
            }
            .request{ allGranted, _, _ ->
                if(allGranted){
                    updateUi(styleUrl)
                }
            }
    }

    private fun updateUi(styleType : String) {
        binding.addFieldBtn.visibility = View.GONE
        binding.saveFieldBtn.visibility = View.GONE
        binding.clearFieldBtn.visibility = View.GONE
        binding.weatherCard.visibility= View.GONE
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                binding.mapView.getMapAsync {
                    it.setStyle(styleType)
                    it.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(location.latitude,location.longitude))
                        .zoom(15.0).build()
                }
            }
        CoroutineScope(Dispatchers.IO).launch {
            val user = account.get()
            val result = databases.listDocuments(
                databaseId = "669df1e1000d18adda57",
                collectionId = "669df1fc0029448b6bef",
                queries = listOf(
                    Query.equal("userid",user.id)
                )
            )
            if (result.documents.isEmpty() ){
                withContext(Dispatchers.Main) {
                    binding.addFieldBtn.visibility = View.VISIBLE
                    binding.addFieldBtn.setOnClickListener {
                        binding.addFieldBtn.visibility = View.GONE
                        binding.saveFieldBtn.visibility = View.VISIBLE
                        binding.clearFieldBtn.visibility = View.VISIBLE
                        binding.mapView.getMapAsync { mapbox ->
                            mapbox.addOnMapLongClickListener { lt ->
                                polygonPoints.add(lt)
                                mapbox.addMarker(MarkerOptions()
                                    .position(lt))
                                if (polygonPoints.size >= 4) {
                                    val ploy = PolygonOptions()
                                        .addAll(polygonPoints)
                                        .strokeColor(Color.RED)
                                        .fillColor(Color.argb(50, 255, 0, 0))
                                    mapbox.addPolygon(ploy)
                                }

                                return@addOnMapLongClickListener true
                            }
                            binding.clearFieldBtn.setOnClickListener {
                                polygonPoints.clear()
                                mapbox.clear()
                                mapbox.removeOnMapLongClickListener {
                                    return@removeOnMapLongClickListener true
                                }
                                binding.addFieldBtn.visibility = View.VISIBLE
                                binding.saveFieldBtn.visibility = View.GONE
                                binding.clearFieldBtn.visibility = View.GONE
                            }

                            binding.saveFieldBtn.setOnClickListener {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val pl = gson.toJson(polygonPoints)
                                    val llList : MutableList<MutableList<Double>> = mutableListOf()
                                    polygonPoints.map {
                                        llList.add(mutableListOf(it.longitude,it.latitude))
                                    }
                                    llList.add(mutableListOf(polygonPoints[0].longitude,polygonPoints[0].latitude))
                                    val agModel = SendPolyModel(
                                        name = user.id,
                                        geo_json = GeoJson(
                                            type = "Feature",
                                            properties = JSONObject("{}"),
                                            geometry = Geometry(
                                                type = "Polygon",
                                                coordinates = listOf(llList.toList())
                                            )
                                        )
                                    )

                                    val body = gson.toJson(agModel).toRequestBody(jsonType)
                                    val req = Request.Builder()
                                        .header("Content-Type","application/json")
                                        .url("http://api.agromonitoring.com/agro/1.0/polygons?appid=API_KEY")
                                        .post(body)
                                        .build()
                                    val resp = okHttpClient.newCall(req).execute()
                                    val jsp = JSONObject(resp.body!!.string())
                                    databases.createDocument(
                                        databaseId = "669df1e1000d18adda57",
                                        collectionId = "669df1fc0029448b6bef",
                                        documentId = ID.unique(),
                                        data = PolyModel(userid = user.id,
                                            ploygon = pl, id = jsp.getString("id"))
                                    )
                                    withContext(Dispatchers.Main){
                                        binding.addFieldBtn.visibility = View.GONE
                                        binding.saveFieldBtn.visibility = View.GONE
                                        binding.clearFieldBtn.visibility = View.GONE
                                        updateUi(styleUrl)
                                    }
                                }
                            }
                        }
                    }
                }
            }else{
                val polygonId = result.documents[0].data["id"].toString()
                val dta = result.documents[0].data["ploygon"].toString()
                val type = object : TypeToken<ArrayList<LatLng>>() {}.type
                val polyList : ArrayList<LatLng> = gson.fromJson(dta,type)
                val req = Request.Builder()
                    .header("Content-Type","application/json")
                    .url("http://api.agromonitoring.com/agro/1.0/polygons/${polygonId}?appid=API_KEY")
                    .build()
                val resp = okHttpClient.newCall(req).execute()
                val jsp = JSONObject(resp.body!!.string())
                val lon = jsp.getJSONArray("center")[0] as Double
                val lat = jsp.getJSONArray("center")[1] as Double
                val area = jsp.getDouble("area")

                val req1 = Request.Builder()
                    .header("Content-Type","application/json")
                    .url("https://api.agromonitoring.com/agro/1.0/weather?lat=${lat}&lon=${lon}&appid=API_KEY")
                    .build()
                val resp1 = okHttpClient.newCall(req1).execute()
                val wJson = JSONObject(resp1.body!!.string())
                val imgWeather = wJson.getJSONArray("weather").getJSONObject(0).getString("icon")
                val textWeather = wJson.getJSONArray("weather").getJSONObject(0).getString("main")
                val temp = wJson.getJSONObject("main").getDouble("temp")
                val pressure = wJson.getJSONObject("main").getDouble("pressure")
                val humidity = wJson.getJSONObject("main").getDouble("humidity")
                val wSpeed = wJson.getJSONObject("wind").getDouble("speed")
                val wDir = wJson.getJSONObject("wind").getDouble("deg")
                val clouds = wJson.getJSONObject("clouds").getDouble("all")

                val req2 = Request.Builder()
                    .header("Content-Type","application/json")
                    .url("http://api.agromonitoring.com/agro/1.0/soil?polyid=${polygonId}&appid=API_KEY")
                    .build()
                val resp2 = okHttpClient.newCall(req2).execute()
                val soilJson = JSONObject(resp2.body!!.string())
                val soilTemp10 = soilJson.getDouble("t10")
                val soilMos = soilJson.getDouble("moisture")
                val soilTemp = soilJson.getDouble("t0")

                val gimPrompt = "Assume you are an agriculture bot." +
                        " I have a farm land with location lat=${lat}°, long=${lon}°. " +
                        "Area of the land is $area ha. "+
                        "The weather current now is like temperature: $temp degrees, " +
                        "wind: $wSpeed m/s, humidity: $humidity%, air pressure: $pressure hPa and cloud percentage is $clouds%. " +
                        "The soil information is like Temperature on the 10 centimeters depth is ${soilTemp10}k, " +
                        "Soil moisture is $soilMos m3/m3 and Surface temperature is $soilTemp k. " +
                        "So suggest me top 5 crop for this farm field in a json format with a percentage of suitability " +
                        "and also 5 top tips for on how to improve the farm land based on the data given. " +
                        "Give only json output noting other. The json object should contain a json array named crops and under which there will be jsonobjects with two value crop and suitability " +
                        "on the other hand there will be another json array named tips under which 5  json objects with name tip"
                val gimResponse = generativeModel.generateContent(gimPrompt)

                val fieldInfo = FieldInfoModel(
                    lat,lon,area,temp,humidity,pressure,
                    wSpeed,wDir,clouds,soilTemp10,soilTemp,
                    soilMos,gimResponse.text.toString()
                )

                withContext(Dispatchers.Main) {
                    val btmSheet = FieldSheet(fieldInfo)
                    binding.addFieldBtn.visibility = View.GONE
                    binding.mapView.getMapAsync { mapbox ->
                        mapbox.addMarker(MarkerOptions()
                            .position(LatLng(lat, lon))
                            .title("Your Field"))
                        val ploy1 = PolygonOptions()
                            .addAll(polyList)
                            .strokeColor(Color.RED)
                            .fillColor(Color.argb(50, 255, 0, 0))
                        mapbox.addPolygon(ploy1)

                        mapbox.setOnPolygonClickListener {
                            btmSheet.show(supportFragmentManager,"FIELD")
                        }
                    }
                    binding.weatherCard.visibility = View.VISIBLE
                    binding.weatheImage.load("https://openweathermap.org/img/wn/${imgWeather}@2x.png")
                    binding.weatheInfo.text = textWeather
                }
            }
        }
    }
}