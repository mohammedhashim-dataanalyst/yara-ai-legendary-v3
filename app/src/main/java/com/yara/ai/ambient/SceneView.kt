package com.yara.ai.ambient

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import androidx.core.view.postOnAnimation
import kotlin.math.sin

class SceneView(context: Context): View(context) {
    private val engine=SceneEngine().also { it.setForTime() }; private val emotes=EmoteEngine(); private val paint=Paint(Paint.ANTI_ALIAS_FLAG); private var last=System.nanoTime(); private var downX=0f; private var downY=0f; private var running=true
    private val tick=object:Runnable{override fun run(){if(running){invalidate();postOnAnimation(this)}}}
    init { isClickable=true; postOnAnimation(tick) }
    fun cycleScene() { engine.cycle(); invalidate() }
    fun showEmote(command:String) { emotes.emit(command,width*.52f,height*.45f); invalidate() }
    fun pauseAnimation(value:Boolean) { running=!value }
    override fun onTouchEvent(e:MotionEvent):Boolean { when(e.action){MotionEvent.ACTION_DOWN->{downX=e.x;downY=e.y};MotionEvent.ACTION_UP->{if(e.x-downX>80) engine.cycle(1); else if(downX-e.x>80) engine.cycle(-1); else showEmote("heart"); performClick()}}; return true }
    override fun performClick():Boolean { super.performClick(); return true }
    override fun onDraw(c:Canvas) { super.onDraw(c); val now=System.nanoTime(); val dt=((now-last)/1e9).toFloat().coerceAtMost(.05f); last=now; drawScene(c,engine.current(),now/1e9f); emotes.update(dt); emotes.draw(c,paint) }
    private fun drawScene(c:Canvas,s:SceneKind,t:Float) { val w=width.toFloat(); val h=height.toFloat(); val bg=when(s){SceneKind.BEACH->0xff8bd8e8.toInt();SceneKind.STREET->0xffc6d3e7.toInt();SceneKind.BEDROOM->0xff261e44.toInt();SceneKind.KITCHEN->0xffffd59a.toInt();SceneKind.CAFE->0xffd9b48c.toInt();SceneKind.LIVING_ROOM->0xffb8a1c9.toInt()}; c.drawColor(bg); paint.style=Paint.Style.FILL; paint.color=0xfff7e6b5.toInt(); if(s==SceneKind.BEDROOM||s==SceneKind.LIVING_ROOM){c.drawRect(0f,h*.18f,w*.42f,h*.55f,paint);paint.color=0xfff6d56f.toInt();c.drawCircle(w*.2f,h*.29f,28f,paint)} else {paint.color=0xffffdc69.toInt();c.drawCircle(w*.82f,h*.18f,32f,paint)}; paint.color=when(s){SceneKind.BEACH->0xffe9c46a.toInt();SceneKind.STREET->0xff667085.toInt();else->0xff704c3a.toInt()};c.drawRect(0f,h*.62f,w,h,paint); drawProps(c,s,w,h); drawYara(c,w*.52f,h*.59f+sin(t*2f)*5f,s,t); }
    private fun drawProps(c:Canvas,s:SceneKind,w:Float,h:Float){paint.color=0xff5b3a29.toInt();when(s){SceneKind.BEDROOM->{c.drawRect(w*.05f,h*.48f,w*.4f,h*.7f,paint);paint.color=0xffe8bfd1.toInt();c.drawRect(w*.08f,h*.43f,w*.36f,h*.51f,paint)};SceneKind.KITCHEN->{c.drawRect(w*.07f,h*.42f,w*.35f,h*.65f,paint);paint.color=0xff5d6870.toInt();c.drawCircle(w*.25f,h*.43f,32f,paint)};SceneKind.BEACH->{paint.color=0xff328c9b.toInt();c.drawRect(w*.08f,h*.46f,w*.16f,h*.64f,paint);paint.color=0xff26846e.toInt();c.drawCircle(w*.12f,h*.4f,44f,paint)};SceneKind.CAFE->{paint.color=0xff6b402c.toInt();c.drawRect(w*.08f,h*.48f,w*.4f,h*.55f,paint);c.drawRect(w*.12f,h*.55f,w*.16f,h*.68f,paint);c.drawRect(w*.34f,h*.55f,w*.38f,h*.68f,paint)};SceneKind.STREET->{paint.color=0xff3c8053.toInt();c.drawRect(w*.12f,h*.37f,w*.15f,h*.62f,paint);c.drawCircle(w*.135f,h*.3f,48f,paint)};SceneKind.LIVING_ROOM->{paint.color=0xff6b4e71.toInt();c.drawRoundRect(w*.05f,h*.47f,w*.4f,h*.68f,18f,18f,paint);paint.color=0xffd7a86e.toInt();c.drawRect(w*.72f,h*.4f,w*.8f,h*.63f,paint)}}}
    private fun drawYara(c:Canvas,x:Float,y:Float,s:SceneKind,t:Float){paint.color=0xffffc3a9.toInt();c.drawCircle(x,y-94f,32f,paint);paint.color=0xff29213e.toInt();c.drawCircle(x,y-112f,34f,paint);paint.color=0xffe66b89.toInt();c.drawCircle(x-11f,y-91f,3f,paint);c.drawCircle(x+11f,y-91f,3f,paint);paint.color=0xff6f5bd3.toInt();c.drawRoundRect(x-22f,y-62f,x+22f,y+18f,18f,18f,paint);paint.color=0xffffc3a9.toInt();val action=if(s==SceneKind.STREET)sin(t*4f)*12f else 0f;c.drawRect(x-35f,y-50f,x-22f+action,y+4f,paint);c.drawRect(x+22f,y-50f,x+35f-action,y+4f,paint);paint.color=0xff30274b.toInt();c.drawRect(x-18f,y+18f,x-5f,y+62f,paint);c.drawRect(x+5f,y+18f,x+18f,y+62f,paint)}
}
