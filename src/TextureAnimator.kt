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

    var start : Int = 0
    var end : Int = numTiles

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
            if (this.currentTile < start) {
                this.currentTile = start
            }
            this.currentTile++;
            if (this.currentTile > this.end) {
                this.currentTile = start;
            }
            var currentColumn = this.currentTile % this.tilesHorizontal;
            texture.offset.x = currentColumn.toDouble() / this.tilesHorizontal.toDouble();
            var currentRow = Math.floor( this.currentTile / this.tilesHorizontal );
            texture.offset.y = currentRow.toDouble() / this.tilesVertical.toDouble();
        }
    };

    fun play(start : Int, end : Int) {
        this.start = start
        this.end = end
    }
}

