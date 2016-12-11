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

/* A random NPC starts from one room, goes to another room and despawns */
class RandomNPCBehavior(fd : FloorAndDoor, nearness : Double) : NPCBehavior {
    val nearness = nearness
    var inElevator = false
    val fd = fd
    override fun update(b : BuildingMap, e : Elevator, h : Hero, n : NPC) : NPCState {
        val heroFloor = fd.floor
        val npcFloor = n.onFloor()
        if (heroFloor != npcFloor) {
            val dir = b.directionOfElevator(npcFloor, n.group.o.position.x)
            if (dir == 0.0) {
                if (n.moving) {
                    n.endMove()
                }
                if (e.onFloor() == npcFloor && e.isOpen()) {
                    n.getInElevator(e)
                    inElevator = true
                    return NPCState(n.nearHero(h), true, false)
                }
            } else {
                n.beginMove(dir)
            }
            return NPCState(n.nearHero(h), false, false)
        } else {
            val door = b.getDoor(fd.floor, fd.door)
            if (e.isOpen() && inElevator) {
                inElevator = false
                n.leaveElevator(e)
            } else if (Math.abs(door.position.x - n.group.o.position.x + 1.0) < 5.0) {
                return NPCState(n.nearHero(h), false, true)
            } else if (door.position.x < n.group.o.position.x) {
                n.beginMove(-1.0)
            } else {
                n.beginMove(1.0)
            }
            return NPCState(n.nearHero(h,nearness), false, false)
        }
    }
}

class PursueHeroNPCBehavior : NPCBehavior {
    var inElevator = false
    override fun update(b : BuildingMap, e : Elevator, h : Hero, n : NPC) : NPCState {
        val heroFloor = h.onFloor()
        val npcFloor = n.onFloor()
        if (heroFloor != npcFloor) {
            val dir = b.directionOfElevator(npcFloor, n.group.o.position.x)
            if (dir == 0.0) {
                if (n.moving) {
                    n.endMove()
                }
                if (e.onFloor() == npcFloor && e.isOpen()) {
                    n.getInElevator(e)
                    inElevator = true
                    return NPCState(n.nearHero(h), true, false)
                }
            } else {
                n.beginMove(dir)
            }
            return NPCState(n.nearHero(h), false, false)
        } else {
            if (e.isOpen() && inElevator) {
                inElevator = false
                n.leaveElevator(e)
            } else if (h.group.o.position.x < n.group.o.position.x) {
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
    fun getDoor(floor : Int, door : Int) : dynamic
    fun directionOfElevator(floor : Int, x : Double) : Double
}

class StaticBuildMap(floors : List<Floor>) : BuildingMap {
    val floors = floors
    override fun getDoor(floor : Int, door : Int) : dynamic {
        return floors[floor].doors[door].o
    }
    override fun directionOfElevator(floor : Int, x : Double) : Double {
        if (x < -1) { return 1.0 }
        else if (x > 1) { return -1.0 }
        else { return 0.0 }
    }
}

class NPC(res : String) : InScene {
    val group = newGroup()

    var moving = false
    var movenext = 0.0
    var moveexpire = 0.0
    var movedir = 0.0
    val movetime = 0.1
    var movespeed = 2.0

    var stored : dynamic = null
    var mixer : dynamic = null

    val stunTime = 1.5

    var texture : dynamic = null
    var animator : TextureAnimator? = null

    init {
        console.log("npc", res)
        loadTexture(res, { texture ->
            this.texture = texture
            val animator = TextureAnimator(texture, 16, 2, 32, 60.0)
            this.animator = animator
            animator.play(AnimRestForward)
            val smesh = Sprite(texture, 1.0, 2.0)
            this.stored = smesh
            smesh.group.o.position.y += 1.0
            group.add(smesh.group)
        })
        group.o.position.z = 2.0
        group.o.position.y = floorHeight
    }

    fun inElevator() : Boolean {
        return group.o.position.z < 0
    }

    fun onFloor() : Int {
        return Math.round(group.o.position.y / floorHeight) - 1
    }

    fun beginMove(x : Double) {
        if (!moving) {
            moving = true
            moveexpire = 0.3
            movedir = x
        } else {
            movenext = x
        }
    }

    fun endMove() {
        moving = false
        movenext = 0.0
        moveexpire = movetime
    }

    var playing = false

    fun update(t : Double) {
        animator?.update(t)
        if (mixer != null) {
            mixer.update(t)
        }
        if (movedir != 0.0) {
            group.o.position.x += movedir * t * movespeed
        }
        if (moveexpire > 0) {
            moveexpire = Math.max(moveexpire - t, 0.0)
            if (moveexpire == 0.0) {
                movedir = 0.0
                moveexpire = -1.0
                moving = false
                val mn = movenext
                if (mn != 0.0) {
                    movenext = 0.0
                    beginMove(mn)
                }
            }
        }
        if (inElevator() || movedir == 0.0) {
            animator?.play(AnimRestForward)
        } else if (movedir < 0) {
            animator?.play(if (moving) { AnimWalkLeft } else { AnimRestLeft })
        } else {
            animator?.play(if (moving) { AnimWalkRight } else { AnimRestRight })
        }
    }

    fun getInElevator(e : Elevator) {
        e.occupy(
                { o ->
                    group.o.position.x = o.position.x
                    group.o.position.y = o.position.y
                    group.o.position.z = o.position.z - 1.0
                }
        )
    }

    fun leaveElevator(e : Elevator) {
        e.vacate()
        group.o.position.x = 0.0
        group.o.position.z = 2.0
    }

    fun stun() {
        moving = true
        movedir = 0.0
        moveexpire = stunTime
    }

    val toBeClose = 6.0

    fun nearHero(h : Hero, nearness : Double = -1.0) : Boolean {
        return h.onFloor() == onFloor() &&
                actorDistance(this.group.o, h.group.o) < if (nearness > 0.0) { nearness } else { toBeClose }
    }

    override fun addToScene(scene : Scene) {
        scene.add(group)
    }

    override fun removeFromScene(scene: Scene) {
        scene.remove(group)
    }
}

