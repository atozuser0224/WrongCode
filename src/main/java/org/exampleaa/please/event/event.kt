package org.exampleaa.please.event

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin

public inline fun <reified T : Event> KListener<*>.event(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    noinline block: T.() -> Unit,
): Unit = event(this.plugin, priority, ignoreCancelled, block)

public fun <T : Event> KListener<*>.event(
    type: Class<T>,
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    block: T.() -> Unit,
): Unit = event(this.plugin, type, priority, ignoreCancelled, block)

public inline fun <reified T : Event> Listener.event(
    plugin: Plugin,
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    noinline block: T.() -> Unit,
) {
    event<T>(plugin, T::class.java, priority, ignoreCancelled, block)
}

public fun <T : Event> Listener.event(
    plugin: Plugin,
    type: Class<T>,
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    block: T.() -> Unit,
) {
    Bukkit.getServer().pluginManager.registerEvent(
        type,
        this,
        priority,
        { _, event ->
            if (type.isInstance(event)) {
                (event as? T)?.block()
            }
        },
        plugin,
        ignoreCancelled,
    )
}
public interface WithPlugin<T : Plugin> { public val plugin: T }
public inline fun WithPlugin<*>.events(block: KListener<*>.() -> Unit): SimpleKListener = plugin.events(block)
public inline fun Plugin.events(block: KListener<*>.() -> Unit): SimpleKListener {
    return SimpleKListener(this).apply(block)
}

public fun Listener.registerEvents(plugin: Plugin): Unit = plugin.server.pluginManager.registerEvents(this, plugin)

public fun Listener.unregisterListener(): Unit = HandlerList.unregisterAll(this)

public fun Event.callEvent(): Unit = Bukkit.getServer().pluginManager.callEvent(this)

public val PlayerMoveEvent.displaced: Boolean
    get() = this.from.x != this.to?.x || this.from.y != this.to?.y || this.from.z != this.to?.z

public interface KListener<T : Plugin> : Listener, WithPlugin<T>

public class SimpleKListener(override val plugin: Plugin) : KListener<Plugin>