/*
 * Copyright 2019-2022 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.internal.utils

import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import net.mamoe.mirai.utils.getRandomUnsignedInt
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

internal object AtomicIntSeq {
    @JvmStatic
    inline fun forMessageSeq(): AtomicIntMaxSeq = AtomicIntMaxSeq(atomic(0))

    @JvmStatic
    inline fun forPrivateSync(): AtomicInt65535Seq = AtomicInt65535Seq(atomic(getRandomUnsignedInt()))
}

// value classes to optimize space

@JvmInline
internal value class AtomicIntMaxSeq(
    private val value: AtomicInt
) {
    /**
     * Increment [value] within the range from `0` (inclusive) to [Int.MAX_VALUE] (exclusive).
     */
    inline fun next(): Int = value.incrementAndGet().mod(Int.MAX_VALUE)

    /**
     * Atomically update [value] if it is smaller than [new].
     *
     * @param new should be positive
     */
    inline fun updateIfSmallerThan(new: Int): Boolean {
        value.update { instant ->
            if (instant < new) new else return false
        }
        return true
    }

    /**
     * Atomically update [value] if it different with [new].
     *
     * @param new should be positive
     */
    inline fun updateIfDifferentWith(new: Int): Boolean {
        value.update { instant ->
            if (instant == new) return false
            new
        }
        return true
    }
}

@JvmInline
internal value class AtomicInt65535Seq(
    private val value: AtomicInt = atomic(0)
) {
    /**
     * Increment [value] within the range from `0` (inclusive) to `65535` (exclusive).
     */
    inline fun next(): Int = value.incrementAndGet().mod(65535)

    /**
     * Atomically update [value] if it is smaller than [new].
     *
     * @param new should be positive
     */
    inline fun updateIfSmallerThan(new: Int): Boolean {
        value.update { instant ->
            if (instant < new) new else return false
        }
        return true
    }

    /**
     * Atomically update [value] if it different with [new].
     *
     * @param new should be positive
     */
    inline fun updateIfDifferentWith(new: Int): Boolean {
        value.update { instant ->
            if (instant == new) return false
            new
        }
        return true
    }
}