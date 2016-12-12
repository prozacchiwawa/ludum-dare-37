/**
 * Created by arty on 12/9/16.
 */

package org.sample

enum class GameUpdateMessageTag {
    NoOp, NewFrame, KeyDown, KeyUp, MouseMove, MouseDown, MouseUp
}

enum class Key(v : Int) {
    None(0), Space(32), C('C'.toInt()), S(83), Left(37), Up(38), Right(39), Down(40);
    val v = v
}

class GameUpdateMessage {
    var tag : GameUpdateMessageTag = GameUpdateMessageTag.NoOp
    var key : Key = Key.None
    var mx : Int = 0
    var my : Int = 0
    var time : Double = 0.0

    constructor(tag : GameUpdateMessageTag, key : Key) {
        this.tag = tag
        this.key = key
    }

    constructor(tag : GameUpdateMessageTag, mx : Int, my : Int) {
        this.tag = tag
        this.mx = mx
        this.my = my
    }

    constructor(time : Double) {
        this.tag = GameUpdateMessageTag.NewFrame
        this.time = time
    }
}

val codemap : Map<Int, Key> =
        listOf(Key.C, Key.S, Key.Space, Key.Left, Key.Up, Key.Down, Key.Right).map({ k -> Pair<Int,Key>(k.v,k) }).toMap()

class SpawnedNPC(id : Int, n : NPC, pursuer : Boolean, b : NPCBehavior) {
    val id = id
    val n = n
    val b = b
    val pursuer = pursuer
}

val timeBetweenSpawns = 7.0

class FloorAndDoor(floor : Int, door : Int) {
    val floor = floor
    val door = door
}

class ModeChange(pop : Boolean, push : IGameMode?) {
    val pop = pop
    val push = push
}

interface IGameMode : InScene {
    fun update(scene : Scene, m : GameUpdateMessage) : ModeChange
    fun getCamera() : Camera
}

class DieMode(returnToMode : IGameMode) : InScene, IGameMode {
    var shownTime = 0.0
    val showTime = 5.0
    val returnToMode = returnToMode
    val deathDiv = kotlin.browser.document.getElementById("death-div")

    override fun update(scene : Scene, m : GameUpdateMessage) : ModeChange {
        val god = deathDiv
        shownTime += m.time
        god?.setAttribute("style", "display: flex")
        if (shownTime >= showTime) {
            god?.setAttribute("style", "display: none")
            return ModeChange(true, null)
        }
        return ModeChange(false, null)
    }

    override fun addToScene(scene : Scene) {
    }

    override fun removeFromScene(scene : Scene) {
    }

    override fun getCamera() : Camera { return returnToMode.getCamera() }
}

class GameOverMode(returnToMode : IGameMode) : InScene, IGameMode {
    var shownTime = 0.0
    val showTime = 5.0
    val returnToMode = returnToMode
    val gameOverDiv = kotlin.browser.document.getElementById("game-over-div")

    override fun update(scene : Scene, m : GameUpdateMessage) : ModeChange {
        val god = gameOverDiv
        shownTime += m.time
        god?.setAttribute("style", "display: flex")
        if (shownTime >= showTime) {
            god?.setAttribute("style", "display: none")
            return ModeChange(true, null)
        }
        return ModeChange(false, null)
    }

    override fun addToScene(scene : Scene) {
    }

    override fun removeFromScene(scene : Scene) {
    }

    override fun getCamera() : Camera { return returnToMode.getCamera() }
}

class ClueMode(clue : String, returnToMode : IGameMode) : InScene, IGameMode {
    val clue = clue
    var shownTime = 0.0
    val showTime = 5.0
    val returnToMode = returnToMode
    val deathDiv = kotlin.browser.document.getElementById("clue-div")
    val textDiv = kotlin.browser.document.getElementById("clue-center")

    override fun update(scene : Scene, m : GameUpdateMessage) : ModeChange {
        val god = deathDiv
        shownTime += m.time
        god?.setAttribute("style", "display: flex")
        textDiv?.innerHTML = clue
        if (shownTime >= showTime) {
            god?.setAttribute("style", "display: none")
            return ModeChange(true, null)
        }
        return ModeChange(false, null)
    }

    override fun addToScene(scene : Scene) {
    }

    override fun removeFromScene(scene : Scene) {
    }

    override fun getCamera() : Camera { return returnToMode.getCamera() }
}

class WinMode(text : String, divName : String, returnToMode: IGameMode) : InScene, IGameMode {
    val text = text
    var shownTime = 0.0
    val showTime = 30.0
    val returnToMode = returnToMode
    val deathDiv = kotlin.browser.document.getElementById("${divName}-div")
    val textDiv = kotlin.browser.document.getElementById("${divName}-center")

    override fun update(scene : Scene, m : GameUpdateMessage) : ModeChange {
        if (m.tag == GameUpdateMessageTag.KeyDown) {
            shownTime = showTime
        }
        val god = deathDiv
        shownTime += m.time
        god?.setAttribute("style", "display: flex")
        val chars = Math.round(shownTime * (text.size.toDouble() / (showTime * 0.75)))
        textDiv?.innerHTML = text.substring(0, chars)
        if (shownTime >= showTime) {
            god?.setAttribute("style", "display: none")
            return ModeChange(true, null)
        }
        return ModeChange(false, null)
    }

    override fun addToScene(scene : Scene) {
    }

    override fun removeFromScene(scene : Scene) {
    }

    override fun getCamera() : Camera { return returnToMode.getCamera() }
}

fun doError(container : org.w3c.dom.Element, content : org.w3c.dom.Element, t : String) {
    container.setAttribute("style", "top: 0")
    content.innerHTML = t;
}

var lastTime = 0.0

fun render(doUpdate : (GameUpdateMessage) -> Unit) {
    var curTime = getCurTime()
    doUpdate(GameUpdateMessage(curTime - lastTime))
    lastTime = getCurTime()
    kotlin.browser.window.requestAnimationFrame { render(doUpdate) }
}

fun flashEffect() {
    val flash = kotlin.browser.window.document.getElementById("flash")
    flash?.setAttribute("style", "display: block")
    kotlin.browser.window.setTimeout({ flash?.setAttribute("style", "display: none") }, 10)
}

fun main(args: Array<String>) {
    val gamerender = kotlin.browser.document.getElementById("gamerender")
    val rawWindow : dynamic = kotlin.browser.window
    val error = kotlin.browser.window.document.getElementById("error")
    val errorContent = kotlin.browser.window.document.getElementById("error-content")
    try {
        val THREE = rawWindow?.THREE
        if (THREE == null) {
            throw Exception("THREE not loaded")
        }
        console.log(THREE)
        val renderer = js("new THREE.WebGLRenderer()")
        if (renderer == null) {
            throw Exception("No renderer")
        }
        renderer.setSize( kotlin.browser.window.innerWidth, kotlin.browser.window.innerHeight );
        renderer.domElement.setAttribute("class", "gamecontent")
        gamerender?.appendChild( renderer.domElement );

        val scene = newScene()

        renderer.setClearColor( 0xdddddd, 1);

        val badgesElement = kotlin.browser.window.document.getElementById("gameui-badges")
        val starsElement = kotlin.browser.window.document.getElementById("gameui-wanted")

        val setWantedStars = { wanted : Double, caught : Double ->
            val stars = Math.round(5.0 * wanted)
            val maxWanted = Math.max(stars, 5)
            val minWanted = Math.min(stars, 0)
            val nonred = Math.round(254.0 * Math.max(0.0, 1.0 - caught))
            starsElement?.setAttribute("style","color: rgb(255,${nonred},${nonred})")
            var res = ""
            for (i in 1..5) {
                if (i <= stars) {
                    res += "\u2605"
                } else {
                    res += "\u2606"
                }
            }
            if (starsElement != null) {
                starsElement.innerHTML = res
            }
        }

        val setBadges = { badges : Int ->
            var res = ""
            for (i in 1..badges) {
                res += "<i class='badge'>\u268e</i>"
            }
            if (badgesElement != null) {
                badgesElement.innerHTML = res
            }
        }

        val stateStack : MutableList<IGameMode> = mutableListOf()
        val game = GameContainer({ flashEffect() })
        stateStack.add(game)
        stateStack.add(WinMode(introText, "win", game))
        val gameState = {
            stateStack[stateStack.size - 1]
        }
        val doUpdate = { msg : GameUpdateMessage ->
            val modechange = gameState().update(scene, msg)
            if (modechange.pop) {
                gameState().removeFromScene(scene)
                stateStack.removeAt(stateStack.size - 1)
                gameState().addToScene(scene)
            }
            val newMode = modechange.push
            if (newMode != null) {
                gameState().removeFromScene(scene)
                stateStack.add(newMode)
                gameState().addToScene(scene)
            }
        }

        kotlin.browser.window.addEventListener("keydown", { evt: dynamic ->
            val key = codemap[evt.keyCode]
            if (key != null) {
                doUpdate(GameUpdateMessage(GameUpdateMessageTag.KeyDown, key))
            } else {
                console.log("keydown unknown", evt.keyCode)
            }
        })
        kotlin.browser.window.addEventListener("keyup", { evt: dynamic ->
            val key = codemap[evt.keyCode]
            if (key != null) {
                gameState().update(scene, GameUpdateMessage(GameUpdateMessageTag.KeyUp, key))
            } else {
                console.log("keyup unknown", evt.keyCode)
            }
        })

        gameState().addToScene(scene)

        lastTime = getCurTime()

        val onResize = { evt: dynamic ->
            renderer.setSize(kotlin.browser.window.innerWidth, kotlin.browser.window.innerHeight)
        }
        kotlin.browser.window.addEventListener("resize", onResize)

        render({ msg : GameUpdateMessage ->
            setBadges(badges)
            setWantedStars(wanted, caught)
            renderer.render( scene.o, game.getCamera().o )
            doUpdate(msg)
        })
    } catch (e : Exception) {
        if (error != null && errorContent != null) {
            doError(error, errorContent, "${e}");
        }
    }
}
