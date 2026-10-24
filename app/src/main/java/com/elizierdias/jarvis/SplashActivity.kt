package com.elizierdias.jarvis

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
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

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val glow = findViewById<View>(R.id.splashGlow)
        val ringOuter = findViewById<View>(R.id.splashRingOuter)
        val ringInner = findViewById<View>(R.id.splashRingInner)
        val core = findViewById<View>(R.id.splashCore)
        val title = findViewById<TextView>(R.id.splashTitle)
        val subtitle = findViewById<TextView>(R.id.splashSubtitle)

        listOf(glow, ringOuter, ringInner, core, title, subtitle).forEach {
            it.alpha = 0f
        }
        ringOuter.scaleX = 0.4f
        ringOuter.scaleY = 0.4f
        ringInner.scaleX = 0.4f
        ringInner.scaleY = 0.4f
        core.scaleX = 0.2f
        core.scaleY = 0.2f
        title.translationY = 24f

        fun scalePair(view: View, from: Float, to: Float, duration: Long) =
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, View.SCALE_X, from, to).setDuration(duration),
                    ObjectAnimator.ofFloat(view, View.SCALE_Y, from, to).setDuration(duration)
                )
            }

        val set = AnimatorSet()
        set.playTogether(
            ObjectAnimator.ofFloat(glow, View.ALPHA, 0f, 1f).setDuration(900),
            ObjectAnimator.ofFloat(ringOuter, View.ALPHA, 0f, 1f).setDuration(600),
            scalePair(ringOuter, 0.4f, 1f, 900).apply {
                interpolator = OvershootInterpolator(1.2f)
            },
            ObjectAnimator.ofFloat(ringInner, View.ALPHA, 0f, 1f).setDuration(500).apply {
                startDelay = 150
            },
            scalePair(ringInner, 0.4f, 1f, 750).apply {
                startDelay = 150
                interpolator = OvershootInterpolator(1.3f)
            },
            ObjectAnimator.ofFloat(core, View.ALPHA, 0f, 1f).setDuration(400).apply {
                startDelay = 300
            },
            scalePair(core, 0.2f, 1f, 500).apply {
                startDelay = 300
                interpolator = OvershootInterpolator(1.6f)
            },
            ObjectAnimator.ofFloat(title, View.ALPHA, 0f, 1f).setDuration(500).apply {
                startDelay = 500
            },
            ObjectAnimator.ofFloat(title, View.TRANSLATION_Y, 24f, 0f).setDuration(500).apply {
                startDelay = 500
                interpolator = DecelerateInterpolator()
            },
            ObjectAnimator.ofFloat(subtitle, View.ALPHA, 0f, 1f).setDuration(400).apply {
                startDelay = 750
            }
        )
        set.interpolator = AccelerateDecelerateInterpolator()
        set.start()

        // Pulso suave no núcleo
        Handler(Looper.getMainLooper()).postDelayed({
            ObjectAnimator.ofFloat(core, View.ALPHA, 1f, 0.55f, 1f).apply {
                duration = 900
                repeatCount = 1
                start()
            }
        }, 1100)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2400)
    }
}
