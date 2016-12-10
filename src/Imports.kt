/**
 * Created by arty on 12/10/16.
 */

package org.sample

class Geometry(o : dynamic) {
    val o = o
}

fun newBoxGeometry(x : Double, y : Double, z : Double) : Geometry {
    return Geometry(js("(function (x,y,z) { return new THREE.BoxGeometry(x,y,z); })")(x,y,z))
}

class Material(o : dynamic) {
    val o = o
}

fun newMeshLambertMaterial(color : Int) : Material {
    return Material(js("(function (c) { return new THREE.MeshLambertMaterial({color: c}); })")(color))
}

class Mesh(o : dynamic) {
    val o = o
}

fun newMesh(geom : dynamic, material : dynamic) : Mesh {
    return Mesh(js("(function(g,m) { return new THREE.Mesh(g,m); })")(geom.o,material.o))
}

class Camera(o : dynamic) {
    val o = o
}

fun newPerspectiveCamera(fl : Double, aspect : Double, minZ : Double, maxZ : Double) : Camera {
    return Camera(js("(function(fl,a,nz,xz) { return new THREE.PerspectiveCamera(fl, a, nz, xz) })")(fl, aspect, minZ, maxZ))
}

fun newObjectLoader() : dynamic {
    return js("new THREE.ObjectLoader()")
}

fun newJSONLoader() : dynamic {
    return js("new THREE.JSONLoader()")
}

fun newAnimationMixer(mesh : Mesh) : dynamic {
    return js("(function (mesh) { return new THREE.AnimationMixer(mesh) })")(mesh.o)
}

fun newSkinnedMesh(geometry : dynamic, material : dynamic) : dynamic {
    return js("(function (geometry,material) { return new THREE.SkinnedMesh(geometry,material) })")(geometry, material)
}

fun newMeshFaceMaterial(materials : dynamic) : dynamic {
    return js("(function (materials) { return new THREE.MeshFaceMaterial(materials) })")(materials)
}

class Scene(o : dynamic) {
    val o = o
    fun add(m : Mesh) { o.add(m.o) }
    fun add(l : Light) { o.add(l.o) }
    fun add(g : Group) { o.add(g.o) }
    fun remove(m : Mesh) { o.remove(m.o) }
    fun remove(l : Light) { o.remove(l.o) }
    fun remove(g : Group) { o.remove(g.o) }
}

fun newScene() : Scene {
    return Scene(js("new THREE.Scene()"))
}

class Light(o : dynamic) {
    val o = o
}

fun newLight(color : Int) : Light {
    return Light(js("(function(c) { return new THREE.PointLight( c ) })")(color))
}

fun getCurTime() : Double {
    return js("(new Date().getTime()) / 1000.0")
}

fun rand() : Double {
    return js("Math.random()")
}

class Group(o : dynamic) {
    val o = o
    fun add(m : Mesh) { o.add(m.o) }
    fun add(m : Group) { o.add(m.o) }
    fun remove(m : Mesh) { o.remove(m.o) }
    fun remove(m : Group) { o.remove(m.o) }
}

fun newGroup() : Group {
    return Group(js("new THREE.Group()"))
}

