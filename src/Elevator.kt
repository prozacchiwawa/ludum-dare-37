/**
 * Created by arty on 12/10/16.
 */

package org.sample

class Elevator(min : Int, max : Int) : InScene {
    var floor = min
    var direction = false
    var open = -1.0
    val min = min
    val max = max
    val espeed = 1.0
    val openTime = 2.0

    var occupied : ((o : dynamic) -> Unit)? = null

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

    fun changeDirection(dir : Boolean) {
        direction = dir
    }

    fun onFloor() : Int {
        return Math.round(group.o.position.y / floorHeight) - 1
    }

    fun isOpen() : Boolean {
        return open > 0.0
    }

    fun occupy(f : (o : dynamic) -> Unit) {
        val occ = occupied
        if (occ == null) {
            occupied = f
            f(group.o)
        }
    }

    fun vacate() {
        occupied = null
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
        val occ = occupied
        if (occ != null) {
            occ(group.o)
        }
    }
}
