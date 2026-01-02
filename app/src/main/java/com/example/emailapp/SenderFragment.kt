package com.example.emailapp

import android.os.Bundle
import android.util.Base64
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher

class SenderFragment : Fragment(R.layout.fragment_sender) {

    // This is the "Shared Memory" both tabs talk to
    private val viewModel: CryptoViewModel by activityViewModels()

    // UI Elements
    private lateinit var msg: EditText
    private lateinit var s_result: EditText
    private lateinit var sendButton: Button

    // Keys (From Bottom Sheet)
    private lateinit var s_pubKey: EditText
    private lateinit var s_privKey: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Initialize Main Screen Elements
        msg = view.findViewById(R.id.msg)
        s_result = view.findViewById(R.id.s_result)
        sendButton = view.findViewById(R.id.send_button)
        val btnOpenSecurity = view.findViewById<Button>(R.id.btn_open_security)

        // 2. Setup Bottom Sheet (Security Keys)
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.layout_security_sheet, null)
        bottomSheetDialog.setContentView(sheetView)

        s_pubKey = sheetView.findViewById(R.id.s_pubKey)
        s_privKey = sheetView.findViewById(R.id.s_privKey)
        val generateBtn = sheetView.findViewById<Button>(R.id.generate_button)
        val sendKeyBtn = sheetView.findViewById<Button>(R.id.send_key)

        // --- BUTTON LOGIC ---

        btnOpenSecurity.setOnClickListener {
            bottomSheetDialog.show()
        }

        generateBtn.setOnClickListener {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val pair = keyGen.generateKeyPair()

            val pub = Base64.encodeToString(pair.public.encoded, Base64.DEFAULT)
            val priv = Base64.encodeToString(pair.private.encoded, Base64.DEFAULT)

            s_pubKey.setText(pub)
            s_privKey.setText(priv)

            // Automatically update the "App Brain" with the new key
            viewModel.sharedPublicKey.value = pub
        }

        sendKeyBtn.setOnClickListener {
            viewModel.sharedPublicKey.value = s_pubKey.text.toString()
            Toast.makeText(context, "Public Key shared!", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }

        view.findViewById<Button>(R.id.hash_button).setOnClickListener {
            val md = MessageDigest.getInstance("MD5")
            val hash = md.digest(msg.text.toString().toByteArray()).joinToString("") { "%02x".format(it) }
            s_result.setText(hash)
            // No need to "Send" yet, but we update the message in the brain
            viewModel.sharedMessage.value = msg.text.toString()
        }

        view.findViewById<Button>(R.id.encrypt_button).setOnClickListener {
            try {
                val privBytes = Base64.decode(s_privKey.text.toString(), Base64.DEFAULT)
                val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(privBytes))
                val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val encrypted = cipher.doFinal(s_result.text.toString().toByteArray())
                val sig = Base64.encodeToString(encrypted, Base64.DEFAULT)

                s_result.setText(sig)
                // Update the brain with the signature
                viewModel.sharedSignature.value = sig
            } catch (e: Exception) {
                Toast.makeText(context, "Error: Setup keys first!", Toast.LENGTH_SHORT).show()
            }
        }

        // THE MAIN SEND BUTTON
        sendButton.setOnClickListener {
            // Save everything to the brain at once
            viewModel.sharedMessage.value = msg.text.toString()
            viewModel.sharedSignature.value = s_result.text.toString()

            Toast.makeText(context, "Data ready! Tap the Receiver tab.", Toast.LENGTH_SHORT).show()
        }
    }
}