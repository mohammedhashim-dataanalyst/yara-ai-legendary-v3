package com.yara.ai.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/** Dependency-free procedural OpenGL ES 2.0 avatar with Phong-ish lighting. */
class YaraHeadRenderer : GLSurfaceView.Renderer {
    @Volatile var jawOpen = 0f; @Volatile var mouthWide = 0f; @Volatile var smile = 0f
    @Volatile var blink = 0f; @Volatile var brow = 0f; @Volatile var yaw = 0f
    @Volatile var pitch = 0f; @Volatile var roll = 0f; @Volatile var idleSway = true
    private val projection=FloatArray(16); private val view=FloatArray(16); private val model=FloatArray(16); private val mvp=FloatArray(16); private val vp=FloatArray(16)
    private var prog=0; private var pos=0; private var normal=0; private var mvpLoc=0; private var modelLoc=0; private var color=0; private var light=0
    private val parts=mutableListOf<Part>(); private val start=System.currentTimeMillis()
    private data class Part(val vertices:FloatBuffer,val normals:FloatBuffer,val count:Int,val color:FloatArray)
    override fun onSurfaceCreated(gl:GL10?, config:EGLConfig?) { GLES20.glClearColor(.035f,.02f,.07f,1f); GLES20.glEnable(GLES20.GL_DEPTH_TEST); GLES20.glEnable(GLES20.GL_CULL_FACE); prog=buildProgram(VERTEX,FRAGMENT); pos=GLES20.glGetAttribLocation(prog,"aPos"); normal=GLES20.glGetAttribLocation(prog,"aNormal"); mvpLoc=GLES20.glGetUniformLocation(prog,"uMvp"); modelLoc=GLES20.glGetUniformLocation(prog,"uModel"); color=GLES20.glGetUniformLocation(prog,"uColor"); light=GLES20.glGetUniformLocation(prog,"uLight"); build() }
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int) { GLES20.glViewport(0,0,w,h); Matrix.perspectiveM(projection,0,35f,w.toFloat()/h.coerceAtLeast(1),.1f,100f); Matrix.setLookAtM(view,0,0f,.1f,4.2f,0f,.05f,0f,0f,1f,0f) }
    override fun onDrawFrame(gl:GL10?) { build(); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT); val t=(System.currentTimeMillis()-start)/1000f; Matrix.setIdentityM(model,0); Matrix.rotateM(model,0,yaw+(if(idleSway) sin(t*.7f)*6f else 0f),0f,1f,0f); Matrix.rotateM(model,0,pitch,1f,0f,0f); Matrix.rotateM(model,0,roll,0f,0f,1f); Matrix.multiplyMM(vp,0,projection,0,view,0); Matrix.multiplyMM(mvp,0,vp,0,model,0); GLES20.glUseProgram(prog); GLES20.glUniformMatrix4fv(mvpLoc,1,false,mvp,0); GLES20.glUniformMatrix4fv(modelLoc,1,false,model,0); GLES20.glUniform3f(light,-.4f,.8f,1f); parts.forEach { part -> GLES20.glUniform4fv(color,1,part.color,0); GLES20.glEnableVertexAttribArray(pos); GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,part.vertices); GLES20.glEnableVertexAttribArray(normal); GLES20.glVertexAttribPointer(normal,3,GLES20.GL_FLOAT,false,0,part.normals); GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,part.count) } }
    private fun build() { parts.clear(); parts+=sphere(0f,.18f,0f,.82f,.98f,.88f,floatArrayOf(.94f,.70f,.58f,1f),40,28); parts+=sphere(0f,-.72f,0f,.28f,.46f,.28f,floatArrayOf(.92f,.66f,.55f,1f),24,16); parts+=sphere(0f,-1.28f,0f,1.22f,.30f,.46f,floatArrayOf(.16f,.11f,.24f,1f),32,12); parts+=sphere(0f,.60f,-.08f,.87f,.72f,.86f,floatArrayOf(.10f,.07f,.11f,1f),36,22); val ey=.27f+brow*.04f; val eyeY=.10f*(1f-blink.coerceIn(0f,.95f)); parts+=sphere(-.28f,ey,.70f,.15f,eyeY,.08f,floatArrayOf(.97f,.97f,.98f,1f),20,12); parts+=sphere(.28f,ey,.70f,.15f,eyeY,.08f,floatArrayOf(.97f,.97f,.98f,1f),20,12); parts+=sphere(-.28f,ey,.785f,.055f,.055f,.03f,floatArrayOf(.1f,.05f,.04f,1f),16,10); parts+=sphere(.28f,ey,.785f,.055f,.055f,.03f,floatArrayOf(.1f,.05f,.04f,1f),16,10); val mw=.16f+mouthWide*.12f; parts+=sphere(0f,-.30f-jawOpen*.18f,.74f,mw,.025f+jawOpen*.08f,.035f,floatArrayOf(.55f,.13f,.19f,1f),20,10); if(smile>.01f) parts+=sphere(0f,-.32f-jawOpen*.18f,.72f,mw*smile,.018f,.025f,floatArrayOf(.72f,.24f,.27f,1f),16,8) }
    private fun sphere(cx:Float,cy:Float,cz:Float,sx:Float,sy:Float,sz:Float,c:FloatArray,segments:Int,rings:Int):Part { val v=ArrayList<Float>(); val n=ArrayList<Float>(); fun add(a:Float,b:Float) { val x=sin(a)*sin(b); val y=cos(b); val z=cos(a)*sin(b); v+=cx+sx*x; v+=cy+sy*y; v+=cz+sz*z; n+=x; n+=y; n+=z }; for(r in 0 until rings) for(s in 0 until segments) { val a0=s.toFloat()/segments*(Math.PI*2).toFloat(); val a1=(s+1).toFloat()/segments*(Math.PI*2).toFloat(); val b0=r.toFloat()/rings*Math.PI.toFloat(); val b1=(r+1).toFloat()/rings*Math.PI.toFloat(); add(a0,b0);add(a1,b0);add(a1,b1);add(a0,b0);add(a1,b1);add(a0,b1) }; return Part(buf(v),buf(n),v.size/3,c) }
    private fun buf(x:List<Float>):FloatBuffer=ByteBuffer.allocateDirect(x.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(x.toFloatArray());position(0)}
    private fun buildProgram(v:String,f:String):Int { fun sh(t:Int,s:String):Int { val id=GLES20.glCreateShader(t); GLES20.glShaderSource(id,s); GLES20.glCompileShader(id); val q=IntArray(1); GLES20.glGetShaderiv(id,GLES20.GL_COMPILE_STATUS,q,0); if(q[0]==0)throw IllegalStateException(GLES20.glGetShaderInfoLog(id)); return id }; val id=GLES20.glCreateProgram(); GLES20.glAttachShader(id,sh(GLES20.GL_VERTEX_SHADER,v)); GLES20.glAttachShader(id,sh(GLES20.GL_FRAGMENT_SHADER,f)); GLES20.glLinkProgram(id); return id }
    companion object { private const val VERTEX="uniform mat4 uMvp; uniform mat4 uModel; attribute vec3 aPos; attribute vec3 aNormal; varying vec3 vN; varying vec3 vP; void main(){vN=normalize(mat3(uModel[0].xyz,uModel[1].xyz,uModel[2].xyz)*aNormal);vP=aPos;gl_Position=uMvp*vec4(aPos,1.0);}"; private const val FRAGMENT="precision mediump float;uniform vec4 uColor;uniform vec3 uLight;varying vec3 vN;varying vec3 vP;void main(){vec3 n=normalize(vN);vec3 l=normalize(uLight);float d=max(dot(n,l),0.0);vec3 v=normalize(vec3(0.0,0.0,4.2)-vP);vec3 h=normalize(l+v);float s=pow(max(dot(n,h),0.0),42.0);gl_FragColor=vec4(uColor.rgb*(.30+.70*d)+vec3(.35)*s,uColor.a);}" }
}
