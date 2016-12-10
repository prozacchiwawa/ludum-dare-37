/**
 * Created by arty on 12/9/16.
 */

package org.sample

import java.util.*

class Geometry(o : dynamic) {
    val o = o
}

fun newBoxGeometry(x : Double, y : Double, z : Double) : Geometry {
    return Geometry(js("(function (x,y,z) { return new THREE.BoxGeometry(x,y,z); })")(x,y,z))
}

class Material(o : dynamic) {
    val o = o
}

fun newMeshLambertMaterial(color : Int) : Material {
    return Material(js("(function (c) { return new THREE.MeshLambertMaterial({color: c}); })")(color))
}

class Mesh(o : dynamic) {
    val o = o
}

fun newMesh(geom : dynamic, material : dynamic) : Mesh {
    return Mesh(js("(function(g,m) { return new THREE.Mesh(g,m); })")(geom.o,material.o))
}

class Camera(o : dynamic) {
    val o = o
}

fun newPerspectiveCamera(fl : Double, aspect : Double, minZ : Double, maxZ : Double) : Camera {
    return Camera(js("(function(fl,a,nz,xz) { return new THREE.PerspectiveCamera(fl, a, nz, xz) })")(fl, aspect, minZ, maxZ))
}

class Scene(o : dynamic) {
    val o = o
    fun add(m : Mesh) { o.add(m.o) }
    fun add(l : Light) { o.add(l.o) }
    fun add(g : Group) { o.add(g.o) }
    fun remove(m : Mesh) { o.remove(m.o) }
    fun remove(l : Light) { o.remove(l.o) }
    fun remove(g : Group) { o.remove(g.o) }
}

fun newScene() : Scene {
    return Scene(js("new THREE.Scene()"))
}

class Light(o : dynamic) {
    val o = o
}

fun newLight(color : Int) : Light {
    return Light(js("(function(c) { return new THREE.PointLight( c ) })")(color))
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

class Group(o : dynamic) {
    val o = o
    fun add(m : Mesh) { o.add(m.o) }
    fun add(m : Group) { o.add(m.o) }
    fun remove(m : Mesh) { o.remove(m.o) }
    fun remove(m : Group) { o.remove(m.o) }
}

fun newGroup() : Group {
    return Group(js("new THREE.Group()"))
}

class Floor(number : Int) {
    val number = number
    val backWallGeom = newBoxGeometry(100.0, 2.0, 0.2)
    val backWallMaterial = newMeshLambertMaterial(0xf2e9c4)
    val backWall = newMesh(backWallGeom, backWallMaterial)
    val floorGeom = newBoxGeometry(100.0, 0.2, 2.0)
    val floorMaterial = newMeshLambertMaterial(0x006600)
    val floor = newMesh(floorGeom, floorMaterial)
    val doorGeom = newBoxGeometry(1.0, 1.5, 0.15)
    val doorMaterial = newMeshLambertMaterial(0x000055)
    val doorHandleGeom = newBoxGeometry(0.1, 0.1, 0.1)
    val doorHandleMaterial = newMeshLambertMaterial(0xffffff)

    var doors : MutableList<Group> = mutableListOf()
    val group = newGroup()

    init {
        backWall.o.position.y = 1.0
        floor.o.position.z = 1.0
        group.add(backWall)
        group.add(floor)
        group.o.position.y = number * 2.0
        doors = (0..10).map({ n ->
            val d = newGroup()
            val plate = newMesh(doorGeom, doorMaterial)
            plate.o.position.y = 0.75
            plate.o.position.z = 0.1
            val handle = newMesh(doorHandleGeom, doorHandleMaterial)
            handle.o.position.x = 0.4
            handle.o.position.y = 0.75
            handle.o.position.z = 0.15
            d.add(plate)
            d.add(handle)
            d.o.position.x = (n - 6.0) * 4.0
            d
        }).toMutableList()
        doors.forEach({ d -> group.add(d) })
    }
    fun addToScene(scene : Scene) {
        scene.add(group)
    }
    fun removeFromScene(scene : Scene) {
        scene.remove(group)
    }
}

fun sigmoid(x : Double) : Double {
    return 1.0 / (1.0 + Math.exp(-x))
}

val codemap : Map<Int, Key> = hashMapOf(
        Pair<Int,Key>(Key.Space.v, Key.Space)
)

class GameContainer() {
    val geom = newBoxGeometry(0.5,0.5,0.5)
    val material = newMeshLambertMaterial(0xff0000)
    val mesh = newMesh(geom,material)

    val light = newLight(0xeeeeee)

    val aspect =
            kotlin.browser.window.innerWidth / kotlin.browser.window.innerHeight
    val camera = newPerspectiveCamera(35.0, aspect, 0.1, 10000.0)

    val floors = (1..100).map({n -> Floor(n)}).toList()

    var cameraY = 3.0
    var targetCameraY = 3.0
    var curTime = 0.0
    var lastMove = 0.0
    var targetFloor = 0

    fun addToScene(scene : Scene) {
        scene.add(mesh)
        scene.add(light)
        floors.forEach { f -> f.addToScene(scene) }
    }

    fun removeFromScene(scene : Scene) {
        scene.remove(mesh)
        floors.forEach { f -> f.removeFromScene(scene) }
    }

    fun update(m : GameUpdateMessage) {
        if (m.tag == GameUpdateMessageTag.NewFrame) {
            curTime += m.time
            cameraY = (cameraY + (targetCameraY * 3.0 * m.time)) / (1.0 + 3.0 * m.time)
            camera.o.position.set( 0, cameraY, 15.0 )
            camera.o.lookAt( 0, 1.0, 0.0 )
            light.o.position.set( camera.o.position.x, camera.o.position.y, 10000.0 )
        } else if (m.tag == GameUpdateMessageTag.KeyDown) {
            targetFloor = (targetFloor + 1) % 100
            targetCameraY = (targetFloor * 2.0) + 1.0
        }
    }
}

fun doError(container : org.w3c.dom.Element, content : org.w3c.dom.Element, t : String) {
    container.setAttribute("style", "top: 0")
    content.innerHTML = t;
}

var lastTime = 0.0

fun render(renderer : dynamic, scene : Scene, game : GameContainer) {
    var curTime = getCurTime()
    game.update(GameUpdateMessage(curTime - lastTime))
    lastTime = getCurTime()
    renderer.render( scene.o, game.camera.o )
    kotlin.browser.window.requestAnimationFrame { render(renderer,scene,game) }
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

        var game = GameContainer()

        kotlin.browser.window.addEventListener("keydown", { evt : dynamic ->
            val key = codemap[evt.keyCode]
            if (key != null) {
                game.update(GameUpdateMessage(GameUpdateMessageTag.KeyDown, key))
            }
        })

        game.addToScene(scene)

        lastTime = getCurTime()

        val onResize = { evt : dynamic ->
            renderer.setSize(kotlin.browser.window.innerWidth, kotlin.browser.window.innerHeight)
        }
        kotlin.browser.window.addEventListener("resize", onResize)

        render(renderer, scene, game)
    } catch (e : Exception) {
        if (error != null && errorContent != null) {
            doError(error, errorContent, "${e}");
        }
    }
}
