/**
 * Created by arty on 12/10/16.
 */

package org.sample

class Room : InScene, IGameMode {
    override fun update(scene : Scene, m : GameUpdateMessage) : ModeChange {
        return ModeChange(false, null)
    }

    override fun addToScene(scene : Scene) {
    }

    override fun removeFromScene(scene : Scene) {
    }
}