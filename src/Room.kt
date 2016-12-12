/**
 * Created by arty on 12/10/16.
 */

package org.sample

val maxProps = 3

fun propRes() : String {
    if (Math.floor(rand() * 2) > 0) {
        return TYPEWRITER_RES
    } else {
        return FILECABINET_RES
    }
}

fun clueText(floor : Int, room : Int, maxFloor : Int) : String {
    val r = Math.round(Math.sqrt(rand() * 100))
    val randfloor1Lowest = Math.max(maxFloor - 5, floor - 2)
    val randfloor1Highest = Math.min(maxFloor, floor + 2)
    val randfloor1 = Math.floor(rand() * (randfloor1Highest - randfloor1Lowest)) + randfloor1Lowest
    val randroom1Lowest = Math.max(5, room - 2)
    val randroom1Highest = Math.min(8, room + 1)
    val randroom1 = Math.floor(rand() * (randroom1Highest - randroom1Lowest)) + randroom1Lowest
    val wing = if (room >= 4) { "east" } else { "west" }
    if (r > 9.5) {
        return "I can't take it anymore ... You know who this is and what I want.  Meet me in room ${room+1} on ${floor+1} or we'll both regret it."
    } else if (r > 8.5) {
        return "Our associates have noticed your progress in the ${floor+1} lab.  We look forward to continued progress."
    } else if (r > 0.6) {
        return "... Construction noise near suite ${room+1} on ${randfloor1} continues to be a disrupting influence on our work..."
    } else if (r > 0.3) {
        return "Work Order: Issue(s) regarding dust and odors from the ventilation system in room ${randroom1} on floor ${randfloor1}"
    } else {
        return "Want to grab some coffee?  The power outlets are humming in a wierd way over in the ${wing} wing and we can't plug in anything"
    }
}

class Room(floor : Int, door : Int, oneroom : FloorAndDoor, maxFloor : Int, hero : Hero, camera : Camera) : InScene, IGameMode {
    val light = newLight(0xffeeaa)
    val camera = camera
    val hero = hero

    val floor = floor
    val door = door
    val maxFloor = maxFloor
    val oneroom = oneroom

    var targetCameraX = 0.0
    var targetCameraY = 0.0

    var curTime = 0.0
    var lastTime = 0.0

    var id = 0

    val stunDistance = 2.5
    val lastHeroX = hero.group.o.position.x
    val lastHeroY = hero.group.o.position.y
    val lastHeroZ = hero.group.o.position.z
    val lastCameraX = camera.o.position.x

    val npcs : MutableMap<Int, SpawnedNPC> = mutableMapOf()
    val props : MutableList<Prop> = mutableListOf()

    val backWallGeom = newBoxGeometry(20.0, floorHeight, 0.2)
    val backWallMaterial = newMeshLambertMaterial(0xc9c0bb)
    val floorGeom = newBoxGeometry(100.0, 0.2, 2.0)
    val floorMaterial = newMeshLambertMaterial(0x006600)
    val floorMesh = newMesh(floorGeom, floorMaterial)
    val doorGeom = newBoxGeometry(1.0, 2.2, 0.15)
    val doorMaterial = newMeshLambertMaterial(0x000055)
    val doorHandleGeom = newBoxGeometry(0.1, 0.1, 0.1)
    val doorHandleMaterial = newMeshLambertMaterial(0xffffff)

    var doors : MutableList<Group> = mutableListOf()
    val group = newGroup()

    val doorOpenRotation = -0.2

    init {
        val backWallLeft = newMesh(backWallGeom, backWallMaterial)
        backWallLeft.o.position.x = 0
        backWallLeft.o.position.y = floorHeight / 2.0
        backWallLeft.o.position.z = 0.0
        group.add(backWallLeft)
        floorMesh.o.position.z = 1.0
        group.add(floorMesh)
        group.o.position.y = floor * floorHeight
        doors = (0..1).map({ n ->
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
            d.o.position.x = 0
            d
        }).toMutableList()
        doors.forEach({ d -> group.add(d) })
        light.o.position.x = 0
        light.o.position.y = 1.0 + floor * floorHeight
        light.o.position.z = -100.0

        val numProps = Math.round(rand() * maxProps)
        (0..numProps-1).forEach { n ->
            val res = propRes()
            val useDims = dims.getOrElse(res, { Pair<Double,Double>(1.0,2.0) })
            val prop = Prop(res, clueText(oneroom.floor, oneroom.door, maxFloor), useDims.first, useDims.second)
            prop.group.o.position.x = (rand() * 15.0) - 7.5
            prop.group.o.position.y = floor * floorHeight
            prop.group.o.position.z = 1.0
            props.add(prop)
        }
        val numNPCs = Math.round(rand() * 5.0)
        (0..numNPCs-1).forEach { n ->
            val npc = SpawnedNPC(id++, NPC(SKINNER_RES), false, StaticNPCBehavior())
            npc.n.group.o.position.x = (rand() * 15.0) - 7.5
            npc.n.group.o.position.y = floor * floorHeight
            npc.n.group.o.position.z = 1.2
            console.log(npc.n.group.o.position)
            npcs.put(npc.id, npc)
        }
        if (rand() < 0.025) {
            props.add(Prop(KEYCARD_RES, "*", 0.5, 0.5))
        }
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

    fun doorOpen(d : Int) : Boolean {
        val door = doors.get(d)
        return door != null && door.o.rotation.y < 0
    }

    override fun addToScene(scene : Scene) {
        scene.add(light)
        scene.add(group)
        hero.addToScene(scene)
        props.forEach { p -> p.addToScene(scene) }
        npcs.forEach { n -> console.log("scene", n.key); n.value.n.addToScene(scene) }
        hero.group.o.position.x = 1.0
        camera.o.position.x = 1.0
    }
    override fun removeFromScene(scene : Scene) {
        scene.remove(light)
        scene.remove(group)
        hero.removeFromScene(scene)
        props.forEach { p -> p.removeFromScene(scene) }
        npcs.forEach { n -> n.value.n.removeFromScene(scene) }
        hero.group.o.position.x = lastHeroX
        hero.group.o.position.y = lastHeroY
        hero.group.o.position.z = lastHeroZ
        camera.o.position.x = lastCameraX
    }

    fun handleNPCs(scene : Scene, m : GameUpdateMessage) : ModeChange {
        wanted = Math.max(0.0, Math.min(maxWanted, wanted + (m.time * (npcs.size.toDouble() - 0.10) / 10.0)))
        npcs.forEach { n -> n.value.n.update(m.time) }
        return ModeChange(false, null)
    }

    fun enterDoor() : ModeChange {
        return ModeChange(true, null)
    }

    override fun update(scene : Scene, m : GameUpdateMessage) : ModeChange {
        if (m.tag == GameUpdateMessageTag.NewFrame) {
            curTime += m.time
            hero.update(m.time)
            targetCameraX = hero.group.o.position.x
            targetCameraY = hero.group.o.position.y + (floorHeight / 2.0)
            camera.o.position.x = (camera.o.position.x + (targetCameraX * 3.0 * m.time)) / (1.0 + 3.0 * m.time)
            camera.o.position.y = (camera.o.position.y + (targetCameraY * 3.0 * m.time)) / (1.0 + 3.0 * m.time)
            camera.o.lookAt( 0, 1.0, 0.0 )
            light.o.position.set( camera.o.position.x, camera.o.position.y, 10000.0 )
            return handleNPCs(scene, m)
        } else if (m.tag == GameUpdateMessageTag.KeyDown) {
            when (m.key) {
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
                    val floor = this
                    console.log("try open door on",floor)
                    if (floor != null) {
                        val door = floor.nearDoor(hero.group.o.position.x)
                        if (door != null) {
                            return enterDoor()
                        }
                    }
                }
                Key.Left -> { if (!hero.inElevator()) { hero.beginMove(-1.0) } }
                Key.Right -> { if (!hero.inElevator()) { hero.beginMove(1.0) } }
                Key.Space -> {
                    val floor = this
                    console.log("try open door on",floor)
                    if (floor != null) {
                        val door = floor.nearDoor(hero.group.o.position.x)
                        console.log("try open door",door,"on",floor)
                        if (door != null) {
                            console.log("toggle",door,"on",floor)
                            floor.toggleDoor(door)
                        }
                    }
                    val takeObject = props.filter { e -> e.nearHero(hero) }.firstOrNull()
                    if (takeObject != null) {
                        if (takeObject.clue == "*") {
                            props.remove(takeObject)
                            takeObject.removeFromScene(scene)
                            badges++
                        } else {
                            takeObject.taken()
                            clues.add(takeObject.clue)
                            return ModeChange(false, ClueMode(takeObject.clue, this))
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