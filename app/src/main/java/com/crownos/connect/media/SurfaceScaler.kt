package com.crownos.connect.media

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLExt
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.util.Size
import android.view.Surface
import com.crownos.connect.util.CaptureSizes
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private const val EGL_RECORDABLE_ANDROID = 0x3142
private const val POSITION_ATTRIBUTE = 0

private const val VERTEX_SHADER = """
attribute vec2 position;
uniform mat4 frameTransform;
uniform vec2 cropScale;
varying vec2 uv;
void main() {
    gl_Position = vec4(position, 0.0, 1.0);
    uv = (frameTransform * vec4(0.5 + 0.5 * position * cropScale, 0.0, 1.0)).xy;
}
"""

private const val FRAGMENT_SHADER = """
#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES frame;
varying vec2 uv;
void main() {
    gl_FragColor = texture2D(frame, uv);
}
"""

/** Redraws frames of [sourceSize] onto a surface of [outputSize], centre-cropped to fill it. Use from one thread. */
class SurfaceScaler(private val sourceSize: Size, private val outputSize: Size) {
    private var display = EGL14.EGL_NO_DISPLAY
    private var context = EGL14.EGL_NO_CONTEXT
    private var target = EGL14.EGL_NO_SURFACE
    private var frames: SurfaceTexture? = null
    private var input: Surface? = null
    private var transformLocation = -1
    private val transform = FloatArray(16)
    private val quad: FloatBuffer = ByteBuffer.allocateDirect(QUAD.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(QUAD)
        .apply { position(0) }

    fun start(output: Surface, handler: Handler): Surface {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "EGL did not start" }
        val config = recordableConfig()
        context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
        check(context != EGL14.EGL_NO_CONTEXT) { "no GL context" }
        target = EGL14.eglCreateWindowSurface(display, config, output, intArrayOf(EGL14.EGL_NONE), 0)
        check(target != EGL14.EGL_NO_SURFACE) { "the encoder surface refused GL" }
        check(EGL14.eglMakeCurrent(display, target, target, context)) { "the GL context is not current" }
        prepareProgram()
        val texture = SurfaceTexture(externalTexture()).apply {
            setDefaultBufferSize(sourceSize.width, sourceSize.height)
            setOnFrameAvailableListener(::draw, handler)
        }
        frames = texture
        return Surface(texture).also { input = it }
    }

    fun release() {
        frames?.setOnFrameAvailableListener(null)
        input?.release()
        frames?.release()
        input = null
        frames = null
        if (display == EGL14.EGL_NO_DISPLAY) return
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        if (target != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, target)
        if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
        EGL14.eglReleaseThread()
        target = EGL14.EGL_NO_SURFACE
        context = EGL14.EGL_NO_CONTEXT
        display = EGL14.EGL_NO_DISPLAY
    }

    private fun draw(texture: SurfaceTexture) {
        if (frames !== texture) return
        texture.updateTexImage()
        texture.getTransformMatrix(transform)
        GLES20.glUniformMatrix4fv(transformLocation, 1, false, transform, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, QUAD.size / 2)
        EGLExt.eglPresentationTimeANDROID(display, target, texture.timestamp)
        EGL14.eglSwapBuffers(display, target)
    }

    private fun recordableConfig(): EGLConfig {
        val attributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0)
        return checkNotNull(configs[0].takeIf { count[0] > 0 }) { "no recordable EGL config" }
    }

    private fun prepareProgram() {
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, compile(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER))
        GLES20.glAttachShader(program, compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER))
        GLES20.glBindAttribLocation(program, POSITION_ATTRIBUTE, "position")
        GLES20.glLinkProgram(program)
        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        check(linked[0] != 0) { GLES20.glGetProgramInfoLog(program) }
        GLES20.glUseProgram(program)
        transformLocation = GLES20.glGetUniformLocation(program, "frameTransform")
        val (scaleX, scaleY) = CaptureSizes.cropScale(sourceSize, outputSize)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "cropScale"), scaleX, scaleY)
        GLES20.glEnableVertexAttribArray(POSITION_ATTRIBUTE)
        GLES20.glVertexAttribPointer(POSITION_ATTRIBUTE, 2, GLES20.GL_FLOAT, false, 0, quad)
        GLES20.glViewport(0, 0, outputSize.width, outputSize.height)
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        check(compiled[0] != 0) { GLES20.glGetShaderInfoLog(shader) }
        return shader
    }

    private fun externalTexture(): Int {
        val names = IntArray(1)
        GLES20.glGenTextures(1, names, 0)
        val texture = names[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return texture
    }

    private companion object {
        val QUAD = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
    }
}
