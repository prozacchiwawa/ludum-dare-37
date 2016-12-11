/**
 * Created by arty on 12/11/16.
 */

package org.sample

var wanted = 0.0
var caught = 0.0
var badges = 3

val maxWanted = 1.7

val clues : MutableSet<String> = mutableSetOf()

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
    val genroom : MutableMap<Pair<Int,Int>, Room> = mutableMapOf()

    var nextId = 0

    fun reset(scene : Scene) {
        elevator.reset()
        numFloors = 6
        elevator = Elevator(1, numFloors)
        hero.group.o.posiiton.x = 0.0
        hero.group.o.position.y = floorHeight
        hero.group.o.position.z = 2.0
        floors = (1..numFloors).map({n->Floor(n)}).toList()
        buildingMap = StaticBuildMap(floors)
        oneroom = randomFloorAndDoor()
        removeFromScene(scene)
        npcs.clear()
        wanted = 0.0
        caught = 0.0
        badges = 3
        addToScene(scene)
    }

    fun randomFloorAndDoor(floorPref : Int = -1) : FloorAndDoor {
        if (floorPref < 0 || rand() < 0.5) {
            return FloorAndDoor(Math.floor(rand() * buildingMap.floors.size), Math.floor(rand() * numDoors))
        } else {
            return FloorAndDoor(floorPref, Math.floor(rand() * numDoors))
        }
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
        npcs.forEach { e -> e.value.n.addToScene(scene) }
    }

    override fun removeFromScene(scene : Scene) {
        scene.remove(light)
        hero.removeFromScene(scene)
        elevator.removeFromScene(scene)
        floors.forEach { f -> f.removeFromScene(scene) }
        npcs.forEach { e -> e.value.n.removeFromScene(scene) }
    }

    fun spawnNPC(scene : Scene, pursuer : Boolean, floor : Int, door : Int, resname : String, behavior : NPCBehavior) {
        console.log("Spawn NPC", resname, "on", floor, "at", door)
        val floorObj = floors[floor]
        floorObj.toggleDoor(door)
        val spawned = SpawnedNPC(nextId++, NPC(resname), pursuer, behavior)
        spawned.n.group.o.position.x = floorObj.doors[door].o.position.x + 1
        spawned.n.group.o.position.y = (floor + 1) * floorHeight
        spawned.n.group.o.position.z = 2.0
        spawned.n.addToScene(scene)
        npcs.put(spawned.id, spawned)
    }

    fun loseLife(scene : Scene) : ModeChange {
        elevator.reset()
        badges = Math.max(0, badges - 1)
        if (badges == 0) {
            wanted = 0.0
            caught = 0.0
            return gameOver(scene)
        } else {
            removeFromScene(scene)
            wanted = 0.0
            caught = 0.0
            npcs.clear()
            addToScene(scene)
            hero.group.o.position.x = 0.0
            hero.group.o.position.y = floorHeight
            hero.group.o.position.z = 1.0
            camera.o.position.x = 0.0
            camera.o.position.y = floorHeight + 1.0
            camera.o.position.z = 15.0
            targetCameraX = 0.0
            targetCameraY = floorHeight + 1.0
            return ModeChange(false, DieMode(this))
        }
    }

    fun gameOver(scene : Scene) : ModeChange {
        reset(scene)
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

        val prevWanted = wanted
        wanted = Math.max(0.0, Math.min(maxWanted, wanted + (m.time * (suspicious.toDouble() - 0.15) / 12.0)))
        caught = Math.max(0.0, caught + (m.time * (catching.toDouble() - 0.2) / 2.5))
        if (caught >= 1.0) {
            return loseLife(scene)
        }
        if (prevWanted >= 0.2 && wanted < 0.2) {
            val stopPursuit = npcs.filter { e -> e.value.pursuer && actorDistance(hero.group.o, e.value.n.group.o) > 10.0 / (1.0 - Math.min(0.99, wanted)) }

            stopPursuit.forEach { e ->
                npcs.put(e.key, SpawnedNPC(e.value.id, e.value.n, e.value.pursuer, RandomNPCBehavior(randomFloorAndDoor(), 3.0)))
            }
        }

        toDespawn.forEach { x -> npcs.remove(x) }
        return ModeChange(false, null)
    }

    val stunDistance = 2.0

    fun enterDoor(scene : Scene, floor : Int, door : Int) : ModeChange {
        console.log("want",oneroom,"have",floor,door)
        if (floor == oneroom.floor + 1 && door == oneroom.door) {
            return ModeChange(false, WinMode(vicText, "win", this))
        }
        val wantedStars = npcs.filter { e ->
            e.value.n.onFloor() == hero.onFloor() && actorDistance(e.value.n.group.o, hero.group.o) < 7.0
        }.count()
        caught += rand() * wantedStars / 2.0
        if (caught >= 1.0) {
            loseLife(scene)
        } else {
            caught = 0.0
        }
        wanted = Math.min(maxWanted, wanted + wantedStars.toDouble() / 5.0)
        val oldRoom = genroom.get(Pair<Int,Int>(floor, door))
        if (oldRoom != null) {
            return ModeChange(false, oldRoom)
        } else {
            val room = Room(floor, door, numFloors, hero, camera)
            genroom.put(Pair<Int,Int>(floor, door), room)
            return ModeChange(false, room)
        }
    }

    override fun update(scene : Scene, m : GameUpdateMessage) : ModeChange {
        if (m.tag == GameUpdateMessageTag.NewFrame) {
            curTime += m.time
            hero.update(m.time)
            val floor = floors[hero.onFloor()]
            if (hero.group.o.position.x < floor.floorLeftExt) {
                hero.group.o.position.x = floor.floorLeftExt
            }
            if (hero.group.o.position.x > floor.floorRightExt) {
                hero.group.o.position.x = floor.floorRightExt
            }
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
                        elevator.callButton(hero.onFloor())
                    }
                }
                Key.S -> {
                    val heroFloor = hero.onFloor()
                    val onFloor = npcs.filter { e -> e.value.n.onFloor() == heroFloor }
                    val closeEnoughToStun = onFloor.filter { e -> actorDistance(e.value.n.group.o, hero.group.o) < stunDistance }
                    console.log("Close", closeEnoughToStun)
                    closeEnoughToStun.forEach { e ->
                        console.log("stun",e.value.n)
                        e.value.n.stun()
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
                    } else {
                        val floor = floors.get(hero.onFloor())
                        console.log("try open door on",floor)
                        if (floor != null) {
                            val door = floor.nearDoor(hero.group.o.position.x)
                            if (door != null && floor.doorOpen(door)) {
                                return enterDoor(scene, floor.number, door)
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
