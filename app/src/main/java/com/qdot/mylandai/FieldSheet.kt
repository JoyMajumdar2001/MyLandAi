package com.qdot.mylandai

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.qdot.mylandai.databinding.FragmentFieldSheetBinding
import org.json.JSONObject

class FieldSheet(val fieldInfoModel: FieldInfoModel) : BottomSheetDialogFragment() {

    private var _binding: FragmentFieldSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFieldSheetBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.areaText.text = "${fieldInfoModel.area} ha area"

        binding.tempBtn.text = "${fieldInfoModel.temp} k"
        binding.pressBtn.text = "${fieldInfoModel.pressure} hPa"
        binding.humidBtn.text = "${fieldInfoModel.humidity} %"
        binding.windSpeedBtn.text = "${fieldInfoModel.windSpeed} m/s"
        binding.windDirBtn.text = "${fieldInfoModel.windDirection}°"
        binding.cloudBtn.text = "${fieldInfoModel.cloud} %"

        binding.soil10TempText.text = "${fieldInfoModel.soilTemp10} k"
        binding.soilTempText.text = "${fieldInfoModel.soilTemp} k"
        binding.soilMosText.text = "${fieldInfoModel.soilMoisture} m3/m3"

        val response = fieldInfoModel.aiResponse.substring(8, fieldInfoModel.aiResponse.length - 3)

        val json = JSONObject(response)

        val crops = json.getJSONArray("crops")
        val tips = json.getJSONArray("tips")

        binding.crop1.text = crops.getJSONObject(0).getString("crop")
        binding.crop1Percent.text = crops.getJSONObject(0)
            .getDouble("suitability").toString()  + " %"

        binding.crop2.text = crops.getJSONObject(1).getString("crop")
        binding.crop2Percent.text = crops.getJSONObject(1)
            .getDouble("suitability").toString() + " %"

        binding.crop3.text = crops.getJSONObject(2).getString("crop")
        binding.crop3Percent.text = crops.getJSONObject(2)
            .getDouble("suitability").toString() + " %"

        binding.crop4.text = crops.getJSONObject(3).getString("crop")
        binding.crop4Percent.text = crops.getJSONObject(3)
            .getDouble("suitability").toString() + " %"

        binding.crop5.text = crops.getJSONObject(4).getString("crop")
        binding.crop5Percent.text = crops.getJSONObject(4)
            .getDouble("suitability").toString() + " %"

        binding.tip1.text = tips.getJSONObject(0).getString("tip")
        binding.tip2.text = tips.getJSONObject(1).getString("tip")
        binding.tip3.text = tips.getJSONObject(2).getString("tip")
        binding.tip4.text = tips.getJSONObject(3).getString("tip")
        binding.tip5.text = tips.getJSONObject(4).getString("tip")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}