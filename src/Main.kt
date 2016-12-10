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

fun newObjectLoader() : dynamic {
    return js("new THREE.ObjectLoader()")
}

fun newJSONLoader() : dynamic {
    return js("new THREE.JSONLoader()")
}

fun newAnimationMixer(mesh : Mesh) : dynamic {
    return js("(function (mesh) { return new THREE.AnimationMixer(mesh) })")(mesh.o)
}

fun newSkinnedMesh(geometry : dynamic, material : dynamic) : dynamic {
    return js("(function (geometry,material) { return new THREE.SkinnedMesh(geometry,material) })")(geometry, material)
}

fun newMeshFaceMaterial(materials : dynamic) : dynamic {
    return js("(function (materials) { return new THREE.MeshFaceMaterial(materials) })")(materials)
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
    None(0), Space(32), C('C'.toInt()), Left(37), Up(38), Right(39), Down(40);
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

val floorHeight = 3.0

val clock = js("new THREE.Clock()")

interface InScene {
    fun addToScene(scene : Scene)
    fun removeFromScene(scene : Scene)
}

class Floor(number : Int) : InScene {
    val number = number
    val backWallGeom = newBoxGeometry(20.0, floorHeight, 0.2)
    val backWallMaterial = newMeshLambertMaterial(0xf2e9c4)
    val floorGeom = newBoxGeometry(100.0, 0.2, 2.0)
    val floorMaterial = newMeshLambertMaterial(0x006600)
    val floor = newMesh(floorGeom, floorMaterial)
    val doorGeom = newBoxGeometry(1.0, 2.2, 0.15)
    val doorMaterial = newMeshLambertMaterial(0x000055)
    val doorHandleGeom = newBoxGeometry(0.1, 0.1, 0.1)
    val doorHandleMaterial = newMeshLambertMaterial(0xffffff)

    var doors : MutableList<Group> = mutableListOf()
    val group = newGroup()

    val doorOpenRotation = -0.2

    init {
        val backWallLeft = newMesh(backWallGeom, backWallMaterial)
        backWallLeft.o.position.x = -10.5
        backWallLeft.o.position.y = floorHeight / 2.0
        backWallLeft.o.position.z = 0.0
        group.add(backWallLeft)
        val backWallRight = newMesh(backWallGeom, backWallMaterial)
        backWallRight.o.position.x = 10.5
        backWallRight.o.position.y = floorHeight / 2.0
        backWallRight.o.position.z = 0.0
        group.add(backWallRight)
        floor.o.position.z = 1.0
        group.add(floor)
        group.o.position.y = number * floorHeight
        doors = (0..8).map({ n ->
            val d = newGroup()
            val plate = newMesh(doorGeom, doorMaterial)
            plate.o.position.x = 1.0
            plate.o.position.y = 1.1
            plate.o.position.z = 0.1
            val handle = newMesh(doorHandleGeom, doorHandleMaterial)
            handle.o.position.x = 1.4
            handle.o.position.y = 1.1
            handle.o.position.z = 0.15
            d.add(plate)
            d.add(handle)
            val elevatorBump = if (n < 5) { 1.0 } else { 0.0 }
            d.o.position.x = (n - 4.0 - elevatorBump) * 4.0
            d
        }).toMutableList()
        doors.forEach({ d -> group.add(d) })
    }

    fun nearDoor(x : Double) : Int? {
        val res = doors.mapIndexedNotNull({ i, doorGroup ->
            val res = doorGroup.o.position.x + 1.1 < x && doorGroup.o.position.x + 1.6 > x
            return if (res) { i } else { null }
        }).firstOrNull()
        return res
    }

    fun toggleDoor(d : Int) {
        val door = doors.get(d)
        if (door != null) {
            if (door.o.rotation.y < 0) {
                door.o.rotation.y = 0
            } else {
                door.o.rotation.y = doorOpenRotation
            }
        }
    }

    override fun addToScene(scene : Scene) {
        scene.add(group)
    }
    override fun removeFromScene(scene : Scene) {
        scene.remove(group)
    }
}

class Hero : InScene {
    val box = newBoxGeometry(0.3, 1.3, 0.4)
    val material = newMeshLambertMaterial(0x8caadb)
    val mesh = newMesh(box, material)
    val group = newGroup()

    var moving = false
    var moveexpire = 0.0
    var movedir = 0.0
    val movetime = 0.1
    var movespeed = 2.0

    var stored : dynamic = null
    var mixer : dynamic = null

    var hello : dynamic = null

    init {
        val loader = newJSONLoader()
        loader.load("SkinnerWalk2x1.json", { geometry, materials ->
            console.log(materials)
            (0..materials.length - 1).forEach({ i ->
                val material = materials[i]
                // material.skinning = true // Allows animation, bug
            })
            console.log("loaded", geometry)
            val smesh = Mesh(newSkinnedMesh(geometry, newMeshFaceMaterial(materials)))
            this.stored = smesh
            val holderGroup = newGroup()
            holderGroup.o.position.y = 0.0
            holderGroup.o.position.z = 1.0
            holderGroup.o.rotation.y = Math.PI / 2.0
            mixer = newAnimationMixer(smesh)
            hello = mixer.clipAction(geometry.animations[0])
            hello.enabled = true
            holderGroup.add(smesh)
            group.add(holderGroup)
        })
        group.o.position.y = floorHeight
    }

    fun inElevator() : Boolean {
        return group.o.position.z < 0
    }

    fun onFloor() : Int {
        return Math.round(group.o.position.y / floorHeight) - 1
    }

    fun beginMove(x : Double) {
        moving = true
        moveexpire = -1.0
        movedir = x
    }

    fun endMove() {
        moving = false
        moveexpire = movetime
    }

    var playing = false

    fun update(t : Double) {
        if (mixer != null) {
            mixer.update(t)
        }
        if (!playing && hello != null) {
            playing = true
            hello.play()
        }
        if (movedir != 0.0) {
            group.o.position.x += movedir * t * movespeed
        }
        if (moveexpire > 0) {
            moveexpire = Math.max(moveexpire - t, 0.0)
            if (moveexpire == 0.0) {
                movedir = 0.0
                moveexpire = -1.0
            }
        }
    }

    override fun addToScene(scene : Scene) {
        scene.add(group)
    }

    override fun removeFromScene(scene: Scene) {
        scene.remove(group)
    }
}

class Elevator(min : Int, max : Int) : InScene {
    var floor = min
    var direction = false
    var open = -1.0
    val min = min
    val max = max
    val espeed = 1.0
    val openTime = 2.0

    val elevatorBackGeom = newBoxGeometry(1.0, 2.0, 0.1)
    val elevatorBackMat = newMeshLambertMaterial(0x3f4f3e)
    val elevatorBack = newMesh(elevatorBackGeom, elevatorBackMat)
    val elevatorDoorGeom = newBoxGeometry(1.0, 2.0, 0.1)
    val elevatorDoorMat = newMeshLambertMaterial(0x3f4f3e)
    val elevatorDoor = newMesh(elevatorDoorGeom, elevatorDoorMat)
    val elevatorFloorGeom = newBoxGeometry(1.0, 0.1, 2.0)
    val elevatorFloorMat = newMeshLambertMaterial(0xdae0d9)
    val elevatorFloor = newMesh(elevatorFloorGeom, elevatorFloorMat)
    val group = newGroup()

    init {
        elevatorBack.o.position.y = 1.0
        elevatorBack.o.position.z = -2.0
        elevatorDoor.o.position.y = 1.0
        elevatorDoor.o.position.z = 0.0
        group.add(elevatorBack)
        group.add(elevatorFloor)
        group.o.position.y = floorHeight
    }

    override fun addToScene(scene : Scene) {
        scene.add(group)
    }

    override fun removeFromScene(scene: Scene) {
        scene.remove(group)
    }

    fun callButton(newFloor : Int) {
        if (floor > newFloor) {
            direction = false
        } else if (floor < newFloor) {
            direction = true
        } else {
            if (open > 0.0) {
                open = openTime
            }
        }
    }

    fun onFloor() : Int {
        return Math.round(group.o.position.y / floorHeight) - 1
    }

    fun isOpen() : Boolean {
        return open > 0.0
    }

    fun update(time : Double) {
        val targetY = floor * floorHeight
        if (targetY != group.o.position.y) {
            open = -1.0
            if (group.o.position.y < targetY) {
                group.o.position.y = Math.min(group.o.position.y + (espeed * time), targetY)
            } else {
                group.o.position.y = Math.max(group.o.position.y - (espeed * time), targetY)
            }
        } else {
            if (open == -1.0) {
                open = openTime
                group.remove(elevatorDoor)
            } else {
                open = Math.max(open - time, 0.0)
                if (open == 0.0) {
                    group.add(elevatorDoor)
                    floor = if (direction) { Math.min(floor + 1, max) } else { Math.max(floor - 1, min) }
                    if (floor == max) { direction = false }
                    if (floor == min) { direction = true }
                }
            }
        }
    }
}

fun sigmoid(x : Double) : Double {
    return 1.0 / (1.0 + Math.exp(-x))
}

val codemap : Map<Int, Key> =
        listOf(Key.C, Key.Space, Key.Left, Key.Up, Key.Down, Key.Right).map({ k -> Pair<Int,Key>(k.v,k) }).toMap()

class GameContainer() {
    val light = newLight(0xeeeeee)

    val aspect =
            kotlin.browser.window.innerWidth / kotlin.browser.window.innerHeight
    val camera = newPerspectiveCamera(35.0, aspect, 0.1, 10000.0)

    val floors = (1..100).map({n -> Floor(n)}).toList()

    var targetCameraX = 0.0
    var targetCameraY = 3.0

    var curTime = 0.0

    val hero = Hero()
    val elevator = Elevator(1, 6)

    init {
        camera.o.position.x = 0
        camera.o.position.z = 15.0
        camera.o.lookAt(0, 1.0, 0.0)
    }

    fun addToScene(scene : Scene) {
        scene.add(light)
        hero.addToScene(scene)
        elevator.addToScene(scene)
        floors.forEach { f -> f.addToScene(scene) }
    }

    fun removeFromScene(scene : Scene) {
        scene.remove(light)
        hero.removeFromScene(scene)
        elevator.removeFromScene(scene)
        floors.forEach { f -> f.removeFromScene(scene) }
    }

    fun heroCurrentFloor() : Int {
        return Math.round((hero.group.o.position.y - 1.0) / 2.0)
    }

    fun update(m : GameUpdateMessage) {
        if (m.tag == GameUpdateMessageTag.NewFrame) {
            curTime += m.time
            hero.update(m.time)
            elevator.update(m.time)
            if (hero.inElevator()) {
                hero.group.o.position.y = elevator.group.o.position.y
            }
            camera.o.position.x = (camera.o.position.x + (targetCameraX * 3.0 * m.time)) / (1.0 + 3.0 * m.time)
            camera.o.position.y = (camera.o.position.y + (targetCameraY * 3.0 * m.time)) / (1.0 + 3.0 * m.time)
            camera.o.lookAt( 0, 1.0, 0.0 )
            light.o.position.set( camera.o.position.x, camera.o.position.y, 10000.0 )
        } else if (m.tag == GameUpdateMessageTag.KeyDown) {
            when (m.key) {
                Key.C -> {
                    console.log("call elevator:",hero.group.o.position.x)
                    if (hero.group.o.position.x >= -1 &&
                        hero.group.o.position.x <= 1) {
                        elevator.callButton(heroCurrentFloor())
                    }
                }
                Key.Up -> {
                    console.log("enter elevator:",elevator.isOpen(), hero.inElevator())
                    if (hero.group.o.position.x >= -1 &&
                            hero.group.o.position.x <= 1 &&
                            elevator.isOpen() &&
                            elevator.onFloor() == hero.onFloor() &&
                            !hero.inElevator()) {
                        hero.group.o.position.x = 0.0
                        hero.group.o.position.z = -1.5
                    }
                }
                Key.Down -> {
                    console.log("leave elevator:",hero.group.o.position.x)
                    if (elevator.isOpen() && hero.inElevator()) {
                        hero.group.o.position.x = 0.0
                        hero.group.o.position.z = 0.0
                    }
                }
                Key.Left -> { if (!hero.inElevator()) { hero.beginMove(-1.0) } }
                Key.Right -> { if (!hero.inElevator()) { hero.beginMove(1.0) } }
                Key.Space -> {
                    val floor = floors.get(hero.onFloor())
                    console.log("try open door on",floor)
                    if (floor != null) {
                        val door = floor.nearDoor(hero.group.o.position.x)
                        console.log("try open door",door,"on",floor)
                        if (door != null) {
                            console.log("toggle",door,"on",floor)
                            floor.toggleDoor(door)
                        }
                    }
                }
                else -> { }
            }
        } else if (m.tag == GameUpdateMessageTag.KeyUp) {
            when (m.key) {
                Key.Left -> { hero.endMove() }
                Key.Right -> { hero.endMove() }
                else -> { }
            }
        }
        targetCameraX = hero.group.o.position.x
        targetCameraY = hero.group.o.position.y + (floorHeight / 2.0)
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
            } else {
                console.log("keydown unknown", evt.keyCode)
            }
        })
        kotlin.browser.window.addEventListener("keyup", { evt : dynamic ->
            val key = codemap[evt.keyCode]
            if (key != null) {
                game.update(GameUpdateMessage(GameUpdateMessageTag.KeyUp, key))
            } else {
                console.log("keyup unknown", evt.keyCode)
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
