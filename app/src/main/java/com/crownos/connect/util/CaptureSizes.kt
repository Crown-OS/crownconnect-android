package com.crownos.connect.util

import android.util.Size
import kotlin.math.abs

object CaptureSizes {
    /** The smallest size that covers [wanted] with the nearest aspect ratio, else the largest one. */
    fun closest(candidates: List<Size>, wanted: Size): Size? {
        val aspect = wanted.aspect
        return candidates
            .filter { it.width >= wanted.width && it.height >= wanted.height }
            .minWithOrNull(compareBy<Size>({ abs(it.aspect - aspect) }, { it.area }))
            ?: candidates.maxByOrNull { it.area }
    }

    /** How much of the source's width and height survive a centre crop that fills [target]. */
    fun cropScale(source: Size, target: Size): Pair<Float, Float> {
        val ratio = target.aspect / source.aspect
        return if (ratio < 1f) ratio to 1f else 1f to 1f / ratio
    }

    private val Size.aspect: Float get() = width.toFloat() / height
    private val Size.area: Int get() = width * height
}
