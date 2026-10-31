package com.yara.ai.render
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
class YaraAvatarView(context:Context):GLSurfaceView(context){ val headRenderer=YaraHeadRenderer(); private var x=0f;private var y=0f; init{setEGLContextClientVersion(2);setRenderer(headRenderer);renderMode=RENDERMODE_CONTINUOUSLY}; override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){MotionEvent.ACTION_DOWN->{x=e.x;y=e.y;return true};MotionEvent.ACTION_MOVE->{headRenderer.yaw+=(e.x-x)*.4f;headRenderer.pitch=(headRenderer.pitch-(e.y-y)*.3f).coerceIn(-35f,35f);x=e.x;y=e.y;return true}};return true} }
