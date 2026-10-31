package com.yara.ai.ambient

import android.app.Service
import android.content.*
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.FrameLayout
import com.yara.ai.ui.MainActivity

class AmbientOverlayService: Service() {
 private lateinit var wm:WindowManager; private var root:FrameLayout?=null; private var scene:SceneView?=null
 override fun onBind(i:Intent?):IBinder?=null
 override fun onStartCommand(i:Intent?,flags:Int,id:Int):Int { if(!Settings.canDrawOverlays(this)) return START_NOT_STICKY; if(i?.action==ACTION_HIDE){remove();return START_NOT_STICKY}; show(); return START_STICKY }
 private fun show(){if(OverlayState.inApp||root!=null)return;wm=getSystemService(WINDOW_SERVICE) as WindowManager;root=FrameLayout(this);scene=SceneView(this);root!!.addView(scene,FrameLayout.LayoutParams(260,300));val lp=WindowManager.LayoutParams(260,300,if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.END;x=12;y=160};var down=0L;var sx=0f;var sy=0f;var ox=0;var oy=0;root!!.setOnTouchListener{_,e->when(e.action){MotionEvent.ACTION_DOWN->{down=SystemClock.uptimeMillis();sx=e.rawX;sy=e.rawY;ox=lp.x;oy=lp.y;true};MotionEvent.ACTION_MOVE->{lp.x=ox+(e.rawX-sx).toInt();lp.y=oy+(e.rawY-sy).toInt();try{wm.updateViewLayout(root,lp)}catch(_:Exception){};true};MotionEvent.ACTION_UP->{if(SystemClock.uptimeMillis()-down>700){remove();true}else{startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));true}};else->true}};try{wm.addView(root,lp)}catch(_:Exception){root=null}}
 private fun remove(){root?.let{try{wm.removeView(it)}catch(_:Exception){}};root=null;scene=null}
 override fun onDestroy(){remove();super.onDestroy()}
 companion object { const val ACTION_HIDE="com.yara.ai.ambient.HIDE"; fun start(c:Context){c.startService(Intent(c,AmbientOverlayService::class.java))};fun stop(c:Context){c.startService(Intent(c,AmbientOverlayService::class.java).setAction(ACTION_HIDE))} }
}

object OverlayState { @Volatile var inApp = false }
