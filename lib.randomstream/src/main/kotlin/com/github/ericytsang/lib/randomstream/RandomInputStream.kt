package com.github.ericytsang.lib.randomstream

import java.io.InputStream
import java.util.Random

class RandomInputStream(val random:Random = Random()):InputStream()
{
    override fun read():Int
    {
        return (1..8)
            .map {if (random.nextBoolean()) 1 else 0}
            .fold(0) {old,new -> old.shl(1).or(new)}
    }
}
