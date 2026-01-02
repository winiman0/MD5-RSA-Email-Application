package com.example.emailapp

import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.card.MaterialCardView
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

class ReceiverFragment : Fragment(R.layout.fragment_receiver) {

    private val viewModel: CryptoViewModel by activityViewModels()

    private lateinit var receivedMessage: EditText
    private lateinit var receivedEncrypted: EditText
    private lateinit var pubKeyInput: EditText
    private lateinit var decryptedHashView: EditText
    private lateinit var computedHashView: EditText
    private lateinit var resultView: TextView
    private lateinit var statusCard: MaterialCardView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Connect UI
        receivedMessage = view.findViewById(R.id.receivedMessage)
        receivedEncrypted = view.findViewById(R.id.receivedEncrypted)
        pubKeyInput = view.findViewById(R.id.pubKeyInput)
        decryptedHashView = view.findViewById(R.id.decryptedHashView)
        computedHashView = view.findViewById(R.id.computedHashView)
        resultView = view.findViewById(R.id.resultView)
        statusCard = view.findViewById(R.id.statusCard)

        // --- THE MAGIC: AUTOFILL LOGIC ---
        // If the Sender tab has data, fill it. BUT the user can still paste manually!
        viewModel.sharedMessage.observe(viewLifecycleOwner) { if (it.isNotEmpty()) receivedMessage.setText(it) }
        viewModel.sharedSignature.observe(viewLifecycleOwner) { if (it.isNotEmpty()) receivedEncrypted.setText(it) }
        viewModel.sharedPublicKey.observe(viewLifecycleOwner) { if (it.isNotEmpty()) pubKeyInput.setText(it) }

        view.findViewById<Button>(R.id.btn_rehash).setOnClickListener {
            val md = MessageDigest.getInstance("MD5")
            val hash = md.digest(receivedMessage.text.toString().toByteArray()).joinToString("") { "%02x".format(it) }
            computedHashView.setText(hash)
        }

        view.findViewById<Button>(R.id.btn_decrypt_sig).setOnClickListener {
            try {
                val pubBytes = Base64.decode(pubKeyInput.text.toString(), Base64.DEFAULT)
                val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(pubBytes))
                val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                cipher.init(Cipher.DECRYPT_MODE, key)
                val dec = String(cipher.doFinal(Base64.decode(receivedEncrypted.text.toString(), Base64.DEFAULT)))
                decryptedHashView.setText(dec)
            } catch (e: Exception) { Toast.makeText(context, "Decryption Failed", Toast.LENGTH_SHORT).show() }
        }

        view.findViewById<Button>(R.id.verify_button).setOnClickListener {
            val h1 = decryptedHashView.text.toString()
            val h2 = computedHashView.text.toString()
            if (h1 == h2 && h1.isNotEmpty()) {
                resultView.text = "Status: AUTHENTIC"
                statusCard.setCardBackgroundColor(Color.parseColor("#C8E6C9"))
            } else {
                resultView.text = "Status: TAMPERED"
                statusCard.setCardBackgroundColor(Color.parseColor("#FFCDD2"))
            }
        }
    }
}