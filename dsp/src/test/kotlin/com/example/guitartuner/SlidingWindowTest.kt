package com.example.guitartuner

import com.example.guitartuner.dsp.SlidingWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SlidingWindowTest {

    @Test
    fun `emits windows sliding by hop size`() {
        val w = SlidingWindow(frameSize = 4, hopSize = 2)
        val frames = mutableListOf<ShortArray>()
        w.push(shortArrayOf(1, 2, 3, 4, 5, 6, 7, 8)) { frames.add(it) }

        assertEquals(3, frames.size)
        assertTrue(frames[0].contentEquals(shortArrayOf(1, 2, 3, 4)))
        assertTrue(frames[1].contentEquals(shortArrayOf(3, 4, 5, 6)))
        assertTrue(frames[2].contentEquals(shortArrayOf(5, 6, 7, 8)))
    }

    @Test
    fun `no emit before frame is full`() {
        val w = SlidingWindow(frameSize = 4, hopSize = 2)
        var emits = 0
        w.push(shortArrayOf(1, 2, 3)) { emits++ }
        assertEquals(0, emits)
    }

    @Test
    fun `realistic frame and hop counts`() {
        val frameSize = 2048
        val hop = 512
        val w = SlidingWindow(frameSize, hop)
        val frames = mutableListOf<ShortArray>()

        val n = frameSize + hop * 2  // 2048 + 1024 = 3072
        val samples = ShortArray(n) { (it % 30000).toShort() }
        w.push(samples) { frames.add(it) }

        assertEquals(3, frames.size)
        // 第一帧 = 样本 0..2047
        assertEquals(0, frames[0][0].toInt())
        assertEquals(2047, frames[0][2047].toInt())
        // 第二帧 = 512..2559
        assertEquals(512, frames[1][0].toInt())
        assertEquals(2559, frames[1][2047].toInt())
        // 第三帧 = 1024..3071
        assertEquals(1024, frames[2][0].toInt())
        assertEquals(3071, frames[2][2047].toInt())
    }

    @Test
    fun `reset reaccumulates fresh content`() {
        val w = SlidingWindow(frameSize = 4, hopSize = 2)
        val frames = mutableListOf<ShortArray>()
        w.push(shortArrayOf(1, 2, 3, 4)) { }   // 填满并发射一次（丢弃）
        w.reset()
        w.push(shortArrayOf(9, 8, 7, 6)) { frames.add(it) }
        assertEquals(1, frames.size)
        assertTrue(frames[0].contentEquals(shortArrayOf(9, 8, 7, 6)))
    }
}
