/**
 * Created by arty on 12/11/16.
 */

package org.sample

class MazeWall(x : Int, z : Int, upper : Boolean) {
    val x = x
    val z = z
    val upper = upper
}

class DisjointSet<A>(data : Map<A, Set<A>>) {
    val data : Map<A, Set<A>> = data
    fun add(target : A, makeLike : A) : DisjointSet<A> {
        val setLike = data.getOrElse(makeLike, { setOf() })
        val setLikeWithTarget = setLike.plus(target)
        val dataWithTargetAdded = setLikeWithTarget.fold(data,{ m,e -> m.plus(e.to(setLikeWithTarget)) })
        return DisjointSet(dataWithTargetAdded)
    }
    fun get(target : A) : Set<A> {
        return data.getOrElse(target, { setOf() })
    }
    fun union(target : A, makeLike : A) : DisjointSet<A> {
        val targetSet = data.getOrElse(target, { setOf() })
        val likeSet = data.getOrElse(makeLike, { setOf() })
        val jointSet = targetSet.union(likeSet)
        return DisjointSet(jointSet.fold(data, { d,e -> d.plus(e.to(jointSet)) }))
    }
}

/*
 * The doors connect to a maze behind each wing of the building.
 * There are maze walls and clues in a maze.
 */
class Maze(x : Double, y : Double, doorX : Double, doorY : Double, width : Double, height : Double) {
    val x = x
    val y = y
    val doorX = doorX
    val doorY = doorY
    val width = width
    val height = height

    fun stepWidth() : Int { return Math.floor(width / 2.0) }
    fun stepHeight() : Int { return Math.floor(height / 2.0) }

    val walls = constructWalls()

    fun bothWalls(x : Int, z : Int) : Set<MazeWall> {
        return setOf(MazeWall(x, z, false), MazeWall(x, z, true))
    }

    fun fullRow(z : Int) : Set<MazeWall> {
        return (0..stepWidth()-1).fold(setOf(), { s,x -> s.union(bothWalls(x,z)) })
    }

    fun allWalls() : Set<MazeWall> {
        return (0..stepHeight()-1).fold(setOf(), { s,z -> s.union(fullRow(z)) })
    }

    fun discardExterior(s : Set<MazeWall>) : Set<MazeWall> {
        val withoutTopWall = (0..stepWidth()-1).fold(s, { s,e -> s.minus(MazeWall(e,stepHeight()-1,true)) })
        return (0..stepHeight()-1).fold(s, { s,e -> s.minus(MazeWall(stepWidth()-1,e,false)) })
    }

    fun shuffle(walls : MutableList<MazeWall>) : MutableList<MazeWall> {
        for (i in 0..10 * walls.size) {
            val idx1 = Math.floor(rand() * walls.size)
            val idx2 = Math.floor(rand() * walls.size)
            val t = walls[idx1]
            walls[idx1] = walls[idx2]
            walls[idx2] = t
        }
        return walls
    }

    fun neighborsOfWall(mw : MazeWall) : Pair<Pair<Int,Int>,Pair<Int,Int>> {
        return Pair<Int,Int>(mw.x,mw.z).to(
                if (mw.upper) {
                    Pair<Int,Int>(mw.x,mw.z+1)
                } else {
                    Pair<Int,Int>(mw.x+1,mw.z)
                }
        )
    }

    // Randomized Krustal
    fun constructWalls() : Set<MazeWall> {
        // Set up which set datastructure
        val disjointSet = DisjointSet((0..stepWidth() * stepHeight() - 1).map {
            i ->
            val coord = Pair<Int,Int>(i % stepWidth(), i / stepWidth())
            coord.to(setOf(coord))
        }.toMap())
        val entrySet = Pair<Int,Int>(Math.floor((doorX - x) / 2.0), 0)
        console.log("enterHere",entrySet.toString())
        val fullSet = discardExterior(allWalls())
        console.log("allPossibleWalls",fullSet.toList().toString())
        // We choose the square adjacent to the door
        val shuffledWalls = shuffle(fullSet.toMutableList())
        return shuffledWalls.fold(fullSet.to(disjointSet), { wsAndDs,w ->
            val ns = neighborsOfWall(w)
            val firstSet = wsAndDs.second.get(ns.first)
            val secondSet = wsAndDs.second.get(ns.second)
            if (firstSet != secondSet) {
                wsAndDs.first.minus(w).to(wsAndDs.second.union(ns.first, ns.second))
            } else {
                wsAndDs
            }
        }).first
    }
}