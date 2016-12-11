/**
 * Created by arty on 12/10/16.
 */

package org.sample

class Room(floor : Int, door : Int, hero : Hero, camera : Camera) : InScene, IGameMode {
    val light = newLight(0xffeeaa)
    val camera = camera
    val hero = hero

    val floor = floor
    val door = door

    var targetCameraX = 0.0
    var targetCameraY = 0.0

    var curTime = 0.0
    var lastTime = 0.0

    val stunDistance = 2.5
    val lastHeroX = hero.group.o.position.x
    val lastHeroY = hero.group.o.position.y
    val lastHeroZ = hero.group.o.position.z
    val lastCameraX = camera.o.position.x

    val npcs : MutableMap<Int, SpawnedNPC> = mutableMapOf()

    val backWallGeom = newBoxGeometry(20.0, floorHeight, 0.2)
    val backWallMaterial = newMeshLambertMaterial(0x4f3e24)
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
        scene.add(hero.group)
        hero.group.o.position.x = 1.0
        camera.o.position.x = 1.0
    }
    override fun removeFromScene(scene : Scene) {
        scene.remove(light)
        scene.remove(group)
        scene.remove(hero.group)
        hero.group.o.position.x = lastHeroX
        hero.group.o.position.y = lastHeroY
        hero.group.o.position.z = lastHeroZ
        camera.o.position.x = lastCameraX
    }

    fun handleNPCs(scene : Scene, m : GameUpdateMessage) : ModeChange {
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
                        if (door != null && floor.doorOpen(door)) {
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