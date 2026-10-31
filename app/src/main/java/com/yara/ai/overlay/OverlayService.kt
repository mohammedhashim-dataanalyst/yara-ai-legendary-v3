package com.yara.ai.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.yara.ai.R
import com.yara.ai.ui.MainActivity

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: View? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val text = intent?.getStringExtra(EXTRA_TEXT)
        when {
            intent?.action == ACTION_HIDE -> removeBubble()
            text != null -> showBubble(text)
            else -> showHead()
        }
        return START_STICKY
    }

    private fun params(w: Int, h: Int, gravity: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        return WindowManager.LayoutParams(w, h, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply { this.gravity = gravity }
    }

    private fun showHead() {
        if (isYaraForeground) return
        removeBubble()
        val v = LayoutInflater.from(this).inflate(R.layout.overlay_head, null)
        v.setOnClickListener { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        v.findViewById<ImageView>(R.id.head_icon)?.setImageResource(R.mipmap.ic_launcher)
        wm.addView(v, params(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL))
        bubble = v
    }

    private fun showBubble(text: String) {
        if (isYaraForeground) return
        removeBubble()
        val v = LayoutInflater.from(this).inflate(R.layout.overlay_bubble, null)
        v.findViewById<TextView>(R.id.bubble_text)?.text = text
        v.findViewById<View>(R.id.bubble_root)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        v.findViewById<View>(R.id.bubble_close)?.setOnClickListener { removeBubble() }
        wm.addView(v, params(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END))
        bubble = v
    }

    private fun removeBubble() { bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }; bubble = null }
    override fun onDestroy() { removeBubble(); super.onDestroy() }

    companion object {
        const val EXTRA_TEXT = "text"
        const val ACTION_HIDE = "hide"
        @Volatile var isYaraForeground = false
        fun bubble(ctx: Context, text: String) {
            if (isYaraForeground) return
            ctx.startService(Intent(ctx, OverlayService::class.java).putExtra(EXTRA_TEXT, text))
        }
        fun hide(ctx: Context) { ctx.startService(Intent(ctx, OverlayService::class.java).setAction(ACTION_HIDE)) }
    }
}
