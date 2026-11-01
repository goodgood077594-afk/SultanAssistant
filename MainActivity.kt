package com.sultan.assistant

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnMic: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvResponse: TextView
    
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private var isListening = false

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // إنشاء واجهة بسيطة برمجياً لتسهيل العمل من الهاتف مباشرة دون الحاجة لملفات XML متعددة
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundColor(android.graphics.Color.parseColor("#121212"))
        }

        val tvTitle = TextView(this).apply {
            text = "SULTAN"
            textSize = 32f
            setTextColor(android.graphics.Color.parseColor("#D4AF37"))
            gravity = android.view.Gravity.CENTER
        }

        tvStatus = TextView(this).apply {
            text = "مرحباً، أنا سلطان. اضغط على الميكروفون للتحدث"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#B0BEC5"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }

        btnMic = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setBackgroundColor(android.graphics.Color.parseColor("#D4AF37"))
            setPadding(30, 30, 30, 30)
        }

        tvResponse = TextView(this).apply {
            text = ""
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 60, 0, 0)
        }

        layout.addView(tvTitle)
        layout.addView(tvStatus)
        layout.addView(btnMic)
        layout.addView(tvResponse)
        setContentView(layout)

        checkPermissions()
        initTextToSpeech()
        initSpeechRecognizer()

        btnMic.setOnClickListener {
            if (!isListening) {
                startListening()
            } else {
                stopListening()
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CALL_PHONE, android.Manifest.permission.SET_ALARM),
                REQUEST_CODE_PERMISSION
            )
        }
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale("ar"))
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    tvStatus.text = "أنا أسمعك..."
                    isListening = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    tvStatus.text = "جاري المعالجة..."
                    isListening = false
                }
                override fun onError(error: Int) {
                    tvStatus.text = "لم أسمعك جيدا، اضغط للتحدث"
                    isListening = false
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        tvResponse.text = "أنت: $spokenText"
                        processCommand(spokenText)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-DZ")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تفضل، أنا أسمعك...")
        }
        speechRecognizer.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer.stopListening()
        isListening = false
        tvStatus.text = "مرحباً، أنا سلطان."
    }

    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun processCommand(command: String) {
        val reply: String = if (command.contains("السلام عليكم") || command.contains("سلطان")) {
            "وعليكم السلام، نعم أنا سلطان، تفضل أسمعك."
        } else if (command.contains("الوقت") || command.contains("الساعة")) {
            "الوقت الحالي هو " + Calendar.getInstance().time.toString()
        } else {
            "لقد قلت: $command. أنا أسمعك بوضوح."
        }
        
        tvResponse.append("\nسلطان: $reply")
        speak(reply)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
