package com.example.emailapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.util.Base64
import android.widget.Toast

import java.security.KeyPairGenerator
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.PKCS8EncodedKeySpec

import javax.crypto.Cipher



class MainActivity : AppCompatActivity() {

    // Declare UI elements
    private lateinit var msg: EditText
    private lateinit var s_pubKey: EditText
    private lateinit var s_privKey: EditText
    private lateinit var s_result: EditText
    private lateinit var sendTo: EditText
    private lateinit var cc: EditText
    private lateinit var subject: EditText

    private lateinit var hashButton: Button
    private lateinit var generateButton: Button
    private lateinit var encryptButton: Button
    private lateinit var sendButton: Button
    private lateinit var send_key: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Connect XML views to Kotlin
        msg = findViewById(R.id.msg)
        s_pubKey = findViewById(R.id.s_pubKey)
        s_privKey = findViewById(R.id.s_privKey)
        s_result = findViewById(R.id.s_result)
        sendTo = findViewById(R.id.sendTo)
        cc = findViewById(R.id.cc)
        subject = findViewById(R.id.subject)

        hashButton = findViewById(R.id.hash_button)
        generateButton = findViewById(R.id.generate_button)
        send_key = findViewById(R.id.send_key)
        encryptButton = findViewById(R.id.encrypt_button)
        sendButton = findViewById(R.id.send_button)

        //RSA Key Generation
        generateButton.setOnClickListener {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val pair = keyGen.generateKeyPair()
            val privateKey = pair.private
            val publicKey = pair.public

            s_pubKey.setText(Base64.encodeToString(publicKey.encoded, Base64.DEFAULT))
            s_privKey.setText(Base64.encodeToString(privateKey.encoded, Base64.DEFAULT))

            Toast.makeText(this, "Keys generated!", Toast.LENGTH_SHORT).show()
        }

        send_key.setOnClickListener {
            val intent = Intent(this, ReceiverActivity::class.java)
            intent.putExtra("PUBLIC_KEY", s_pubKey.text.toString())
            intent.putExtra("PHASE", "KEY_ONLY")
            startActivity(intent)

            Toast.makeText(this, "Public Key sent! Now write your message.", Toast.LENGTH_SHORT).show()
        }

        //MD5 Implementation
        hashButton.setOnClickListener {
            val md = MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(msg.text.toString().toByteArray(Charsets.UTF_8))
            val hashString = hashBytes.joinToString("") { "%02x".format(it) }
            s_result.setText(hashString)
        }

        encryptButton.setOnClickListener {
            val privateKeyBytes = Base64.decode(s_privKey.text.toString(), Base64.DEFAULT)
            val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val kf = KeyFactory.getInstance("RSA")
            val privateKey = kf.generatePrivate(keySpec)

            //Encrypting hash (Digital Signature)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, privateKey)

            val encryptedBytes = cipher.doFinal(s_result.text.toString().toByteArray(Charsets.UTF_8))
            s_result.setText(Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
        }

        //Sending plaintext and encrypted hash
        sendButton.setOnClickListener {
            val intent = Intent(this, ReceiverActivity::class.java)
            intent.putExtra("MESSAGE", msg.text.toString())
            intent.putExtra("SIGNATURE", s_result.text.toString())
            intent.putExtra("PHASE", "MESSAGE")
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}