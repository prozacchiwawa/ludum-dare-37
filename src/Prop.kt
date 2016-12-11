/**
 * Created by arty on 12/11/16.
 */

package org.sample

class Prop(res : String, clue : String, width : Double) : InScene {
    val group = newGroup()

    var stored : dynamic = null

    var texture : dynamic = null
    var animator : TextureAnimator? = null
    val clue = clue
    val width = width

    init {
        loadTexture(res, { texture ->
            this.texture = texture
            val animator = TextureAnimator(texture, 1, 1, 1, 60.0)
            this.animator = animator
            animator.play(listOf(0))
            val smesh = Sprite(texture, width, 2.0)
            this.stored = smesh
            smesh.group.o.position.y += 0.8
            group.add(smesh.group)
        })
        group.o.position.z = 2.0
        group.o.position.y = floorHeight
    }

    var wasTaken = false

    fun taken() {
        wasTaken = true
    }

    fun onFloor() : Int {
        return Math.round(group.o.position.y / floorHeight) - 1
    }

    fun update(t : Double) {
        animator?.update(t)
    }

    fun nearHero(h : Hero) : Boolean {
        val haveDist = actorDistance(this.group.o, h.group.o)
        console.log("nearHero", haveDist, "wasTaken", wasTaken, "floors", h.onFloor(), onFloor())
        return !wasTaken && h.onFloor() == onFloor() && haveDist < 2.0
    }

    override fun addToScene(scene : Scene) {
        scene.add(group)
    }

    override fun removeFromScene(scene: Scene) {
        scene.remove(group)
    }
}

