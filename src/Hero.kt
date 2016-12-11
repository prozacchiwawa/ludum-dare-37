/**
 * Created by arty on 12/10/16.
 */

package org.sample

class Hero : InScene {
    val group = newGroup()

    var moving = false
    var moveexpire = 0.0
    var movedir = 0.0
    val movetime = 0.1
    var movespeed = 2.0

    var stored : dynamic = null

    var texture : dynamic = null
    var animator : TextureAnimator? = null

    init {
        loadTexture("KarlSheet.png", { texture ->
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
        return group.o.position.z < 0 && group.o.position.x > -1 && group.o.position.x < 1
    }

    fun inDoor() : Boolean {
        return group.o.position.z < 0 && !inElevator()
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
        animator?.update(t)
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
        if (movedir == 0.0) {
            animator?.play(AnimRestForward)
        } else if (movedir > 0) {
            animator?.play(if (moving) { AnimWalkRight } else { AnimRestRight })
        } else {
            animator?.play(if (moving) { AnimWalkLeft } else { AnimRestLeft })
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

    override fun addToScene(scene : Scene) {
        scene.add(group)
    }

    override fun removeFromScene(scene: Scene) {
        scene.remove(group)
    }
}
