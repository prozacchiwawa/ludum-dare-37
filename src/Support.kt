/**
 * Created by arty on 12/10/16.
 */

package org.sample

class ResBundle(geometry : dynamic, materials : dynamic) {
    val geometry : dynamic = geometry
    val materials : dynamic = materials
}

interface InScene {
    fun addToScene(scene : Scene)
    fun removeFromScene(scene : Scene)
}

fun actorDistance(n : dynamic, h : dynamic) : Double {
    val dx : Double = n.position.x - h.position.x
    val dy : Double = n.position.y - h.position.y
    val dz : Double = n.position.z - h.position.z
    return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz))
}