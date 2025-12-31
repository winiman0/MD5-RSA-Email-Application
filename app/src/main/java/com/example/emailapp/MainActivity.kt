package com.example.emailapp

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher

class MainActivity : AppCompatActivity() {

    // Main UI elements
    private lateinit var msg: EditText
    private lateinit var s_result: EditText
    private lateinit var sendTo: EditText
    private lateinit var cc: EditText
    private lateinit var subject: EditText
    private lateinit var hashButton: Button
    private lateinit var encryptButton: Button
    private lateinit var sendButton: Button

    // Bottom Sheet UI elements (These live in the sheet)
    private lateinit var s_pubKey: EditText
    private lateinit var s_privKey: EditText
    private lateinit var generateButton: Button
    private lateinit var send_key: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1. Connect Main Screen Views
        msg = findViewById(R.id.msg)
        s_result = findViewById(R.id.s_result)
        sendTo = findViewById(R.id.sendTo)
        cc = findViewById(R.id.cc)
        subject = findViewById(R.id.subject)
        hashButton = findViewById(R.id.hash_button)
        encryptButton = findViewById(R.id.encrypt_button)
        sendButton = findViewById(R.id.send_button)

        // 2. Setup the Bottom Sheet
        val btnOpenSecurity = findViewById<Button>(R.id.btn_open_security)
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.layout_security_sheet, null)
        bottomSheetDialog.setContentView(sheetView)

        // 3. Connect Bottom Sheet Views (CRITICAL: use sheetView.findViewById)
        s_pubKey = sheetView.findViewById(R.id.s_pubKey)
        s_privKey = sheetView.findViewById(R.id.s_privKey)
        generateButton = sheetView.findViewById(R.id.generate_button)
        send_key = sheetView.findViewById(R.id.send_key)

        // 4. Listeners
        btnOpenSecurity.setOnClickListener {
            bottomSheetDialog.show()
        }

        // --- KEY LOGIC (Inside Sheet) ---
        generateButton.setOnClickListener {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val pair = keyGen.generateKeyPair()

            s_pubKey.setText(Base64.encodeToString(pair.public.encoded, Base64.DEFAULT))
            s_privKey.setText(Base64.encodeToString(pair.private.encoded, Base64.DEFAULT))

            Toast.makeText(this, "Keys generated!", Toast.LENGTH_SHORT).show()
        }

        send_key.setOnClickListener {
            val intent = Intent(this, ReceiverActivity::class.java)
            intent.putExtra("PUBLIC_KEY", s_pubKey.text.toString())
            intent.putExtra("PHASE", "KEY_ONLY")
            startActivity(intent)
            bottomSheetDialog.dismiss() // Auto-close sheet after sending
        }

        // --- CRYPTO LOGIC (Main Screen) ---
        hashButton.setOnClickListener {
            val input = msg.text.toString()
            if (input.length != 9 || !input.all { it.isDigit() }) {
                Toast.makeText(this, "Message must be exactly 9 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val md = MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(input.toByteArray(Charsets.UTF_8))
            val hashString = hashBytes.joinToString("") { "%02x".format(it) }
            s_result.setText(hashString)
        }

        encryptButton.setOnClickListener {
            try {
                val privateKeyBytes = Base64.decode(s_privKey.text.toString(), Base64.DEFAULT)
                val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
                val kf = KeyFactory.getInstance("RSA")
                val privateKey = kf.generatePrivate(keySpec)

                val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                cipher.init(Cipher.ENCRYPT_MODE, privateKey)

                val encryptedBytes = cipher.doFinal(s_result.text.toString().toByteArray(Charsets.UTF_8))
                s_result.setText(Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
            } catch (e: Exception) {
                Toast.makeText(this, "Error: Ensure keys are generated first!", Toast.LENGTH_SHORT).show()
            }
        }

        sendButton.setOnClickListener {
            if (msg.text.length != 9) {
                Toast.makeText(this, "Please ensure M is 9 digits before sending", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, ReceiverActivity::class.java).apply {
                putExtra("MESSAGE", msg.text.toString())
                putExtra("SIGNATURE", s_result.text.toString())
                putExtra("PHASE", "MESSAGE")
            }
            startActivity(intent)
        }

        // Handle Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}