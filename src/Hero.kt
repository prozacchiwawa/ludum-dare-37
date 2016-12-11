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
    var mixer : dynamic = null

    var hello : dynamic = null

    init {
        val loader = newJSONLoader()
        loader.load("SkinnerWalk2x1.json", { geometry, materials ->
            console.log(materials)
            (0..materials.length - 1).forEach({ i ->
                val material = materials[i]
                // material.skinning = true // Allows animation, bug
            })
            console.log("loaded", geometry)
            val smesh = Mesh(newSkinnedMesh(geometry, newMeshFaceMaterial(materials)))
            this.stored = smesh
            val holderGroup = newGroup()
            holderGroup.o.position.y = 0.0
            mixer = newAnimationMixer(smesh)
            hello = mixer.clipAction(geometry.animations[0])
            hello.enabled = true
            holderGroup.add(smesh)
            group.add(holderGroup)
            group.o.position.z = 2.0
            group.o.rotation.y = Math.PI / 2.0
            null
        })
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

    override fun addToScene(scene : Scene) {
        scene.add(group)
    }

    override fun removeFromScene(scene: Scene) {
        scene.remove(group)
    }
}
