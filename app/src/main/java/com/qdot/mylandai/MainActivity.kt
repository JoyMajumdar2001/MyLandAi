package com.qdot.mylandai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qdot.mylandai.databinding.ActivityMainBinding
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.services.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var userId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userId = ""
        val client = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("PROJECT_ID")
            .setSelfSigned(true)

        val account = Account(client)

        binding.countryPicker.visibility = View.VISIBLE
        binding.mobLay.visibility = View.VISIBLE
        binding.sendBtn.visibility = View.VISIBLE
        binding.otpLay.visibility = View.GONE
        binding.validateBtn.visibility = View.GONE

        binding.sendBtn.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val acc = account.get()
                if (acc.id.isEmpty()){
                    withContext(Dispatchers.Main) {
                        binding.sendBtn.isEnabled = true
                    }
                }else{
                    startActivity(Intent(this@MainActivity,HomeActivity::class.java))
                    finishAffinity()
                }
            }catch(e : Exception){
                withContext(Dispatchers.Main) {
                    binding.sendBtn.isEnabled = true
                }
            }
        }

        binding.sendBtn.setOnClickListener {
            if (binding.mobText.text.isNullOrEmpty()){
                Toast.makeText(this,"Enter a valid mobile number",Toast.LENGTH_SHORT).show()
            }else{
                val mobNo = binding.countryPicker.defaultCountryCodeWithPlus+binding.mobText.text
                CoroutineScope(Dispatchers.IO).launch {
                    val token = account.createPhoneToken(
                        userId = ID.unique(),
                        phone = mobNo
                    )
                    userId = token.userId
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity,"OTP has been sent",Toast.LENGTH_SHORT).show()
                        binding.countryPicker.visibility = View.GONE
                        binding.mobLay.visibility = View.GONE
                        binding.sendBtn.visibility = View.GONE
                        binding.otpLay.visibility = View.VISIBLE
                        binding.validateBtn.visibility = View.VISIBLE
                    }
                }
            }
        }

        binding.validateBtn.setOnClickListener {
            if (binding.otpText.text.isNullOrEmpty()){
                Toast.makeText(this,"Enter a valid OTP",Toast.LENGTH_SHORT).show()
            }else{
                val otp = binding.otpText.text!!
                CoroutineScope(Dispatchers.IO).launch {
                    account.updatePhoneSession(
                        userId = userId,
                        secret = otp.trim().toString(),
                    )
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                        finishAffinity()
                    }
                }
            }
        }
    }
}