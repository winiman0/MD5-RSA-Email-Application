package com.example.emailapp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

class ReceiverActivity : AppCompatActivity() {
    // These must match the XML types exactly
    private lateinit var receivedMessage: TextView  // In XML this is a TextView
    private lateinit var receivedEncrypted: EditText
    private lateinit var pubKeyInput: EditText
    private lateinit var decryptedHashView: EditText
    private lateinit var computedHashView: EditText
    private lateinit var resultView: TextView
    private lateinit var statusCard: MaterialCardView // Added this

    private lateinit var decryptButton: Button
    private lateinit var hashButton: Button
    private lateinit var compareButton: Button

    private var senderPublicKey: PublicKey? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_receiver)

        // 1. Initialize UI Elements (Names updated to match your XML)
        receivedMessage = findViewById(R.id.receivedMessage)
        receivedEncrypted = findViewById(R.id.receivedEncrypted)
        pubKeyInput = findViewById(R.id.pubKeyInput)
        decryptedHashView = findViewById(R.id.decryptedHashView)
        computedHashView = findViewById(R.id.computedHashView)
        resultView = findViewById(R.id.resultView)
        statusCard = findViewById(R.id.statusCard)

        decryptButton = findViewById(R.id.btn_decrypt_sig)
        hashButton = findViewById(R.id.btn_rehash)
        compareButton = findViewById(R.id.verify_button)

        loadIntentData()

        decryptButton.setOnClickListener { decryptSignature() }
        hashButton.setOnClickListener { hashMessage() }
        compareButton.setOnClickListener { compareHashes() }

        // 2. Handle Insets (Make sure XML root has id="main")
        val mainLayout = findViewById<View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }

    private fun loadIntentData() {
        val sharedPrefs = getSharedPreferences("CryptoAppPrefs", Context.MODE_PRIVATE)
        val phase = intent.getStringExtra("PHASE")

        when (phase) {
            "KEY_ONLY" -> {
                val publicKeyStr = intent.getStringExtra("PUBLIC_KEY")
                if (!publicKeyStr.isNullOrEmpty()) {
                    sharedPrefs.edit().putString("STORED_PUBLIC_KEY", publicKeyStr).apply()
                    setupPublicKey(publicKeyStr)
                    pubKeyInput.setText(publicKeyStr)
                    resultView.text = "Status: Public Key Received"
                }
            }
            "MESSAGE" -> {
                receivedMessage.text = intent.getStringExtra("MESSAGE")
                receivedEncrypted.setText(intent.getStringExtra("SIGNATURE"))

                val savedKey = sharedPrefs.getString("STORED_PUBLIC_KEY", null)
                if (!savedKey.isNullOrEmpty()) {
                    setupPublicKey(savedKey)
                    pubKeyInput.setText(savedKey)
                } else {
                    Toast.makeText(this, "No stored public key found!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupPublicKey(keyStr: String) {
        try {
            val keyBytes = Base64.decode(keyStr, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(keyBytes)
            senderPublicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun decryptSignature() {
        if (senderPublicKey == null) {
            Toast.makeText(this, "No public key stored!", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, senderPublicKey)

            val encryptedBytes = Base64.decode(receivedEncrypted.text.toString(), Base64.DEFAULT)
            val decryptedHash = String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)

            decryptedHashView.setText(decryptedHash)
            Toast.makeText(this, "Signature decrypted", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            decryptedHashView.setText("Error: Decryption failed")
        }
    }

    private fun hashMessage() {
        val messageText = receivedMessage.text.toString()
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(messageText.toByteArray(Charsets.UTF_8))
        val computedHash = hashBytes.joinToString("") { "%02x".format(it) }

        computedHashView.setText(computedHash)
        Toast.makeText(this, "Message hashed", Toast.LENGTH_SHORT).show()
    }

    private fun compareHashes() {
        val h1 = decryptedHashView.text.toString()
        val h2 = computedHashView.text.toString()

        if (h1.isEmpty() || h2.isEmpty()) {
            resultView.text = "Status: Error (Missing data)"
            return
        }

        if (h1 == h2) {
            resultView.text = "Status: AUTHENTIC"
            resultView.setTextColor(Color.parseColor("#2E7D32")) // Green
            statusCard.setCardBackgroundColor(Color.parseColor("#C8E6C9"))
        } else {
            resultView.text = "Status: MODIFIED / TAMPERED"
            resultView.setTextColor(Color.parseColor("#C62828")) // Red
            statusCard.setCardBackgroundColor(Color.parseColor("#FFCDD2"))
        }
    }
}