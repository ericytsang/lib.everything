package com.github.ericytsang.lib.property

import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TestScopeRule:TestWatcher()
{
    val managedScope = CloseableGroup()
    override fun finished(description:Description?)
    {
        managedScope.close()
    }
}

class PropertyTest
{
    @get:Rule
    val testScope = TestScopeRule()

    @Test
    fun `map transforms property`()
    {
        val source = DataProperty(1)
        fun transform(input:Int) = input+1
        val mapped = source.map(::transform)

        val emittedValues = LinkedBlockingQueue<Int>()
        fun emittedValuesSoFar() = generateSequence {emittedValues.poll()}.toList()

        assertEquals(listOf(),emittedValuesSoFar())

        testScope.managedScope.addCloseables()
        {scope ->
            mapped.listen(scope)
            {
                emittedValues += it
            }
        }
        assertEquals(listOf(2),emittedValuesSoFar())

        val newSourceValues = listOf(3,3,4,5)
        newSourceValues.forEach(source::set)
        assertEquals(newSourceValues.map(::transform),emittedValuesSoFar())
    }

    @Test
    fun `isInstance returns false when passed null`()
    {
        val integer: Int? = null
        assertFalse(1::class.isInstance(integer))
    }

    @Test
    fun `map emits a value every time a new listener registers`()
    {
        val source = DataProperty(1)
        fun transform(input:Int) = input+1
        val mapped = source.map(::transform)

        val emittedValues = LinkedBlockingQueue<Int>()
        fun emittedValuesSoFar() = generateSequence {emittedValues.poll()}.toList()

        assertEquals(listOf(),emittedValuesSoFar())

        repeat(3)
        {
            CloseableGroup().use()
            {scope ->
                scope.addCloseables()
                {addScope ->
                    mapped.listen(addScope)
                    {
                        emittedValues += it
                    }
                }
            }
        }
        assertEquals(listOf(2,2,2),emittedValuesSoFar())
    }

    @Test
    fun `switchMap transforms property`()
    {
        val source1 = DataProperty(1)
        val source2 = DataProperty(2.0)
        val source3 = DataProperty(3.0)
        fun transform(input:Int) = if (input%2 == 0) source2 else source3
        val mapped = source1.switchMap(::transform)

        val emittedValues = LinkedBlockingQueue<Double>()
        fun emittedValuesSoFar() = generateSequence {emittedValues.poll()}.toList()

        assertEquals(listOf(),emittedValuesSoFar())

        testScope.managedScope.addCloseables()
        {scope ->
            mapped.listen(scope)
            {
                emittedValues += it
            }
        }
        assertEquals(listOf(3.0),emittedValuesSoFar())

        run()
        {
            val newSourceValues = listOf(3,3,4,5)
            newSourceValues.forEach(source1::set)
            assertEquals(
                newSourceValues
                    .map(::transform)
                    .map(DataProperty<Double>::value),
                emittedValuesSoFar()
            )
        }

        run()
        {
            source1.value = 1
            source3.value = 3.1
            source1.value = 0
            source2.value = 2.1
            assertEquals(
                listOf(3.0,3.1,2.0,2.1),
                emittedValuesSoFar()
            )
        }
    }

    @Test
    fun `DataProperty emits value upon subscribing`() {
        val source = DataProperty(1)

        val emittedValues = LinkedBlockingQueue<Int>()
        fun emittedValuesSoFar() = generateSequence {emittedValues.poll()}.toList()

        testScope.managedScope.addCloseables {scope ->
            source.listen(scope) {emittedValues += it}
        }

        assertEquals(listOf(1), emittedValuesSoFar())
    }

    @Test
    fun `EventProperty doesn't emit value upon subscribing`() {
        val source = EventProperty<Int>()

        val emittedValues = LinkedBlockingQueue<Int>()
        fun emittedValuesSoFar() = generateSequence {emittedValues.poll()}.toList()

        testScope.managedScope.addCloseables {scope ->
            source.listen(scope) {emittedValues += it}
        }

        assertEquals(listOf(), emittedValuesSoFar())
    }

    @Test
    fun `mapped DataProperty emits value upon subscribing`() {
        val source = DataProperty(1)
        val mapped = source.map { it + 1 }.map { it * 2 }

        val emittedValues = LinkedBlockingQueue<Int>()
        fun emittedValuesSoFar() = generateSequence {emittedValues.poll()}.toList()

        testScope.managedScope.addCloseables {scope ->
            mapped.listen(scope) {emittedValues += it}
        }

        assertEquals(listOf(4), emittedValuesSoFar())
    }

    @Test
    fun `switchMapped DataProperty emits value upon subscribing`() {
        val switchMap = SwitchMapSetup().switchMap

        val emittedValues = LinkedBlockingQueue<String>()
        fun emittedValuesSoFar() = generateSequence {emittedValues.poll()}.toList()

        testScope.managedScope.addCloseables {scope ->
            switchMap.listen(scope) {emittedValues += it}
        }

        assertEquals(listOf("left"), emittedValuesSoFar())
    }

    @Test
    fun `switchMapped mapped DataProperty emits value upon subscribing`() {
        val left = DataProperty("left")
        val mapped = MapSetup(left)
        val source = DataProperty(Source.Left)
        val right = DataProperty("right")
        val switchMapped = SwitchMapSetup(source,mapped.map,right)

        val emittedValues = LinkedBlockingQueue<String>()
        fun emittedValuesSoFar() = generateSequence {emittedValues.poll()}.toList()

        testScope.managedScope.addCloseables {scope ->
            switchMapped.switchMap.listen(scope) {emittedValues += it}
        }

        assertEquals(listOf("left mapped"), emittedValuesSoFar())
    }

    @Test
    fun `switchMapped mapped DataProperty propagates events`() {
        val left = DataProperty("left")
        val mapped = MapSetup(left)
        val source = DataProperty(Source.Left)
        val right = DataProperty("right")
        val switchMapped = SwitchMapSetup(source,mapped.map,right)

        val emittedValues = LinkedBlockingQueue<String>()
        fun emittedValuesSoFar() = generateSequence {emittedValues.poll()}.toList()

        testScope.managedScope.addCloseables {scope ->
            switchMapped.switchMap.listen(scope) {emittedValues += it}
        }

        emittedValuesSoFar()

        source.value = Source.Right // "right"
        assertEquals(listOf("right"),emittedValuesSoFar())
        source.value = Source.Left // "left mapped"
        assertEquals(listOf("left mapped"),emittedValuesSoFar())
        left.value = "new left" // "new left mapped"
        assertEquals(listOf("new left mapped"),emittedValuesSoFar())
        right.value = "new right" // not observed
        assertEquals(listOf(), emittedValuesSoFar())
    }

    @Test
    fun `chained maps and switchMaps`() {
        val source1 = DataProperty("source1")
        val source2 = DataProperty("source2")
        val mapped = source1.map {"$it mapped"}
        val switchMapped = mapped.switchMap { if (it.startsWith("source")) mapped else source2 }

        val emittedValues = LinkedBlockingQueue<String>()
        fun emittedValuesSoFar() = generateSequence {emittedValues.poll()}.toList()

        assertEquals(listOf(), emittedValuesSoFar())

        testScope.managedScope.addCloseables {scope ->
            switchMapped.listen(scope) {
                emittedValues += it
            }
        }

        assertEquals(listOf("source1 mapped"), emittedValuesSoFar())

        source1.value = "source1 value2"
        assertEquals(listOf("source1 value2 mapped"), emittedValuesSoFar())
        source2.value = "source2 value6"
        assertEquals(listOf(), emittedValuesSoFar())
        source1.value = "xsource1 value2" // should be observing source2 now
        assertEquals(listOf("source2 value6"), emittedValuesSoFar())
        source1.value = "xsource1 value3" // should ignore this because observing source2 now
        assertEquals(listOf(), emittedValuesSoFar())
        source2.value = "source2 value7"
        assertEquals(listOf("source2 value7"), emittedValuesSoFar())
        source1.value = "source1 value4"
        assertEquals(listOf("source1 value4 mapped"), emittedValuesSoFar())
    }

    enum class Source {
        Left,
        Right
    }

    class SwitchMapSetup(
            val source: Property<Source> = DataProperty(Source.Left),
            val left: Property<String> = DataProperty("left"),
            val right: Property<String> = DataProperty("right")
    ) {
        val switchMap = source.switchMap {
            when (it) {
                Source.Left -> left
                Source.Right -> right
            }
        }
    }

    class MapSetup(
            val source: Property<String> = DataProperty("source")
    ) {
        val map = source.map {"$it mapped"}
    }
}