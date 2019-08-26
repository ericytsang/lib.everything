package com.github.ericytsang.lib.game

import com.github.ericytsang.lib.prop.ReadOnlyProp
import com.github.ericytsang.lib.prop.listen
import com.github.ericytsang.lib.prop.value
import java.io.Serializable

class ScreenLogger<ScreenParams:Serializable,Screen:GdxGame.Screen<ScreenParams>>(
        val logger:(()->String)->Unit,
        val wrapee:Screen)
    :GdxGame.Screen<ScreenParams>
{
    init
    {
        debug {"created"}

        listOf(shouldRenderContinuously).listen()
        {
            debug {"shouldRenderContinuously.value = ${shouldRenderContinuously.value}"}
        }
    }

    override val shouldRenderContinuously:ReadOnlyProp<Unit,Boolean> get() = wrapee.shouldRenderContinuously
    override fun render(elapsedMillis:Long):GdxGame.AppRequest<ScreenParams> = debug("render($elapsedMillis)") {wrapee.render(elapsedMillis)}
    override fun resize(dimensions:RectDimens) = debug("resize($dimensions)") {wrapee.resize(dimensions)}
    override fun save():ScreenParams = debug("save()") {wrapee.save()}
    override fun close():ScreenParams = debug("close()") {wrapee.close()}

    private fun <R> debug(methodInvocation:String,action:()->R):R
    {
        debug {methodInvocation}
        val result = action()
        debug {"$methodInvocation = $result"}
        return result
    }

    private fun debug(text:()->String)
    {
        logger {"${wrapee::class.simpleName?:"noname"}: ${text()}"}
    }
}