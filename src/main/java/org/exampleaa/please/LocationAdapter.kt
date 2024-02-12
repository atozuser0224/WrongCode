package org.exampleaa.please

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.Vector

class LocationAdapter(val vector: Vector,val world: World) {
    fun toLocation()= Location(world,vector.x,vector.y,vector.z)
}
fun Location.toAdapter()= LocationAdapter(this.toVector(),this.world)
