/**
 * Created by arty on 12/10/16.
 */

package org.sample

val floorHeight = 3.0
val numDoors = 9

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
        doors = (0..numDoors-1).map({ n ->
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
