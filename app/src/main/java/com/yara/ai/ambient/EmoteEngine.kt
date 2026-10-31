package com.yara.ai.ambient

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.sin
import kotlin.random.Random

data class EmoteParticle(var x:Float,var y:Float,var vx:Float,var vy:Float,var life:Float,var maxLife:Float,var glyph:String,var size:Float)
class EmoteEngine {
    private val particles= mutableListOf<EmoteParticle>()
    fun emit(command:String, x:Float, y:Float) { val c=command.lowercase(); val glyph=when { c.contains("heart")||c.contains("love")||c.contains("❤") -> "♥"; c.contains("wink") -> "✦"; c.contains("spark")||c.contains("happy") -> "✧"; c.contains("blush")||c.contains("shy") -> "○"; else -> "•" }; repeat(if(glyph=="♥") 4 else 6) { val life=1.2f+Random.nextFloat(); particles += EmoteParticle(x+Random.nextFloat()*24-12,y,Random.nextFloat()*36-18,-30-Random.nextFloat()*35,life,life,glyph,14+Random.nextFloat()*12) } }
    fun update(dt:Float) { particles.forEach { it.vy += 18f*dt; it.x += it.vx*dt; it.y += it.vy*dt; it.life -= dt }; particles.removeAll { it.life<=0f } }
    fun draw(canvas:Canvas, paint:Paint) { particles.forEach { p -> paint.alpha=(255*(p.life/p.maxLife)).toInt().coerceIn(0,255); paint.textSize=p.size; paint.color=if(p.glyph=="♥") 0xffff5f91.toInt() else 0xffffd86b.toInt(); canvas.drawText(p.glyph,p.x,p.y,paint) }; paint.alpha=255 }
}
