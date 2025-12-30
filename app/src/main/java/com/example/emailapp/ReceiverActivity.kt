package com.example.emailapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.*
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import android.util.Base64
import java.security.MessageDigest
import android.content.Context
import android.content.SharedPreferences


class ReceiverActivity : AppCompatActivity() {
    private lateinit var receivedMessage: EditText
    private lateinit var receivedEncrypted: EditText
    private lateinit var pubKeyInput: EditText
    private lateinit var decryptedResult: EditText
    private lateinit var computedResult: EditText
    private lateinit var resultView: TextView

    private lateinit var decryptButton: Button
    private lateinit var hashButton: Button
    private lateinit var compareButton: Button

    private var decryptedHash: String = ""
    private var computedHash: String = ""
    private var senderPublicKey: PublicKey? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_receiver)

        // Initialize UI Elements
        receivedMessage = findViewById(R.id.receivedMessage)
        receivedEncrypted = findViewById(R.id.receivedEncrypted)
        pubKeyInput = findViewById(R.id.pubKeyInput)
        decryptedResult = findViewById(R.id.decryptedResult)
        computedResult = findViewById(R.id.computedResult)
        resultView = findViewById(R.id.resultView)

        decryptButton = findViewById(R.id.encrypt_button)
        hashButton = findViewById(R.id.hash_button)
        compareButton = findViewById(R.id.compare_button)

        loadIntentData()

        decryptButton.setOnClickListener { decryptSignature() }
        hashButton.setOnClickListener { hashMessage() }
        compareButton.setOnClickListener { compareHashes() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadIntentData() {
        val sharedPrefs = getSharedPreferences("CryptoAppPrefs", Context.MODE_PRIVATE)

        when (intent.getStringExtra("PHASE")) {
            "KEY_ONLY" -> {
                val publicKeyStr = intent.getStringExtra("PUBLIC_KEY")
                if (!publicKeyStr.isNullOrEmpty()) {
                    // 1. Save the key to SharedPreferences
                    sharedPrefs.edit().putString("STORED_PUBLIC_KEY", publicKeyStr).apply()

                    // 2. Load it into memory
                    setupPublicKey(publicKeyStr)

                    pubKeyInput.setText(publicKeyStr)
                    Toast.makeText(this, "Public key stored!", Toast.LENGTH_SHORT).show()
                }
            }
            "MESSAGE" -> {
                receivedMessage.setText(intent.getStringExtra("MESSAGE"))
                receivedEncrypted.setText(intent.getStringExtra("SIGNATURE"))

                // 3. Retrieve the key from SharedPreferences if it's not in memory
                val savedKey = sharedPrefs.getString("STORED_PUBLIC_KEY", null)
                if (!savedKey.isNullOrEmpty()) {
                    setupPublicKey(savedKey)
                    pubKeyInput.setText(savedKey)
                } else {
                    Toast.makeText(this, "Warning: No stored public key found!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Helper function to convert String to PublicKey object
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
            decryptedHash = String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)

            // SHOW the result in the EditText result field
            decryptedResult.setText(decryptedHash)
            Toast.makeText(this, "Signature decrypted", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            decryptedResult.setText("Error: Decryption failed")
        }
    }

    private fun hashMessage() {
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(receivedMessage.text.toString().toByteArray(Charsets.UTF_8))
        computedHash = hashBytes.joinToString("") { "%02x".format(it) }

        // SHOW the result in the EditText result field
        computedResult.setText(computedHash)
        Toast.makeText(this, "Message hashed", Toast.LENGTH_SHORT).show()
    }

    private fun compareHashes() {
        if (decryptedHash.isEmpty() || computedHash.isEmpty()) {
            resultView.text = "Status: Error (Missing data)"
            return
        }

        if (decryptedHash == computedHash) {
            resultView.text = "Status: AUTHENTIC"
            resultView.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        } else {
            resultView.text = "Status: MODIFIED"
            resultView.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        }
    }
}
