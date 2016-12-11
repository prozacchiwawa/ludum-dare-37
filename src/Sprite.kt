/**
 * Created by arty on 12/10/16.
 */

package org.sample

/*
	Three.js "tutorials by example"
	Author: Lee Stemkoski
	Date: July 2013 (three.js v59dev)
*/

fun loadTexture(name : String, loaded : (tex : dynamic) -> Unit) : dynamic {
    return js("(function(name,loaded) { var tl = new THREE.TextureLoader(); var tex = tl.load( name, loaded ); return tex })")(name, loaded)
}

class Sprite(texture : dynamic, x : Double, y : Double) {
    val runnerTexture = texture
    val runnerMaterial = newMeshBasicMaterial(runnerTexture)
    val runnerGeometry = newPlaneGeometry(x, y, 1, 1)
    val group = newMesh(runnerGeometry, runnerMaterial)
}
