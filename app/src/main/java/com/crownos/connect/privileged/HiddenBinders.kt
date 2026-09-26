package com.crownos.connect.privileged

import android.os.IBinder
import android.util.Log
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

private const val TAG = "HiddenBinders"

object HiddenBinders {
    fun systemInterface(serviceName: String, interfaceName: String): Any? = runCatching {
        val binder = SystemServiceHelper.getSystemService(serviceName) ?: return null
        Class.forName("$interfaceName\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(binder))
    }.onFailure { Log.w(TAG, "$interfaceName is unavailable", it) }.getOrNull()

    fun callbackProxy(interfaceName: String, binder: IBinder): Any? = runCatching {
        val type = Class.forName(interfaceName)
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
            if (method.name == "asBinder") binder else null
        }
    }.onFailure { Log.w(TAG, "$interfaceName cannot be proxied", it) }.getOrNull()
}

fun Any.invokeHidden(name: String, vararg candidates: Array<Any?>): Result<Any?> {
    val methods = javaClass.methods.filter { it.name == name }
    val (method, arguments) = candidates.firstNotNullOfOrNull { arguments ->
        methods.firstOrNull { it.parameterCount == arguments.size }?.let { it to arguments }
    } ?: return Result.failure(NoSuchMethodException("$name with ${candidates.map { it.size }} arguments"))
    return method.invokeUnwrapped(this, arguments)
}

private fun Method.invokeUnwrapped(target: Any, arguments: Array<Any?>): Result<Any?> = try {
    Result.success(invoke(target, *arguments))
} catch (failure: InvocationTargetException) {
    Log.w(TAG, "$name failed", failure.targetException)
    Result.failure(failure.targetException)
} catch (failure: ReflectiveOperationException) {
    Log.w(TAG, "$name could not be called", failure)
    Result.failure(failure)
} catch (failure: IllegalArgumentException) {
    Log.w(TAG, "$name rejected its arguments", failure)
    Result.failure(failure)
}
