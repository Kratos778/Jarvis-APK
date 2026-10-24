package com.elizierdias.jarvis

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Ecrã completo
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val title = findViewById<TextView>(R.id.splashTitle)
        val subtitle = findViewById<TextView>(R.id.splashSubtitle)
        val ring = findViewById<View>(R.id.splashRing)

        // Estado inicial
        title.alpha = 0f
        title.scaleX = 0.6f
        title.scaleY = 0.6f
        subtitle.alpha = 0f
        ring.alpha = 0f
        ring.scaleX = 0.3f
        ring.scaleY = 0.3f

        // Animação do anel
        val ringFade = ObjectAnimator.ofFloat(ring, View.ALPHA, 0f, 1f).setDuration(700)
        val ringScaleX = ObjectAnimator.ofFloat(ring, View.SCALE_X, 0.3f, 1f).setDuration(900)
        val ringScaleY = ObjectAnimator.ofFloat(ring, View.SCALE_Y, 0.3f, 1f).setDuration(900)
        ringScaleX.interpolator = OvershootInterpolator(1.4f)
        ringScaleY.interpolator = OvershootInterpolator(1.4f)

        // Animação do título
        val titleFade = ObjectAnimator.ofFloat(title, View.ALPHA, 0f, 1f).setDuration(600)
        val titleScaleX = ObjectAnimator.ofFloat(title, View.SCALE_X, 0.6f, 1f).setDuration(700)
        val titleScaleY = ObjectAnimator.ofFloat(title, View.SCALE_Y, 0.6f, 1f).setDuration(700)
        titleScaleX.interpolator = AccelerateDecelerateInterpolator()
        titleScaleY.interpolator = AccelerateDecelerateInterpolator()

        // Subtítulo
        val subFade = ObjectAnimator.ofFloat(subtitle, View.ALPHA, 0f, 1f).setDuration(500)

        val set = AnimatorSet()
        set.playTogether(ringFade, ringScaleX, ringScaleY)
        set.play(titleFade).after(250)
        set.play(titleScaleX).after(250)
        set.play(titleScaleY).after(250)
        set.play(subFade).after(550)
        set.start()

        // Vai para o chat
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2200)
    }
}
