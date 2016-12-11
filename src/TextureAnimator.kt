/**
 * Created by arty on 12/10/16.
 */

package org.sample

/*
	Three.js "tutorials by example"
	Author: Lee Stemkoski
	Date: July 2013 (three.js v59dev)
*/

class TextureAnimator(texture : dynamic, tilesHoriz : Int, tilesVert : Int, numTiles : Int, tileDispDuration : Double)
{
    val texture = texture
    val tilesHorizontal = tilesHoriz
    val tilesVertical = tilesVert
    val numberOfTiles = numTiles

    var alist : List<Int> = listOf(0)

    // note: texture passed by reference, will be updated by the update function.

    // how many images does this spritesheet contain?
    //  usually equals tilesHoriz * tilesVert, but not necessarily,
    //  if there at blank tiles at the bottom of the spritesheet.
    val tileDisplayDuration = tileDispDuration

    var currentDisplayTime = 0.0
    var currentTile = 0

    init {
        val repeatWrapping = kotlin.browser.window.asDynamic().THREE.RepeatWrapping
        texture.wrapS = repeatWrapping
        texture.wrapT = repeatWrapping
        texture.repeat.set(1.0 / this.tilesHorizontal, 1.0 / this.tilesVertical)
    }

    fun update(t : Double) {
        this.currentDisplayTime += (t * 1000.0)
        while (this.currentDisplayTime > this.tileDisplayDuration) {
            this.currentDisplayTime -= this.tileDisplayDuration;
            if (this.currentTile < 0) {
                this.currentTile = 0
            }
            this.currentTile++;
            if (this.currentTile >= this.alist.size) {
                this.currentTile = 0
            }
            val tile = this.alist[this.currentTile]
            var currentColumn = tile % this.tilesHorizontal;
            texture.offset.x = currentColumn.toDouble() / this.tilesHorizontal.toDouble();
            var currentRow = Math.floor( tile / this.tilesHorizontal );
            texture.offset.y = currentRow.toDouble() / this.tilesVertical.toDouble();
        }
    };

    fun play(start : Int, end : Int) {
        this.alist = (start..end - 1).toList()
    }

    fun play(alist : List<Int>) {
        this.alist = alist
    }
}

