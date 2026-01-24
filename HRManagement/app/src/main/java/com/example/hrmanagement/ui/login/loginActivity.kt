package com.example.hrmanagement.ui.login
import com.example.hrmanagement.util.FcmTokenStore
import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hrmanagement.R
import com.example.hrmanagement.data.ForgotPasswordRequest
import com.example.hrmanagement.data.LoginRequest
import com.example.hrmanagement.data.ResetPasswordRequest
import com.example.hrmanagement.data.VerifyResetCodeRequest
import com.example.hrmanagement.network.RetrofitClient
import com.example.hrmanagement.ui.dashboard.DashboardActivity
import com.example.hrmanagement.util.SessionManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*


class LoginActivity : AppCompatActivity() {

    // ---- Session manager (for JWT token) ----
    private lateinit var sessionManager: SessionManager

    // ---- Login views ----
    private lateinit var emailField: TextInputEditText
    private lateinit var passwordField: TextInputEditText
    private lateinit var loginBtn: Button
    private lateinit var forgotPassword: TextView
    private lateinit var viewPwd: ImageView
    private lateinit var loginRoot: View

    // ---- Forgot password overlay views ----
    private lateinit var forgotOverlay: View
    private lateinit var forgotFlipper: ViewFlipper
    private lateinit var forgotCard: View

    private lateinit var fpEmailLayout: TextInputLayout
    private lateinit var fpEmailField: TextInputEditText

    // 6 OTP boxes
    private lateinit var otpBox1: EditText
    private lateinit var otpBox2: EditText
    private lateinit var otpBox3: EditText
    private lateinit var otpBox4: EditText
    private lateinit var otpBox5: EditText
    private lateinit var otpBox6: EditText

    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var newPasswordField: TextInputEditText
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var confirmPasswordField: TextInputEditText

    private lateinit var btnSendCode: Button
    private lateinit var btnSubmitCode: Button
    private lateinit var btnSubmitNewPassword: Button
    private lateinit var btnGoToLogin: Button
    private lateinit var resendText: TextView
    private lateinit var forgotProgress: ProgressBar

    // Header texts inside the card
    private lateinit var forgotTitle: TextView
    private lateinit var forgotSubtitle: TextView

    // Resend timer
    private var resendTimer: CountDownTimer? = null

    // Prevent multiple send-clicks
    private var isSendingCode: Boolean = false

    /**
     * Permission request for POST_NOTIFICATIONS (Android 13+).
     * We register this as an Activity Result; it must be a property (registered before lifecycle start).
     */
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                // User denied; optional: show a rationale or remember the choice.
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Request Android 13+ notification permission (if applicable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val has = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (has != PackageManager.PERMISSION_GRANTED) {
                // Optionally show a rationale here if you want a custom dialog (not required).
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // ---------- SESSION MANAGER ----------
        sessionManager = SessionManager(this)

        // ---------- LOGIN UI ----------
        loginRoot = findViewById(R.id.loginRoot)
        emailField = findViewById(R.id.emailField)
        passwordField = findViewById(R.id.passwordField)
        loginBtn = findViewById(R.id.loginButton)
        forgotPassword = findViewById(R.id.forgotPassword)
        viewPwd = findViewById(R.id.viewPwd)

        // password eye toggle
        var isPasswordVisible = false
        viewPwd.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            passwordField.inputType =
                if (isPasswordVisible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            viewPwd.setImageResource(if (isPasswordVisible) R.drawable.view else R.drawable.hidden)
            passwordField.setSelection(passwordField.text?.length ?: 0)
        }

        loginBtn.setOnClickListener { performLogin() }

        // ---------- FORGOT PASSWORD OVERLAY ----------
        forgotOverlay = findViewById(R.id.forgotOverlay)
        forgotFlipper = findViewById(R.id.forgotFlipper)
        forgotCard = findViewById(R.id.forgotCard)

        fpEmailLayout = findViewById(R.id.emailInputLayout)
        fpEmailField = findViewById(R.id.forgotEmailField)

        // OTP boxes
        otpBox1 = findViewById(R.id.otpBox1)
        otpBox2 = findViewById(R.id.otpBox2)
        otpBox3 = findViewById(R.id.otpBox3)
        otpBox4 = findViewById(R.id.otpBox4)
        otpBox5 = findViewById(R.id.otpBox5)
        otpBox6 = findViewById(R.id.otpBox6)

        setupOtpMovement()

        newPasswordLayout = findViewById(R.id.newPasswordLayout)
        newPasswordField = findViewById(R.id.newPasswordField)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)
        confirmPasswordField = findViewById(R.id.confirmPasswordField)

        btnSendCode = findViewById(R.id.btnSendCode)
        btnSubmitCode = findViewById(R.id.btnSubmitCode)
        btnSubmitNewPassword = findViewById(R.id.btnSubmitNewPassword)
        btnGoToLogin = findViewById(R.id.btnGoToLogin)
        resendText = findViewById(R.id.resendText)
        forgotProgress = findViewById(R.id.forgotProgress)

        // header text views inside the card
        forgotTitle = findViewById(R.id.forgotTitle)
        forgotSubtitle = findViewById(R.id.forgotSubtitle)

        // open overlay
        forgotPassword.setOnClickListener { showForgotOverlay() }

        // close overlay on success button
        btnGoToLogin.setOnClickListener { hideForgotOverlay() }

        // tap outside the card closes overlay
        forgotOverlay.setOnClickListener { hideForgotOverlay() }
        // but taps on card should do nothing
        forgotCard.setOnClickListener { /* swallow */ }

        // Step 1: send code
        btnSendCode.setOnClickListener {
            if (isSendingCode) return@setOnClickListener  // ignore while sending

            val email = fpEmailField.text?.toString()?.trim().orEmpty()
            if (!isValidEmail(email)) {
                fpEmailLayout.error = "Please enter a valid email"
            } else {
                fpEmailLayout.error = null
                sendVerificationCode(email)
            }
        }

        // Step 2: submit OTP
        btnSubmitCode.setOnClickListener {
            val code = getOtpCode()
            if (code.length != 6) {
                Toast.makeText(this, "Enter 6 digit code", Toast.LENGTH_SHORT).show()
                shakeView(forgotCard)
            } else {
                verifyCode(code)
            }
        }

        // Step 2: resend
        resendText.setOnClickListener {
            val email = fpEmailField.text?.toString()?.trim().orEmpty()
            if (isValidEmail(email)) {
                sendVerificationCode(email)
            } else {
                Toast.makeText(this, "Enter email first", Toast.LENGTH_SHORT).show()
            }
        }

        // Step 3: submit new password
        btnSubmitNewPassword.setOnClickListener {
            val pass = newPasswordField.text?.toString().orEmpty()
            val confirm = confirmPasswordField.text?.toString().orEmpty()

            when {
                pass.length < 6 -> {
                    newPasswordLayout.error = "Minimum 6 characters"
                }
                pass != confirm -> {
                    newPasswordLayout.error = null
                    confirmPasswordLayout.error = "Passwords do not match"
                }
                else -> {
                    newPasswordLayout.error = null
                    confirmPasswordLayout.error = null
                    submitNewPassword(pass)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // OTP helpers (movement + reading)
    // -------------------------------------------------------------------------
    private fun setupOtpMovement() {
        val boxes = listOf(otpBox1, otpBox2, otpBox3, otpBox4, otpBox5, otpBox6)

        for (i in boxes.indices) {
            boxes[i].addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < boxes.size - 1) {
                        boxes[i + 1].requestFocus()
                    } else if (s?.isEmpty() == true && i > 0) {
                        boxes[i - 1].requestFocus()
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) { }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) { }
            })
        }
    }

    private fun clearOtpBoxes() {
        otpBox1.text?.clear()
        otpBox2.text?.clear()
        otpBox3.text?.clear()
        otpBox4.text?.clear()
        otpBox5.text?.clear()
        otpBox6.text?.clear()
        otpBox1.requestFocus()
    }

    private fun getOtpCode(): String =
        otpBox1.text.toString() +
                otpBox2.text.toString() +
                otpBox3.text.toString() +
                otpBox4.text.toString() +
                otpBox5.text.toString() +
                otpBox6.text.toString()

    // -------------------------------------------------------------------------
    // LOGIN
    // -------------------------------------------------------------------------
    private fun performLogin() {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email & password", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.authApi.login(
                    LoginRequest(email, password)
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {

                        // get token from response
                        val token = response.body()?.token

                        // save token if present
                        if (!token.isNullOrBlank()) {
                            sessionManager.saveAuthToken(token)
                            // keep original debug log if you want
                            // Log.d("JWT_TOKEN", token)

                            // try to extract userId from JWT payload (if token contains it)
                            val extractedUserId = extractUserIdFromJwt(token)
                            if (extractedUserId != null) {
                                sessionManager.saveUserId(extractedUserId)
                            }
                        }
                        // 🔔 REGISTER FCM TOKEN AFTER LOGIN
                        val fcmToken = FcmTokenStore.get(this@LoginActivity)

                        if (fcmToken != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    RetrofitClient.deviceApi.registerDevice(
                                        bearer = "Bearer $token",
                                        body = RetrofitClient.DeviceRegisterRequest(
                                            deviceUuid = Build.MODEL + "-" + Build.ID,
                                            fcmToken = fcmToken
                                        )
                                    )
                                    Log.d("FCM", "✅ FCM token registered after login")
                                } catch (e: Exception) {
                                    Log.e("FCM", "❌ Failed to register FCM token", e)
                                }
                            }
                        } else {
                            // 🔄 TOKEN NOT READY YET → REQUEST IT
                            FirebaseMessaging.getInstance().token
                                .addOnSuccessListener { newToken ->
                                    FcmTokenStore.save(this@LoginActivity, newToken)

                                    CoroutineScope(Dispatchers.IO).launch {
                                        RetrofitClient.deviceApi.registerDevice(
                                            bearer = "Bearer $token",
                                            body = RetrofitClient.DeviceRegisterRequest(
                                                deviceUuid = Build.MODEL + "-" + Build.ID,
                                                fcmToken = newToken
                                            )
                                        )
                                    }
                                }
                        }



                        Toast.makeText(
                            this@LoginActivity,
                            "Login Successful",
                            Toast.LENGTH_SHORT
                        ).show()

                        // go to dashboard
                        startActivity(
                            Intent(
                                this@LoginActivity,
                                DashboardActivity::class.java
                            )
                        )
                        finish()

                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            response.body()?.message ?: "Login failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Try to extract a numeric user id from a JWT token payload.
     * Looks for common claim keys: userId, user_id, id, sub.
     * Returns Int? or null if not found / parse error.
     */
    private fun extractUserIdFromJwt(token: String): Int? {
        return try {
            // JWT = header.payload.signature
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payloadB64 = parts[1]

            // base64url decode: use android.util.Base64 with URL_SAFE and NO_WRAP
            val decodedBytes = android.util.Base64.decode(payloadB64, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val payloadJson = String(decodedBytes, Charsets.UTF_8)

            // Parse JSON and try several claim names
            val jsonObj = org.json.JSONObject(payloadJson)
            val candidates = listOf("userId", "user_id", "id", "sub")
            for (key in candidates) {
                if (jsonObj.has(key)) {
                    val value = jsonObj.get(key)
                    when (value) {
                        is Number -> return value.toInt()
                        is String -> {
                            val n = value.toIntOrNull()
                            if (n != null) return n
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            // If anything goes wrong, return null (safe fallback)
            null
        }
    }

    // -------------------------------------------------------------------------
    // FORGOT PASSWORD FLOW + SLIDE ANIMATIONS
    // -------------------------------------------------------------------------

    private fun showForgotOverlay() {
        // Prepare overlay
        forgotOverlay.visibility = View.VISIBLE
        forgotOverlay.alpha = 0f

        // First set initial state then animate inside post{} so we know size
        forgotOverlay.post {
            setForgotStep(0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                loginRoot.setRenderEffect(
                    RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP)
                )
            }

            // Start from below and slide up
            forgotCard.translationY = forgotCard.height.toFloat().takeIf { it > 0f } ?: 800f

            forgotOverlay.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null)
                .start()

            forgotCard.animate()
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(null)
                .start()
        }
    }

    private fun hideForgotOverlay() {
        resendTimer?.cancel()

        val slideDistance =
            forgotCard.height.toFloat().takeIf { it > 0f } ?: 800f

        forgotOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .setListener(null)
            .start()

        forgotCard.animate()
            .translationY(slideDistance)
            .setDuration(220)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    forgotOverlay.visibility = View.GONE
                    forgotOverlay.alpha = 1f
                    forgotCard.translationY = 0f

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        loginRoot.setRenderEffect(null)
                    }
                }
            })
            .start()
    }

    private fun isValidEmail(email: String): Boolean =
        email.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    /**
     * Central place to switch step + header text.
     * step:
     * 0 = email, 1 = OTP, 2 = new password, 3 = success
     */
    private fun setForgotStep(step: Int) {
        forgotFlipper.displayedChild = step

        when (step) {
            0 -> { // Enter email
                forgotTitle.visibility = View.VISIBLE
                forgotSubtitle.visibility = View.VISIBLE
                forgotTitle.text = "Forgot Password"
                forgotSubtitle.text =
                    "A verification code will be sent to your email to reset your password."
                clearOtpBoxes()
            }
            1 -> { // Enter code
                forgotTitle.visibility = View.VISIBLE
                forgotSubtitle.visibility = View.VISIBLE
                forgotTitle.text = "Forgot Password"
                forgotSubtitle.text =
                    "A reset code has been sent to your email, check your inbox to continue."
                clearOtpBoxes()
            }
            2 -> { // Set new password
                forgotTitle.visibility = View.VISIBLE
                forgotSubtitle.visibility = View.VISIBLE
                forgotTitle.text = "Set a New Password"
                forgotSubtitle.text =
                    "Please set a new password to secure your account."
            }
            3 -> { // Success
                // Hide header + subtitle; only show success text from ViewFlipper child
                forgotTitle.visibility = View.GONE
                forgotSubtitle.visibility = View.GONE
            }
        }
        animateCardChange()
    }

    // Nice small scale animation when switching steps
    private fun animateCardChange() {
        forgotCard.scaleX = 0.97f
        forgotCard.scaleY = 0.97f
        forgotCard.alpha = 0.8f

        forgotCard.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(180)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    // ---- STEP 1: SEND OTP TO EMAIL ----
    private fun sendVerificationCode(email: String) {
        // block multiple clicks
        if (isSendingCode) return
        isSendingCode = true

        btnSendCode.isEnabled = false
        btnSendCode.alpha = 0.6f
        btnSendCode.text = "Sending..."
        forgotProgress.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.authApi.forgotPassword(
                    ForgotPasswordRequest(email)
                )

                withContext(Dispatchers.Main) {
                    forgotProgress.visibility = View.GONE
                    isSendingCode = false

                    // reset visual state
                    btnSendCode.isEnabled = true
                    btnSendCode.alpha = 1f
                    btnSendCode.text = "Send Verification Code"

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@LoginActivity,
                            response.body()!!.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        setForgotStep(1) // OTP step
                        startResendTimer()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            response.body()?.message ?: "Failed to send code",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    forgotProgress.visibility = View.GONE
                    isSendingCode = false
                    btnSendCode.isEnabled = true
                    btnSendCode.alpha = 1f
                    btnSendCode.text = "Send Verification Code"

                    Toast.makeText(
                        this@LoginActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---- RESEND TIMER (30 seconds) ----
    private fun startResendTimer() {
        resendTimer?.cancel()

        resendText.isEnabled = false
        resendText.alpha = 0.5f

        resendTimer = object : CountDownTimer(30_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                resendText.text = "Resend code in ${seconds}s"
            }

            override fun onFinish() {
                resendText.text = "Haven't received the code? Resend it."
                resendText.isEnabled = true
                resendText.alpha = 1f
            }
        }.start()
    }

    // ---- STEP 2: VERIFY OTP ----
    private fun verifyCode(code: String) {
        val email = fpEmailField.text?.toString()?.trim().orEmpty()
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Email missing or invalid", Toast.LENGTH_SHORT).show()
            shakeView(fpEmailLayout)
            return
        }

        forgotProgress.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.authApi.verifyResetCode(
                    VerifyResetCodeRequest(email, code)
                )

                withContext(Dispatchers.Main) {
                    forgotProgress.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        animateOtpSuccess()
                        setForgotStep(2) // new password step
                    } else {
                        shakeView(forgotCard)
                        Toast.makeText(
                            this@LoginActivity,
                            response.body()?.message ?: "Invalid or expired code",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    forgotProgress.visibility = View.GONE
                    shakeView(forgotCard)
                    Toast.makeText(
                        this@LoginActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---- STEP 3: SUBMIT NEW PASSWORD ----
    private fun submitNewPassword(newPassword: String) {
        val email = fpEmailField.text?.toString()?.trim().orEmpty()
        val code = getOtpCode()

        if (!isValidEmail(email) || code.length != 6) {
            Toast.makeText(this, "Email or code missing/invalid", Toast.LENGTH_SHORT).show()
            shakeView(forgotCard)
            return
        }

        forgotProgress.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.authApi.resetPassword(
                    ResetPasswordRequest(
                        email = email,
                        code = code,
                        newPassword = newPassword
                    )
                )

                withContext(Dispatchers.Main) {
                    forgotProgress.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@LoginActivity,
                            response.body()!!.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        setForgotStep(3) // success screen
                    } else {
                        shakeView(forgotCard)
                        Toast.makeText(
                            this@LoginActivity,
                            response.body()?.message ?: "Failed to reset password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    forgotProgress.visibility = View.GONE
                    shakeView(forgotCard)
                    Toast.makeText(
                        this@LoginActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // ANIMATIONS
    // -------------------------------------------------------------------------
    // ❌ Shake animation for errors
    private fun shakeView(view: View) {
        val shake = TranslateAnimation(-10f, 10f, 0f, 0f)
        shake.duration = 60
        shake.repeatMode = TranslateAnimation.REVERSE
        shake.repeatCount = 4
        view.startAnimation(shake)
    }

    // ✨ Little bounce for OTP success
    private fun animateOtpSuccess() {
        val boxes = listOf(otpBox1, otpBox2, otpBox3, otpBox4, otpBox5, otpBox6)
        boxes.forEach { v ->
            v.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(80)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .setListener(null)
                            .start()
                    }
                })
                .start()
        }
    }
}
