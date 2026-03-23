package com.auto.app.util

import kotlin.random.Random

object RandomUtils {
    
    fun randomDelay(minMs: Long = 500, maxMs: Long = 2000): Long {
        return Random.nextLong(minMs, maxMs + 1)
    }
    
    fun randomOffset(maxOffset: Int = 10): Int {
        return Random.nextInt(-maxOffset, maxOffset + 1)
    }
    
    fun randomCoordinate(baseX: Int, baseY: Int, maxOffset: Int = 10): Pair<Int, Int> {
        val offsetX = randomOffset(maxOffset)
        val offsetY = randomOffset(maxOffset)
        return Pair(baseX + offsetX, baseY + offsetY)
    }
    
    fun randomBetween(min: Int, max: Int): Int {
        return Random.nextInt(min, max + 1)
    }
    
    fun randomBetween(min: Long, max: Long): Long {
        return Random.nextLong(min, max + 1)
    }
    
    fun randomBetween(min: Float, max: Float): Float {
        return Random.nextFloat() * (max - min) + min
    }
    
    fun randomBoolean(probability: Float = 0.5f): Boolean {
        return Random.nextFloat() < probability
    }
    
    fun randomChoice(vararg options: String): String {
        return options[Random.nextInt(options.size)]
    }
    
    fun <T> randomChoice(list: List<T>): T {
        return list[Random.nextInt(list.size)]
    }
    
    fun generateBesselCurve(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        points: Int = 20
    ): List<Pair<Float, Float>> {
        val result = mutableListOf<Pair<Float, Float>>()
        
        val controlX1 = startX + (endX - startX) * Random.nextFloat() * 0.3f
        val controlY1 = startY + (endY - startY) * Random.nextFloat() * 0.5f
        val controlX2 = startX + (endX - startX) * (0.7f + Random.nextFloat() * 0.3f)
        val controlY2 = startY + (endY - startY) * (0.5f + Random.nextFloat() * 0.5f)
        
        for (i in 0..points) {
            val t = i.toFloat() / points
            val x = bezierPoint(startX, controlX1, controlX2, endX, t)
            val y = bezierPoint(startY, controlY1, controlY2, endY, t)
            result.add(Pair(x, y))
        }
        
        return result
    }
    
    private fun bezierPoint(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val u = 1 - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t
        
        return uuu * p0 + 3 * uu * t * p1 + 3 * u * tt * p2 + ttt * p3
    }
}
