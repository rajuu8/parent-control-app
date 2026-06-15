package com.parentcontrol.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ConnectActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("parent_control", MODE_PRIVATE)

        // Already connected hai to seedha main activity
        val savedCode = prefs.getString("parent_code", null)
        if (savedCode != null) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_connect)

        val etCode = findViewById<EditText>(R.id.etCode)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val tvError = findViewById<TextView>(R.id.tvError)

        btnConnect.setOnClickListener {
            val code = etCode.text.toString().trim()
            if (code.length != 4) {
                tvError.text = "❌ 4 digit code daalo!"
                return@setOnClickListener
            }
            validateCode(code, tvError)
        }
    }

    private fun validateCode(code: String, tvError: TextView) {
        Thread {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://overflowing-perception-production-17b2.up.railway.app/parent/validate/$code")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                val json = org.json.JSONObject(body)

                runOnUiThread {
                    if (json.getBoolean("valid")) {
                        // Code save karo
                        getSharedPreferences("parent_control", MODE_PRIVATE)
                            .edit()
                            .putString("parent_code", code)
                            .apply()
                        goToMain()
                    } else {
                        tvError.text = "❌ Galat code! Parent se sahi code lo"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { tvError.text = "❌ Connection error!" }
            }
        }.start()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
