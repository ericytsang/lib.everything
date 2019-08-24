package com.github.ericytsang.lib.game

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.DataProp
import com.github.ericytsang.lib.prop.Prop
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.ReadOnlyProp
import com.github.ericytsang.lib.prop.aggregate
import com.github.ericytsang.lib.prop.listen
import com.github.ericytsang.lib.prop.mutableNullableValue
import com.github.ericytsang.lib.prop.nullableValue
import com.github.ericytsang.lib.prop.statefulListen
import com.github.ericytsang.lib.prop.value
import java.io.Closeable
import java.io.Serializable

class GdxGame<GameContext:Closeable,ScreenParams:Serializable>(
        val platformContext:PlatformContext,
        val gameContextFactory:(PlatformContext)->GameContext,
        val screenFactory:(GameContext,ScreenParams)->Screen<ScreenParams>,
        initialScreenParams:ScreenParams,
        val screenDecorator:(Screen<ScreenParams>)->Screen<ScreenParams> = {it})
    :ApplicationListener
{
    private val currentScreen = object:Prop<Unit,Screen<ScreenParams>>()
    {
        private var field = screenDecorator(InitializeScreen(initialScreenParams))
        override fun doGet(context:Unit):Screen<ScreenParams> = field
        override fun doSet(context:Unit,value:Screen<ScreenParams>)
        {
            if (value != field) field = screenDecorator(value)
        }
    }

    private val created = RaiiProp(Opt.of<GameContext>())
    override fun dispose() = created.close()
    override fun create()
    {
        // create the game context
        created.mutableNullableValue = fun() = gameContextFactory(platformContext)

        // monitor screen properties
        listOf(currentScreen).aggregate {currentScreen.value}.statefulListen()
        {
            (state,closeables) ->

            // keep track of whether the screen wants continuous rendering or not
            closeables.addCloseables()
            {
                it+listOf(state.shouldRenderContinuously).listen()
                {
                    Gdx.graphics.isContinuousRendering = state.shouldRenderContinuously.value
                }
            }
        }
    }

    private val paused = RaiiProp(Opt.of<Closeable>())
    override fun resume() = paused.close()
    override fun pause()
    {
        paused.mutableNullableValue = fun():Closeable
        {
            val closeableGroup = CloseableGroup()

            closeableGroup.addCloseables()
            {
                closeable ->

                // save & dispose of the current screen
                val savedState = currentScreen.value.save()
                currentScreen.value.close()

                // set the current screen to a paused screen
                val pauseScreen = PauseScreen(savedState)
                currentScreen.value = pauseScreen

                // upon resume, tell the pause screen to resume to the previous screen
                closeable += Closeable()
                {
                    pauseScreen.resume()
                }
            }

            return closeableGroup
        }
    }

    override fun render()
    {
        val elapsedMillis = Gdx.graphics.deltaTime.times(1000).toLong()
        currentScreen.value = when(currentScreen.value.render(elapsedMillis))
        {
            is AppRequest.NoAction -> currentScreen.value
            is AppRequest.Transition ->
            {
                val nextScreenParams = currentScreen.value.close()
                screenFactory(created.nullableValue!!,nextScreenParams)
            }
        }
    }
    override fun resize(width:Int,height:Int) = currentScreen.value.resize(RectDimens(width,height))

    interface Screen<ScreenParams:Serializable>:Raii<ScreenParams>
    {
        val shouldRenderContinuously:ReadOnlyProp<Unit,Boolean>
        fun render(elapsedMillis:Long):AppRequest<ScreenParams>
        fun resize(dimensions:RectDimens)
        fun save():ScreenParams
        override fun close():ScreenParams
    }

    private class InitializeScreen<ScreenParams:Serializable>(
            val firstRealScreenParams:ScreenParams)
        :Screen<ScreenParams>
    {
        override val shouldRenderContinuously:ReadOnlyProp<Unit,Boolean> = DataProp(true)
        override fun render(elapsedMillis:Long) = AppRequest.Transition<ScreenParams>()
        override fun resize(dimensions:RectDimens) = Unit
        override fun save():ScreenParams = firstRealScreenParams
        override fun close():ScreenParams = firstRealScreenParams
    }

    private class PauseScreen<ScreenParams:Serializable>(
            val previousScreenParams:ScreenParams)
        :Screen<ScreenParams>
    {
        private val shouldResume = DataProp(false)
        override val shouldRenderContinuously:ReadOnlyProp<Unit,Boolean> get() = shouldResume
        override fun render(elapsedMillis:Long) = if (!shouldResume.value)
        {
            AppRequest.NoAction<ScreenParams>()
        }
        else
        {
            AppRequest.Transition<ScreenParams>()
        }
        override fun resize(dimensions:RectDimens) = Unit
        override fun save():ScreenParams = previousScreenParams
        override fun close():ScreenParams = previousScreenParams
        fun resume()
        {
            shouldResume.value = true
        }
    }

    sealed class AppRequest<ScreenParams:Serializable>
    {
        data class NoAction<ScreenParams:Serializable>(val unused:Serializable = 0):AppRequest<ScreenParams>()
        data class Transition<ScreenParams:Serializable>(val unused:Serializable = 0):AppRequest<ScreenParams>()
    }
}