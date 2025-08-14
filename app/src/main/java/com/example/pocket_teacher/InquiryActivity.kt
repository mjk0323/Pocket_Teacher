package com.example.pocket_teacher

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class InquiryActivity : AppCompatActivity() {

    // UI refs
    private lateinit var tilSubject: TextInputLayout
    private lateinit var tilBody: TextInputLayout
    private lateinit var etSubject: TextInputEditText
    private lateinit var etBody: TextInputEditText
    private lateinit var ivPreview: ImageView
    private lateinit var btnAttach: MaterialButton
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnClose: ImageView

    private var imageUri: Uri? = null

    // 갤러리 열기 콜백
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        imageUri = uri
        ivPreview.setImageURI(uri) // Glide 없이도 미리보기
        ivPreview.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inquiry) // 너의 XML 레이아웃 이름

        // 시스템 인셋 적용 (루트 id: inquiry_root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.inquiry_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // 바인딩
        tilSubject = findViewById(R.id.tilSubject)
        tilBody = findViewById(R.id.tilBody)
        etSubject = findViewById(R.id.etSubject)
        etBody = findViewById(R.id.etBody)
        ivPreview = findViewById(R.id.ivPreview)
        btnAttach = findViewById(R.id.btnAttach)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnClose = findViewById(R.id.btn_close)

        // ── 뒤로가기 모드: 공통 툴바의 왼쪽 아이콘을 뒤로가기 화살표로 세팅
        btnClose.visibility = View.VISIBLE
        btnClose.setImageResource(R.drawable.ic_arrow_back) // 화살표 아이콘 리소스 필요
        btnClose.contentDescription = "뒤로가기"
        btnClose.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 사진 첨부
        btnAttach.setOnClickListener { pickImage.launch("image/*") }

        // 등록
        btnSubmit.setOnClickListener { submit() }
    }

    private fun submit() {
        val subject = etSubject.text?.toString()?.trim().orEmpty()
        val body = etBody.text?.toString()?.trim().orEmpty()

        var valid = true
        if (subject.isBlank()) {
            tilSubject.error = "제목을 입력해주세요"
            valid = false
        } else tilSubject.error = null

        if (body.isBlank()) {
            tilBody.error = "내용을 입력해주세요"
            valid = false
        } else tilBody.error = null

        if (!valid) return

        // TODO: Retrofit 등으로 서버 전송 (subject, body, imageUri)
        Toast.makeText(this, "문의가 등록되었습니다.", Toast.LENGTH_SHORT).show()
        finish()
    }
}
