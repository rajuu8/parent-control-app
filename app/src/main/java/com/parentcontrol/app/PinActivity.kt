package com.parentcontrol.app

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.*

class PinActivity : Activity() {

    private val CORRECT_PIN = "1234" // Parent ka PIN — baad mein change kar sakte ho

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val pinInput = findViewById<EditText>(R.id.pinInput)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val tvError = findViewById<TextView>(R.id.tvError)

        btnSubmit.setOnClickListener {
            val enteredPin = pinInput.text.toString()
            if (enteredPin == CORRECT_PIN) {
                // Sahi PIN — Device Admin deactivate karo
                val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val adminComponent = ComponentName(this, AdminReceiver::class.java)
                dpm.removeActiveAdmin(adminComponent)
                Toast.makeText(this, "PIN sahi! Uninstall karo", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                tvError.text = "❌ Galat PIN! Try again"
                pinInput.text.clear()
            }
        }
    }

    // Back button block karo
    override fun onBackPressed() {
        Toast.makeText(this, "PIN enter karo!", Toast.LENGTH_SHORT).show()
    }
}
