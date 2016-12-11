/**
 * Created by arty on 12/11/16.
 */

package org.sample

var wanted = 0.0
var caught = 0.0
var badges = 3

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

    fun reset(scene : Scene) {
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
        badges = Math.max(0, badges - 1)
        if (badges == 0) {
            return gameOver(scene)
        } else {
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
        wanted = Math.max(0.0, Math.min(1.1, wanted + (m.time * (suspicious.toDouble() - 0.5) / 10.0)))
        caught = Math.max(0.0, caught + (m.time * (catching.toDouble() - 0.5) / 2.5))
        if (caught >= 1.0) {
            return loseLife(scene)
        }
        toDespawn.forEach { x -> npcs.remove(x) }
        return ModeChange(false, null)
    }

    val stunDistance = 2.0

    fun enterDoor(floor : Int, door : Int) : ModeChange {
        return ModeChange(false, Room(floor, door, hero, camera))
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
