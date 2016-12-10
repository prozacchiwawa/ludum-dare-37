/**
 * Created by arty on 12/9/16.
 */

package org.sample

import java.util.*

fun newBoxGeometry(x : Double, y : Double, z : Double) {
    return js("(function (x,y,z) { return new THREE.BoxGeometry(x,y,z); })")(x,y,z)
}

fun newMeshLambertMaterial(color : Int) {
    return js("(function (c) { return new THREE.MeshLambertMaterial({color: c}); })")(color)
}

fun newMesh(geom : dynamic, material : dynamic) : dynamic {
    return js("(function(g,m) { return new THREE.Mesh(g,m); })")(geom,material)
}

fun newPerspectiveCamera(fl : Double, aspect : Double, minZ : Double, maxZ : Double) : dynamic {
    return js("(function(fl,a,nz,xz) { return new THREE.PerspectiveCamera(fl, a, nz, xz) })")(fl, aspect, minZ, maxZ);
}

fun newScene() : dynamic {
    return js("new THREE.Scene()")
}

fun newLight(color : Int) : dynamic {
    return js("(function(c) { return new THREE.PointLight( c ) })")(color)
}

fun getCurTime() : Double {
    return js("(new Date().getTime()) / 1000.0")
}

fun rand() : Double {
    return js("Math.random()")
}

enum class GameUpdateMessageTag {
    NoOp, NewFrame, KeyDown, KeyUp, MouseMove, MouseDown, MouseUp
}

enum class Key(v : Int) {
    None(0), Space(32);
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

class PlayState(n : Double, inc : Double) {
    val n : Double = n
    val inc : Double = inc
    fun update(m : GameUpdateMessage) : PlayState {
        when (m.tag) {
            GameUpdateMessageTag.NewFrame -> {
                return PlayState(n + (inc * m.time), inc)
            }
            GameUpdateMessageTag.KeyDown -> {
                val inc = rand() * 10
                return PlayState(n, inc)
            }
            else -> {
                throw Exception("Unhandled case ${m}")
            }
        }
    }
}

val codemap : Map<Int, Key> = hashMapOf(
        Pair<Int,Key>(Key.Space.v, Key.Space)
)

enum class GameState {
    PrePlay, Play, LevelOut, LevelIn, Dying
}

class GameContainer(state : PlayState) {
    val stateStack : Array<GameState> = Array<GameState>(20, { n -> GameState.PrePlay })
    val curState : Int = 0
    var state : PlayState = state
    val geom = newBoxGeometry(5.0,5.0,5.0)
    val material = newMeshLambertMaterial(0xff0000)
    val mesh = newMesh(geom,material)

    fun update(m : GameUpdateMessage) {
        state = state.update(m)
        mesh.rotation.x = state.n
    }
}

fun doError(container : org.w3c.dom.Element, content : org.w3c.dom.Element, t : String) {
    container.setAttribute("style", "top: 0")
    content.innerHTML = t;
}

var lastTime = 0.0

fun render(renderer : dynamic, scene : dynamic, camera : dynamic, game : GameContainer) {
    var curTime = getCurTime()
    game.update(GameUpdateMessage(curTime - lastTime))
    lastTime = getCurTime()
    renderer.render( scene, camera );
    kotlin.browser.window.requestAnimationFrame { render(renderer,scene,camera,game) }
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
        var aspect =
                kotlin.browser.window.innerWidth / kotlin.browser.window.innerHeight;
        val camera = newPerspectiveCamera(35.0, aspect, 0.1, 10000.0)
        renderer.setSize( kotlin.browser.window.innerWidth, kotlin.browser.window.innerHeight );
        renderer.domElement.setAttribute("class", "gamecontent")
        gamerender?.appendChild( renderer.domElement );

        val scene = newScene()
        camera.position.set( -15, 10, 15 );
        camera.lookAt( scene.position );

        var light = newLight(0xffff00)
        light.position.set( 10, 0, 10 );
        scene.add( light );

        renderer.setClearColor( 0xdddddd, 1);

        var game = GameContainer(PlayState(0.0, 0.0))

        kotlin.browser.window.addEventListener("keydown", { evt : dynamic ->
            val key = codemap[evt.keyCode]
            if (key != null) {
                game.update(GameUpdateMessage(GameUpdateMessageTag.KeyDown, key))
            }
        })

        scene.add( game.mesh );

        lastTime = getCurTime()

        render(renderer, scene, camera, game)
    } catch (e : Exception) {
        if (error != null && errorContent != null) {
            doError(error, errorContent, "${e}");
        }
    }
}
