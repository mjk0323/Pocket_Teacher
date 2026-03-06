package com.example.pocket_teacher

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.ai.client.generativeai.GenerativeModel
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale


class VoiceAssistantActivity : AppCompatActivity() {

    private var isListening = false
    private var pulseAnimator: ValueAnimator? = null

    // 버튼 및 레이아웃
    private lateinit var micBtn: FrameLayout
    private lateinit var tvPrompt: TextView
    private lateinit var dot: View
    private lateinit var guideLine: View
    private lateinit var backBtn: ImageButton
    private lateinit var tvResponse: TextView

    private lateinit var mRecognizer: SpeechRecognizer

    // Gemini API 가져오기
    val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_voice_assistant)

        // 상태바/네비바 패딩
        findViewById<View>(R.id.voice_root)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(v.paddingLeft, bars.top, v.paddingRight, bars.bottom)
                insets
            }
        }

        // 뷰 바인딩
        micBtn = findViewById(R.id.btnMic)
        tvPrompt = findViewById(R.id.tvPrompt)
        dot = findViewById(R.id.dot)
        guideLine = findViewById(R.id.guideLine)
        backBtn = findViewById(R.id.button_back)
        tvResponse = findViewById(R.id.tvResponse)

        // 처음 화면: 기본 안내 문구, 점/선 표시
        tvPrompt.text = getString(R.string.voice)
        tvPrompt.isVisible = true
        dot.isVisible = true
        guideLine.isVisible = true
        tvResponse.isVisible = false

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        }

        // 마이크 권한 체크
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }

        // 마이크 버튼 클릭 시
        micBtn.setOnClickListener {
            if (isListening) finishListening() else startListening()
        }

        // 뒤로가기 버튼 클릭 시
        backBtn.setOnClickListener {
            if (isListening) finishListening()
            onBackPressedDispatcher.onBackPressed()
        }
    }

//    듣기 시작: 점/선 숨김 + 펄스 애니메이션
    private fun startListening() {
        if (isListening) return
        isListening = true

        // 텍스트/접근성
        tvPrompt.text = getString(R.string.listening)
        tvPrompt.isVisible = true
        micBtn.contentDescription = getString(R.string.listening)

        // 점/선 숨김
        dot.isVisible = false
        guideLine.isVisible = false

        // 펄스 애니메이션(1f <-> 1.12f)
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.12f).apply {
            duration = 380
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { anim ->
                val scale = anim.animatedValue as Float
                micBtn.scaleX = scale
                micBtn.scaleY = scale
            }
            start()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "말씀하세요...")
        }

        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        mRecognizer.setRecognitionListener(object:RecognitionListener{
            override fun onResults(bundle: Bundle?){
                // 음성 인식하여 텍스트로 변환 후 Gemini에게 전달
                val result = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = result?.get(0)?:return
                finishListening()
                askGemini(recognizedText)
            }
            override fun onError(p0: Int) { finishListening() }
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() { mRecognizer.stopListening() }
            override fun onEvent(p0: Int, p1: Bundle?) {}
            override fun onPartialResults(p0: Bundle?) {}
            override fun onReadyForSpeech(p0: Bundle?) {}
            override fun onRmsChanged(p0: Float) {}
        })
        mRecognizer.startListening(intent)
    }

    // 음성으로 인식된 텍스트를 받음
    private fun askGemini(prompt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = generativeModel.generateContent(prompt).text
            withContext(Dispatchers.Main) {
                tvResponse.text = result
                tvResponse.isVisible = true
                tvPrompt.isVisible = false
            }
        }
    }

    // 듣기 종료: 텍스트 원래대로, 점/선 다시 표시 + 애니메이션 정리
    private fun finishListening() {
        if (!isListening) return
        isListening = false

        pulseAnimator?.cancel()
        pulseAnimator = null
        micBtn.scaleX = 1f
        micBtn.scaleY = 1f

        tvPrompt.text = getString(R.string.voice) // "음성 입력 안내"
        tvPrompt.isVisible = true

        dot.isVisible = true
        guideLine.isVisible = true

        // 접근성 문구 원래대로
        micBtn.contentDescription = getString(R.string.voice)
    }

    override fun onPause() {
        super.onPause()
        if (isListening) finishListening()
    }
}
