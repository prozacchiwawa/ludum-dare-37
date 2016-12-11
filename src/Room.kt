/**
 * Created by arty on 12/10/16.
 */

package org.sample

class Room : InScene, IGameMode {
    val light = newLight(0xffeeaa)
    val camera = newPerspectiveCamera(0.35, kotlin.browser.window.innerWidth / kotlin.browser.window.innerHeight, 0.001, 10000.0)

    init {
        light.o.position.x = 0
        light.o.position.y = 1.0
        light.o.position.z = 0
    }

    override fun update(scene : Scene, m : GameUpdateMessage) : ModeChange {
        return ModeChange(false, null)
    }

    override fun addToScene(scene : Scene) {
        scene.add(light)
    }

    override fun removeFromScene(scene : Scene) {
        scene.remove(light)
    }

    override fun getCamera() : Camera { return camera }
}