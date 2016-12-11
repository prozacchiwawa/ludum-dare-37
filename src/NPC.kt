/**
 * Created by arty on 12/10/16.
 */

package org.sample

class NPCState(nearHero : Boolean, inElevator : Boolean, despawn : Boolean) {
    val nearHero = nearHero
    val inElevator = inElevator
    val despawn = despawn
}

interface NPCBehavior {
    fun update(b : BuildingMap, e : Elevator, h : Hero, n : NPC) : NPCState
}

class PursueHeroNPCBehavior : NPCBehavior {
    override fun update(b : BuildingMap, e : Elevator, h : Hero, n : NPC) : NPCState {
        val heroFloor = h.onFloor()
        val npcFloor = n.onFloor()
        if (heroFloor != npcFloor) {
            val dir = b.directionOfElevator(npcFloor, n.group.o.position.x)
            if (dir == 0.0) {
                if (n.moving) {
                    n.endMove()
                }
                if (e.isOpen()) {
                    n.getInElevator(e)
                    return NPCState(n.nearHero(h), true, false)
                }
            } else {
                n.beginMove(dir)
            }
            return NPCState(n.nearHero(h), false, false)
        } else {
            if (h.group.o.position.x < n.group.o.position.x) {
                n.beginMove(-1.0)
            } else {
                n.beginMove(1.0)
            }
            return NPCState(n.nearHero(h), false, false)
        }
    }
}

/* Graceful despawn:
 * Go toward the elevator,
 * call it,
 * ride to the target floor
 * choose a door
 * move toward that door (left or right)
 * despawn at the door
 */
class DespawnBehavior(floor : Int, door : Int) {
}

interface BuildingMap {
    fun directionOfElevator(floor : Int, x : Double) : Double
}

class StaticBuildMap(floors : Int) : BuildingMap {
    val floors = floors
    override fun directionOfElevator(floor : Int, x : Double) : Double {
        if (x < -1) { return 1.0 }
        else if (x > 1) { return -1.0 }
        else { return 0.0 }
    }
}

class NPC(res : ResBundle) : InScene {
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
        val smesh = Mesh(newSkinnedMesh(res.geometry, newMeshFaceMaterial(res.materials)))
        this.stored = smesh
        val holderGroup = newGroup()
        holderGroup.o.position.y = 0.0
        mixer = newAnimationMixer(smesh)
        hello = mixer.clipAction(res.geometry.animations[0])
        hello.enabled = true
        holderGroup.add(smesh)
        group.add(holderGroup)
        group.o.position.z = 2.0
        group.o.rotation.y = Math.PI / 2.0
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
        if (inElevator() || movedir == 0.0) {
            group.o.rotation.y = 0
        } else if (movedir < 0) {
            group.o.rotation.y = -Math.PI / 2.0
        } else {
            group.o.rotation.y = Math.PI / 2.0
        }
    }

    fun getInElevator(e : Elevator) {
        e.occupy(
                { o ->
                    group.o.position.x = o.position.x
                    group.o.position.y = o.position.y + 1.0
                    group.o.position.z = o.position.z
                }
        )
    }

    fun leaveElevator(e : Elevator) {
        e.vacate()
        group.o.position.x = 0.0
        group.o.position.z = 0.0
    }

    val toBeClose = 6.0

    fun nearHero(h : Hero) : Boolean {
        return actorDistance(this.group.o, h.group.o) < toBeClose
    }

    override fun addToScene(scene : Scene) {
        scene.add(group)
    }

    override fun removeFromScene(scene: Scene) {
        scene.remove(group)
    }
}

