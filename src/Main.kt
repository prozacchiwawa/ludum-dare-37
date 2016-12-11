/**
 * Created by arty on 12/9/16.
 */

package org.sample

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

val codemap : Map<Int, Key> =
        listOf(Key.C, Key.Space, Key.Left, Key.Up, Key.Down, Key.Right).map({ k -> Pair<Int,Key>(k.v,k) }).toMap()

class SpawnedNPC(id : Int, n : NPC, b : NPCBehavior) {
    val id = id
    val n = n
    val b = b
}

val timeBetweenSpawns = 5.0

class FloorAndDoor(floor : Int, door : Int) {
    val floor = floor
    val door = door
}

/* Every second, there's a chance to spawn a new npc if there aren't already the maximum number.
 * if an NPC will spawn, it'll be near the user, slightly more likely in the direction of the one room.
 */
class GameContainer(loadedResources : MutableMap<String, ResBundle>) {
    val light = newLight(0xeeeeee)

    val aspect =
            kotlin.browser.window.innerWidth / kotlin.browser.window.innerHeight
    val camera = newPerspectiveCamera(35.0, aspect, 0.1, 10000.0)

    var targetCameraX = 0.0
    var targetCameraY = 3.0

    var curTime = 0.0

    val hero = Hero()
    val elevator = Elevator(1, 6)

    val buildingMap = StaticBuildMap(6)
    val floors = (1..buildingMap.floors).map({n -> Floor(n)}).toList()

    var oneroom = randomFloorAndDoor()

    var wantNPCs = 10
    var nextSpawnTime = 0.0
    val npcs : MutableMap<Int, SpawnedNPC> = mutableMapOf()
    val loadedResources = loadedResources

    var nextId = 0

    fun randomFloorAndDoor() : FloorAndDoor {
        return FloorAndDoor(Math.floor(rand() * buildingMap.floors), Math.floor(rand() * numDoors))
    }

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

    fun spawnNPC(scene : Scene, floor : Int, door : Int, resname : String, behavior : NPCBehavior) {
        console.log("Spawn NPC", resname, "on", floor, "at", door)
        val floorObj = floors[floor]
        floorObj.toggleDoor(door)
        val gotRes = loadedResources.get(resname)
        if (gotRes != null) {
            val spawned = SpawnedNPC(nextId++, NPC(gotRes), behavior)
            spawned.n.group.o.position.x = floorObj.doors[door].o.position.x + 1
            spawned.n.group.o.position.y = (floor + 1) * floorHeight
            spawned.n.group.o.position.z = 2.0
            spawned.n.addToScene(scene)
            npcs.put(spawned.id, spawned)
        }
    }

    fun update(scene : Scene, m : GameUpdateMessage) {
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

            if (curTime > nextSpawnTime && npcs.size < wantNPCs) {
                nextSpawnTime = curTime + timeBetweenSpawns
                val fd = randomFloorAndDoor()
                spawnNPC(scene, fd.floor, fd.door, SKINNER_RES, PursueHeroNPCBehavior())
            }

            npcs.forEach { npc ->
                npc.value.b.update(buildingMap, elevator, hero, npc.value.n)
                npc.value.n.update(m.time)
            }
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
                        hero.getInElevator(elevator)
                    }
                }
                Key.Down -> {
                    console.log("leave elevator:",hero.group.o.position.x)
                    if (elevator.isOpen() && hero.inElevator()) {
                        hero.leaveElevator(elevator)
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
    game.update(scene, GameUpdateMessage(curTime - lastTime))
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

        val loader = newJSONLoader()
        val toLoad = arrayOf(SKINNER_RES)
        val loadedResources : MutableMap<String, ResBundle> = mutableMapOf()

        val runGame = {
            try {
                var game = GameContainer(loadedResources)

                kotlin.browser.window.addEventListener("keydown", { evt: dynamic ->
                    val key = codemap[evt.keyCode]
                    if (key != null) {
                        game.update(scene, GameUpdateMessage(GameUpdateMessageTag.KeyDown, key))
                    } else {
                        console.log("keydown unknown", evt.keyCode)
                    }
                })
                kotlin.browser.window.addEventListener("keyup", { evt: dynamic ->
                    val key = codemap[evt.keyCode]
                    if (key != null) {
                        game.update(scene, GameUpdateMessage(GameUpdateMessageTag.KeyUp, key))
                    } else {
                        console.log("keyup unknown", evt.keyCode)
                    }
                })

                game.addToScene(scene)

                lastTime = getCurTime()

                val onResize = { evt: dynamic ->
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

        toLoad.forEach { s ->
            loader.load(s, { geometry, materials ->
                (0..materials.length - 1).forEach({ i ->
                    val material = materials[i]
                    //material.skinning = true
                })
                loadedResources.put(s, ResBundle(geometry, materials))
                if (loadedResources.size == toLoad.size) { runGame() }
            })
        }
    } catch (e : Exception) {
        if (error != null && errorContent != null) {
            doError(error, errorContent, "${e}");
        }
    }
}
