/**
 * Created by arty on 12/9/16.
 */

package org.sample

enum class GameUpdateMessageTag {
    NoOp, NewFrame, KeyDown, KeyUp, MouseMove, MouseDown, MouseUp
}

enum class Key(v : Int) {
    None(0), Space(32), C('C'.toInt()), S('S'.toInt()), Left(37), Up(38), Right(39), Down(40);
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

class SpawnedNPC(id : Int, n : NPC, pursuer : Boolean, b : NPCBehavior) {
    val id = id
    val n = n
    val b = b
    val pursuer = pursuer
}

val timeBetweenSpawns = 5.0

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
            return ModeChange(true, returnToMode)
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
            return ModeChange(true, returnToMode)
        }
        return ModeChange(false, null)
    }

    override fun addToScene(scene : Scene) {
    }

    override fun removeFromScene(scene : Scene) {
    }

    override fun getCamera() : Camera { return returnToMode.getCamera() }
}

/* Every second, there's a chance to spawn a new npc if there aren't already the maximum number.
 * if an NPC will spawn, it'll be near the user, slightly more likely in the direction of the one room.
 */
class GameContainer() : InScene, IGameMode {
    val light = newLight(0xeeeeee)

    val aspect =
            kotlin.browser.window.innerWidth / kotlin.browser.window.innerHeight
    val camera = newPerspectiveCamera(35.0, aspect, 0.1, 10000.0)

    var targetCameraX = 0.0
    var targetCameraY = 3.0

    var curTime = 0.0

    val hero = Hero()
    var numFloors = 6
    var elevator = Elevator(1, numFloors)

    var floors = (1..numFloors).map({n -> Floor(n)}).toList()
    var buildingMap = StaticBuildMap(floors)

    var oneroom = randomFloorAndDoor()

    var wantNPCs = 10
    var nextSpawnTime = 0.0
    val npcs : MutableMap<Int, SpawnedNPC> = mutableMapOf()

    var nextId = 0

    var wanted = 0.0
    var caught = 0.0
    var badges = 3

    fun reset() {
        numFloors = 6
        elevator = Elevator(1, numFloors)
        hero.group.o.posiiton.x = 0.0
        hero.group.o.position.y = floorHeight
        hero.group.o.position.z = 2.0
        floors = (1..numFloors).map({n->Floor(n)}).toList()
        buildingMap = StaticBuildMap(floors)
        oneroom = randomFloorAndDoor()
        npcs.clear()
        wanted = 0.0
        caught = 0.0
        badges = 3
    }

    fun randomFloorAndDoor() : FloorAndDoor {
        return FloorAndDoor(Math.floor(rand() * buildingMap.floors.size), Math.floor(rand() * numDoors))
    }

    fun distanceToOneRoom(fd : FloorAndDoor) : Double {
        val oneRoomLocation = buildingMap.getDoor(oneroom.floor, oneroom.door)
        val fromLocation = buildingMap.getDoor(fd.floor, fd.door)
        return actorDistance(oneRoomLocation, fromLocation)
    }

    init {
        camera.o.position.x = 0
        camera.o.position.z = 15.0
        camera.o.lookAt(0, 1.0, 0.0)
    }

    override fun addToScene(scene : Scene) {
        scene.add(light)
        hero.addToScene(scene)
        elevator.addToScene(scene)
        floors.forEach { f -> f.addToScene(scene) }
    }

    override fun removeFromScene(scene : Scene) {
        scene.remove(light)
        hero.removeFromScene(scene)
        elevator.removeFromScene(scene)
        floors.forEach { f -> f.removeFromScene(scene) }
    }

    fun heroCurrentFloor() : Int {
        return Math.round((hero.group.o.position.y - 1.0) / 2.0)
    }

    fun spawnNPC(scene : Scene, pursuer : Boolean, floor : Int, door : Int, resname : String, behavior : NPCBehavior) {
        console.log("Spawn NPC", resname, "on", floor, "at", door)
        val floorObj = floors[floor]
        floorObj.toggleDoor(door)
        val spawned = SpawnedNPC(nextId++, NPC(resname), pursuer, behavior)
        spawned.n.group.o.position.x = floorObj.doors[door].o.position.x + 1
        spawned.n.group.o.position.y = 1.0 + (floor + 1) * floorHeight
        spawned.n.group.o.position.z = 2.0
        spawned.n.addToScene(scene)
        npcs.put(spawned.id, spawned)
    }

    fun loseLife() : ModeChange {
        badges = Math.max(0, badges - 1)
        if (badges == 0) {
            return gameOver()
        } else {
            return ModeChange(false, DieMode(this))
        }
    }

    fun gameOver() : ModeChange {
        reset()
        return ModeChange(false, GameOverMode(this))
    }

    fun handleNPCs(scene : Scene, m : GameUpdateMessage) : ModeChange {
        if (curTime > nextSpawnTime && npcs.size < wantNPCs) {
            nextSpawnTime = curTime + timeBetweenSpawns
            val fd = randomFloorAndDoor()
            val pursuer = (rand() * wanted) > 0.25
            val dist = distanceToOneRoom(fd)
            val participant = Math.log(0.5 / dist) > 1.0
            val closeness = if (participant) { 5.0 } else { 3.0 }
            spawnNPC(scene, pursuer, fd.floor, fd.door,
                    if (pursuer) { COP_RES } else { SKINNER_RES },
                    if (pursuer) { PursueHeroNPCBehavior() } else { RandomNPCBehavior(randomFloorAndDoor(), closeness) })
        }

        val toDespawn : MutableList<Int> = mutableListOf()
        var suspicious = 0
        var catching = 0
        npcs.forEach { npc ->
            val res = npc.value.b.update(buildingMap, elevator, hero, npc.value.n)
            npc.value.n.update(m.time)
            if (res.nearHero) {
                suspicious++
                val dist = if (npc.value.pursuer) { actorDistance(npc.value.n.group.o, hero.group.o) } else { 10000.0 }
                if (npc.value.pursuer && (dist < 1.5 || (dist < 3.5 && hero.inElevator()))) {
                    catching++
                }
            }
            if (res.despawn) {
                npc.value.n.removeFromScene(scene)
                toDespawn.add(npc.key)
            }
        }
        wanted = Math.max(0.0, Math.min(1.1, wanted + (m.time * (suspicious.toDouble() - 0.5) / 10.0)))
        caught = Math.max(0.0, caught + (m.time * (catching.toDouble() - 0.5) / 2.5))
        if (caught >= 1.0) {
            return loseLife()
        }
        toDespawn.forEach { x -> npcs.remove(x) }
        return ModeChange(false, null)
    }

    val stunDistance = 2.0

    fun enterDoor(floor : Int, door : Int) : ModeChange {
        return ModeChange(false, Room())
    }

    override fun update(scene : Scene, m : GameUpdateMessage) : ModeChange {
        if (m.tag == GameUpdateMessageTag.NewFrame) {
            curTime += m.time
            hero.update(m.time)
            targetCameraX = hero.group.o.position.x
            targetCameraY = hero.group.o.position.y + (floorHeight / 2.0)
            elevator.update(m.time)
            if (hero.inElevator()) {
                hero.group.o.position.y = elevator.group.o.position.y
            }
            camera.o.position.x = (camera.o.position.x + (targetCameraX * 3.0 * m.time)) / (1.0 + 3.0 * m.time)
            camera.o.position.y = (camera.o.position.y + (targetCameraY * 3.0 * m.time)) / (1.0 + 3.0 * m.time)
            camera.o.lookAt( 0, 1.0, 0.0 )
            light.o.position.set( camera.o.position.x, camera.o.position.y, 10000.0 )
            return handleNPCs(scene, m)
        } else if (m.tag == GameUpdateMessageTag.KeyDown) {
            when (m.key) {
                Key.C -> {
                    console.log("call elevator:",hero.group.o.position.x)
                    if (hero.group.o.position.x >= -1 &&
                        hero.group.o.position.x <= 1) {
                        elevator.callButton(heroCurrentFloor())
                    }
                }
                Key.S -> {
                    val heroFloor = heroCurrentFloor()
                    npcs.filter { e -> e.value.n.onFloor() == heroFloor }.filter { e -> actorDistance(e.value.n.group.o, hero.group.o) < stunDistance }.forEach { e -> e.value.n.stun() }
                }
                Key.Up -> {
                    console.log("enter elevator:",elevator.isOpen(), hero.inElevator())
                    if (hero.group.o.position.x >= -1 &&
                            hero.group.o.position.x <= 1 &&
                            elevator.isOpen() &&
                            elevator.onFloor() == hero.onFloor() &&
                            !hero.inElevator()) {
                        hero.getInElevator(elevator)
                    } else {
                        val floor = floors.get(hero.onFloor())
                        console.log("try open door on",floor)
                        if (floor != null) {
                            val door = floor.nearDoor(hero.group.o.position.x)
                            if (door != null && floor.doorOpen(door)) {
                                return enterDoor(floor.number, door)
                            }
                        }
                    }
                }
                Key.Down -> {
                    console.log("leave elevator:",elevator.isOpen(),hero.inElevator())
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
        return ModeChange(false, null)
    }

    override fun getCamera() : Camera { return camera }
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
        val game = GameContainer()
        stateStack.add(game)
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
            setBadges(game.badges)
            setWantedStars(game.wanted, game.caught)
            renderer.render( scene.o, game.getCamera().o )
            doUpdate(msg)
        })
    } catch (e : Exception) {
        if (error != null && errorContent != null) {
            doError(error, errorContent, "${e}");
        }
    }
}
