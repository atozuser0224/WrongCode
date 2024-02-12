package org.exampleaa.please


import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.monun.invfx.InvFX.frame
import io.github.monun.invfx.openFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.configuration.Configuration
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.*
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.metadata.MetadataValue
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.exampleaa.please.Please.Companion.plugin
import org.exampleaa.please.event.event
import org.exampleaa.please.event.events
import org.gang.debugcat.item.item
import xyz.icetang.lib.kommand.getValue
import xyz.icetang.lib.kommand.kommand
import java.io.File
import java.util.*


@Suppress("UnstableApiUsage")
class Please : JavaPlugin() {
    val entityTypes = listOf(
        EntityType.ZOMBIE,
        EntityType.SPIDER,
        EntityType.CAVE_SPIDER,
        EntityType.WITHER,
        EntityType.WITHER_SKELETON,
        EntityType.HOGLIN,
        EntityType.BLAZE,
        EntityType.DROWNED
    )
    var hottime = 0
    var multiplier  = 0.0
    companion object{
        lateinit var pleaseprotocolManager: ProtocolManager
        lateinit var plugin: Plugin
    }
    override fun onLoad() {
        pleaseprotocolManager = ProtocolLibrary.getProtocolManager()
        plugin = this
    }
    override fun onEnable() {
        val folder = File(dataFolder,"config.yml")
        saveDefaultConfig();
        val bans = config.getConfigurationSection("bans")?:config.createSection("bans")
        val drops = config.getConfigurationSection("drops")?:config.createSection("bans")
        HeartbeatScope().launch {
            delay(20)
            if (hottime == 1){
                Bukkit.getServer().broadcastMessage("&c핫타임&f이 종료되었습니다.")
            }
            saveConfig()
        }
        HeartbeatScope().launch {
            delay(60*20)
            Bukkit.getOnlinePlayers().forEach {
                val folder = File(dataFolder,"Players")
                if (!folder.exists()) {
                    folder.mkdirs()
                }
                val file = File(folder, "${it.name}.yml")
                val playerConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(file)
                playerConfig.set("MobCount",0)
                playerConfig.save(file)
            }
        }
        this.kommand {
            register("money"){
                requires { isOp }
                then("check"){
                    executes {
                        val attackerName = sender.name
                        val folder = File(dataFolder,"Players")
                        if (!folder.exists()) {
                            folder.mkdirs()
                        }
                        val file = File(folder, "${attackerName}.yml")
                        val playerConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(file)
                        sender.sendMessage("[서버] ${playerConfig.getInt("money")}의 잔액이 남았습니다.")
                        playerConfig.save(file)

                    }
                }
                then("set"){
                    then("money" to int()){
                        executes {
                            val money : Int by it
                            val attackerName = sender.name
                            val folder = File(dataFolder,"Players")
                            if (!folder.exists()) {
                                folder.mkdirs()
                            }
                            val file = File(folder, "${attackerName}.yml")
                            val playerConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(file)
                            playerConfig.set("money",money)
                            sender.sendMessage("[서버] ${playerConfig.getInt("money")}의 잔액이 남았습니다.")
                            playerConfig.save(file)
                        }
                    }
                }
                then("add"){
                    then("money" to int()){
                        executes {
                            val money : Int by it
                            val attackerName = sender.name
                            val folder = File(dataFolder,"Players")
                            if (!folder.exists()) {
                                folder.mkdirs()
                            }
                            val file = File(folder, "${attackerName}.yml")
                            val playerConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(file)
                            playerConfig.set("money",playerConfig.getInt("money")+money)
                            sender.sendMessage("[서버] ${playerConfig.getInt("money")}의 잔액이 남았습니다.")
                            playerConfig.save(file)

                        }
                    }
                }
            }
            register("dropitem"){
                requires {isPlayer && isOp}
                executes {
                    val p = sender as Player
                    if (p.inventory.itemInMainHand == null){
                        p.sendMessage("[서버] 아이템을 손에 쥔채로 명령어를 실행하십시오..")
                    }else{
                        drops.set(p.inventory.itemInMainHand.i18NDisplayName?:"",p.inventory.itemInMainHand)
                        p.sendMessage("[서버] ${p.inventory.itemInMainHand.i18NDisplayName}를 추가했어요.")
                    }
                    config.set("drops",drops)
                    saveConfig()
                }
                then("목록"){
                    executes {
                        drops.toItemstacks().forEach {
                            sender.sendMessage("[목록] : ${it.i18NDisplayName}")
                        }
                    }
                }
            }
            register("banitem"){
                requires {isPlayer && isOp}
                executes {
                    val p = sender as Player
                    if (p.inventory.itemInMainHand == null){
                        p.sendMessage("[서버] 아이템을 손에 쥔채로 명령어를 실행하십시오..")
                    }else{
                        if (bans.toItemstacks().contains(p.inventory.itemInMainHand)){
                            bans.set(bans.ItemStackKey(p.inventory.itemInMainHand)?:"",null)
                            p.sendMessage("[서버] ${p.inventory.itemInMainHand.i18NDisplayName}를 삭제했어요.")
                        }else{
                            bans.set(p.inventory.itemInMainHand.i18NDisplayName?:"",p.inventory.itemInMainHand)
                            p.sendMessage("[서버] ${p.inventory.itemInMainHand.i18NDisplayName}를 추가했어요.")
                        }
                        config.set("bans",bans)
                        saveConfig()
                    }
                }
                then("목록"){
                    executes {
                        bans.toItemstacks().forEach {
                            sender.sendMessage("[목록] : ${it.i18NDisplayName}")
                        }
                    }
                }
            }
            register("mob"){
                then("center"){
                    executes {
                        config.set("center",(sender as Player).location)
                        sender.sendMessage("위치를 지정했습니다. ${(sender as Player).location}")
                        saveConfig()
                    }
                }
                then("tp"){
                    executes {
                        (sender as Player).teleport(config.getLocation("center")?:location)
                    }
                }
            }
            register("hottime"){
                then("multiplier" to int()){
                    then("duration" to int()){
                        executes {
                            val multiplier : Int by it
                            val duration : Int by it
                            hottime = duration * 60
                            this@Please.multiplier = multiplier.toDouble()
                            player.server.broadcastMessage("${ChatColor.RED}핫타임${ChatColor.WHITE}이 시작되었습니다!. 드랍률 ${multiplier * 100}% [$hottime 초]")
                        }
                    }
                }

            }
        }
        this.events {
            event<EntityDeathEvent>(){
                val victim : Entity = entity
                if (victim.type in arrayOf(EntityType.ZOMBIE, EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.WITHER,
                        EntityType.WITHER_SKELETON, EntityType.HOGLIN, EntityType.BLAZE, EntityType.DROWNED, EntityType.ENDERMAN,
                        EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN, EntityType.SKELETON)) {
                    if (victim !is Player) {
                        val attacker = getEntity().killer

                        if (attacker?.type == EntityType.PLAYER) {
                            val attackerName = attacker.name
                            val folder = File(dataFolder,"Players")
                            if (!folder.exists()) {
                                folder.mkdirs()
                            }
                            val file = File(folder, "${attackerName}.yml")
                            val playerConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(file)
                            val mobCount = playerConfig.getInt("MobCount")+1
                            playerConfig.set("MobCount",mobCount)
                            playerConfig.save(file)
                            if (mobCount > 50) {
                                attacker.kickPlayer("§c몹 카운트가 초과되어 자동으로 밴 처리되었습니다.")
                                attacker.banPlayer("§c관리자에게 문의하세요.", "")
                                server.broadcastMessage("§c${attackerName}의 몹 카운트가 초과되어 자동으로 밴 처리되었습니다.")
                            }

                            val hotTimeAdd = multiplier ?: 1.0
                            val h = if (hotTimeAdd >= 1.0) hotTimeAdd else 1.0

                            val drop = drops.toItemstacks().random()

                            val worldName = getEntity().world.name

                            val atper = (0.5 * h) + ((victim as LivingEntity) .maxHealth * 0.09 * h)

                            val location = getEntity().location

                            when (worldName) {
                                "world", "world_nether", "world_the_end" -> {
                                    if (Random().nextDouble(0.0, 100.0) < atper) {
                                        location.world.dropItemNaturally(location, drop)
                                    }
                                }
                            }
                        } else {
                            getDrops().clear()
                        }
                    }
                }
                HeartbeatScope().launch {
                    if (!((victim as LivingEntity).isGlowing)){
                        if (entityTypes.contains(victim.type)){
                            if (Random().nextInt(1000)<= 15){
                                spawnEntity(victim, config)
                            }
                        }else if (victim::class.java is Skeleton){
                            if (Random().nextInt(1000)<= 11){
                                spawnEntity(victim, config)
                            }
                        }
                    }else{
                        if (entityTypes.contains(victim.type)){
                            if (Random().nextInt(1000)<= 900){
                                dropRandomItem(victim.location)
                            }
                        }else if (victim::class.java is Skeleton){
                            if (Random().nextInt(1000)<= 900){
                                dropRandomItem(victim.location)
                            }
                        }
                    }
                }
            }
            event<PlayerDeathEvent> {
                val victim = entity
                keepLevel = true // 레벨 유지
                val xp = victim.level
                val chance = Random().nextInt(1, 101)
                when {
                    chance in 0..12 -> {
                        victim.level = 0
                        victim.sendMessage("${ChatColor.RED}${ChatColor.BOLD}모든 경험치를 잃었습니다...")
                    }
                    chance in 12..29 -> {
                        // 17%의 확률로 20%의 경험치 잃음
                        val lostXp = (xp * 0.2).toInt()
                        victim.level -= lostXp
                        victim.sendMessage("${ChatColor.RED}${ChatColor.BOLD}많은 경험치를 잃었습니다...")
                    }
                    chance in 30..50 -> {
                        // 20%의 확률로 50%의 경험치 잃음
                        val lostXp = (xp * 0.5).toInt()
                        victim.level -= lostXp
                        victim.sendMessage("${ChatColor.RED}${ChatColor.BOLD}꽤 많은 경험치를 잃었습니다...")
                    }
                    else -> {
                        // 나머지 경우 랜덤한 양의 경험치 잃음
                        val lostXp = (xp * Random().nextDouble(0.01, 0.5)).toInt()
                        victim.level -= lostXp
                        victim.sendMessage("${ChatColor.RED}${ChatColor.BOLD}행운이네요. 많은 경험치를 지켰습니다!")
                    }
                }
            }
            event<PlayerInteractEntityEvent> {
                if (this.rightClicked.name == "은행원" || player.isOp){
                    isCancelled = true
                    var n =0
                    val folder = File(dataFolder,"Players")
                    if (!folder.exists()) {
                        folder.mkdirs()
                    }
                    val file = File(folder, "${player.name}.yml")
                    val playerConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(file)
                    var playerMoney = playerConfig.getInt("money")
                    var money = StringBuilder("")
                    val inv = frame(4, Component.text("${ChatColor.GRAY}${ChatColor.BOLD}수표발행")){
                        HeartbeatScope().launch {
                            while (true){
                                item(5,3,item(Material.LIME_STAINED_GLASS_PANE){
                                    setDisplayName("&a&l${if(money.toString()!="")money else "0"}원 발행하기".translateColor())
                                    lore = listOf("&7클릭 시 금액을 수표로 발행합니다. 5%(0원)의 수수료가 발생합니다.".translateColor(),
                                        "&7현재 잔액 ${playerMoney}".translateColor())
                                    ItemFlag.values().forEach {
                                        addItemFlags(it)
                                    }})
                                delay(5)
                            }
                        }
                        repeat(4){y->
                            repeat(9){x->
                                if (x in 3..5 && y in 0..2){
                                    slot(x,y){
                                        val addn = ++n
                                        item = makeItem(n)
                                        onClick {
                                            money.append(addn.toString())
                                            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)

                                        }
                                    }
                                }else if (x==3 && y==3){
                                    slot(x,y){
                                        item = item(Material.BLACK_STAINED_GLASS_PANE){
                                            setDisplayName("&c&l0".translateColor())
                                            lore = listOf("&7클릭 시 금액에 추가됩니다.".translateColor())
                                            ItemFlag.values().forEach {
                                                addItemFlags(it)
                                            }
                                        }
                                        onClick {
                                            money.append("0")
                                            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)

                                        }
                                    }
                                }else if (x==4 && y==3){
                                    slot(x,y){
                                        item = item(Material.RED_STAINED_GLASS_PANE){
                                            setDisplayName("&c&l지우기".translateColor())
                                            lore = listOf("&7클릭 시 금액을 지웁니다.".translateColor())
                                            ItemFlag.values().forEach {
                                                addItemFlags(it)
                                            }
                                            onClick {
                                                money.deleteAt(money.length-1)
                                            }
                                        }
                                    }
                                }else if (x==5 && y==3){
                                    slot(x,y){
                                        onClick {
                                            val a = if  (money.toString() == "") "0" else money.toString()
                                            if (a.toInt() >= 1000) {
                                                val v = a.toInt() + (a.toInt() * 0.05).toInt()
                                                if (playerMoney >= v) {
                                                    playerConfig.set("money",playerMoney - v)
                                                    playerConfig.save(file)
                                                    player.closeInventory()
                                                    val paper = item(Material.PAPER){
                                                        setDisplayName("&e&l수표 &a&l$money}원".translateColor())
                                                        lore = listOf("&f".translateColor(), "&7클릭 시 금액만큼 돈이 지급됩니다.".translateColor(), "&f".translateColor())
                                                        persistentDataContainer.set(NamespacedKey.fromString("money")!!,
                                                            PersistentDataType.INTEGER,money.toString().toInt())
                                                    }
                                                    player.inventory.addItem(paper)
                                                    val _ic = (money.toString().toInt() * 0.05).toInt()
                                                    player.sendMessage("&e&l수표를 발행하였습니다. 금액 &f&l$money | 수수료 &f&l$_ic".translateColor())
                                                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1.0f, 1.0f)
                                                } else {
                                                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1.0f, 1.0f)
                                                    player.sendMessage("&c&l금액이 부족합니다.".translateColor())
                                                }
                                            } else {
                                                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1.0f, 1.0f)
                                                player.sendMessage("&c&l1000원부터 수표를 발행할 수 있습니다.".translateColor())
                                            }
                                        }
                                    }
                                }else{
                                    slot(x,y){
                                        item = item(Material.LIGHT_GRAY_STAINED_GLASS_PANE){
                                            setDisplayName("&c&l빈 슬롯".translateColor())
                                            lore = listOf("&7해당 슬롯은 장식용입니다.".translateColor())
                                            ItemFlag.values().forEach {
                                                addItemFlags(it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    player.openFrame(inv)
                }
            }
            event<BlockBreakEvent> {
                val block: Block = block
                HeartbeatScope().launch {
                    val h = multiplier
                    when (block.type) {
                        Material.DEEPSLATE -> {
                            val chance = 0.02 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.STONE -> {
                            val chance = 0.011 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.DIAMOND_ORE -> {
                            val chance = 2.1 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.DEEPSLATE_DIAMOND_ORE -> {
                            val chance = 3.1 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.ANCIENT_DEBRIS -> {
                            val chance = 10.0 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.IRON_ORE -> {
                            val chance = 0.62 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.DEEPSLATE_IRON_ORE -> {
                            val chance = 0.62 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.REDSTONE_ORE -> {
                            val chance = 0.65 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.DEEPSLATE_REDSTONE_ORE -> {
                            val chance = 0.65 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.GOLD_ORE -> {
                            val chance = 0.75 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.DEEPSLATE_GOLD_ORE -> {
                            val chance = 0.75 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.COPPER_ORE -> {
                            val chance = 0.33 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.DEEPSLATE_COPPER_ORE -> {
                            val chance = 0.33 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.COAL_ORE -> {
                            val chance = 0.28 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.DEEPSLATE_COAL_ORE -> {
                            val chance = 0.28 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.LAPIS_ORE -> {
                            val chance = 0.65 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.DEEPSLATE_LAPIS_ORE -> {
                            val chance = 0.65 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.EMERALD_ORE -> {
                            val chance = 2.5 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.PUMPKIN -> {
                            val chance = 0.35 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.MELON -> {
                            val chance = 0.35 * h
                            if (Random().nextDouble(0.0, 100.0) < chance) {
                                dropRandomItem(block.location)
                            }
                        }
                        Material.WHEAT -> checkCropAndDropItem(block, 7, h)
                        Material.BEETROOTS -> checkCropAndDropItem(block, 3, h)
                        Material.POTATOES -> checkCropAndDropItem(block, 7, h)
                        Material.CARROTS -> checkCropAndDropItem(block, 7, h)
                        else -> {
                            // Handle other block types if needed
                        }
                    }
                }

            }
            event<BlockPlaceEvent> {
                val player = player
                val placedBlock = blockPlaced
                if (isRestrictedBlock(placedBlock.type) && !player.isOp) {
                    isCancelled = true
                }
            }
            event<EntityDamageByEntityEvent> {
                if (damager.type == EntityType.PLAYER && !damager.isOp && bans.toItemstacks().contains((damager as Player).inventory.itemInMainHand)){
                    isCancelled = true
                }
                if (damager.type != EntityType.PLAYER){
                    if (damager.isGlowing){
                        damage = config.getDouble("damage")
                    }else{
                        val maxHealth = (damager as LivingEntity).getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value!!
                        var damage = 0.0

                        when (damager) {
                            is Skeleton -> {
                                damage = maxHealth * 0.037
                                if (damage > 16) damage = 16.0
                            }
                            is Zombie -> {
                                damage = maxHealth * 0.09
                                if (damage > 24) damage = 24.0
                            }
                            is Enderman -> {
                                damage = maxHealth * 0.04
                                if (damage > 24) damage = 24.0
                            }
                            else -> {
                                damage = maxHealth * 0.08
                                if (damage > 24) damage = 24.0
                            }
                        }

                        setDamage(damage)
                    }
                }
            }
            event<PlayerInteractEvent> {
                val item = player.inventory.itemInMainHand
                if (!player.isOp && bans.toItemstacks().contains(item)){
                    isCancelled = true
                }
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    val player = player
                    val item = player.inventory.itemInMainHand

                    if (item.type == Material.PAPER && item.hasItemMeta() && item.itemMeta.persistentDataContainer.get(
                            NamespacedKey.fromString("money")!!,
                            PersistentDataType.INTEGER) != null) {
                        val money = item.itemMeta.persistentDataContainer.get(
                            NamespacedKey.fromString("money")!!,
                            PersistentDataType.INTEGER)!!
                        val folder = File(dataFolder,"Players")
                        if (!folder.exists()) {
                            folder.mkdirs()
                        }
                        val file = File(folder, "${player.name}.yml")
                        val playerConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(file)
                        player.inventory.removeItem(item)
                        playerConfig.set("money",playerConfig.getInt("money")+money)
                        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1.0f, 1.0f)
                        player.sendMessage("§e§l${money}만큼 잔액에 추가되었습니다.".translateColor())
                        playerConfig.save(file)

                    }
                }
            }
            event<PlayerItemConsumeEvent> {
                val item = player.inventory.itemInMainHand
                if (!player.isOp && bans.toItemstacks().contains(item)){
                    isCancelled = true
                }
            }
            event<PlayerSwapHandItemsEvent> {
                val item = player.inventory.itemInMainHand
                if (!player.isOp && bans.toItemstacks().contains(item)){
                    isCancelled = true
                }
            }
            event<BlockDispenseEvent> {
                if (bans.toItemstacks().contains(item)){
                    isCancelled = true
                }
            }
            event<EntitySpawnEvent> {
                when {
                    entity is Zombie -> createMob(entity, location, 125, 0.34)
                    entity is Spider -> createMob(entity, location, 115, 0.34)
                    entity is CaveSpider -> createMob(entity, location, 115, 0.34)
                    entity is WitherSkeleton -> createMob(entity, location, 115, 0.34)
                    entity is Hoglin -> createMob(entity, location, 115, 0.34)
                    entity is Blaze -> createMob(entity, location, 115, 0.34)
                    entity is Drowned -> createMob(entity, location, 115, 0.34)
                    entity is Creeper -> createMob(entity, location, 95, 0.34)
                    entity is Skeleton -> createMob(entity, location, 40, 0.25)
                }
            }

        }
    }
    override fun onDisable() {

    }
    suspend fun checkCropAndDropItem(block: Block, maturity: Int, dropChanceMultiplier: Double) {
        val state = block.state
        val age = (state.blockData as Ageable).age
        logger.info("$age $dropChanceMultiplier $maturity")
        if (age <= maturity && Random().nextDouble(0.0, 100.0) < 0.25 * dropChanceMultiplier) {
            dropRandomItem(block.location)
        }
    }
    suspend fun dropRandomItem(loc: Location) {
        delay(1)
        config.getConfigurationSection("drops")?.let {
            val drop = it.toItemstacks().random()
            loc.world.dropItem(loc,drop)
        }
    }
    private fun isRestrictedBlock(material: Material): Boolean {
        return material == Material.DIAMOND_ORE ||
                material == Material.ANCIENT_DEBRIS ||
                material == Material.PUMPKIN ||
                material == Material.MELON
    }
}
data class test(
    val name: String,
)

fun spawnEntity(victim : Entity,config : Configuration){
    val it = victim.location.world.spawnEntity(victim.location,victim.type) as LivingEntity
    it.isGlowing = true
    (it as LivingEntity).maxHealth += config.getInt("Hp")?:0
    (it as LivingEntity).health = (it as LivingEntity).maxHealth
    (it as LivingEntity).addPotionEffect(PotionEffect(
        PotionEffectType.SPEED,
        Int.MAX_VALUE,
        config.getInt("speed")?:0
    ))

}
fun createMob(entityd: Entity, loc: Location, maxHealth: Int, maxSpeed: Double){
    val entity = entityd as LivingEntity
    val mobSpawnMeta: MetadataValue? = entity.getMetadata("MobSpawn").firstOrNull()
    HeartbeatScope().launch {
        if (mobSpawnMeta == null || !mobSpawnMeta.asBoolean()) {
            entity.setMetadata("MobSpawn", FixedMetadataValue(plugin, true))

            // 중앙 위치로 이동
            loc.y = loc.world.getSpawnLocation().y
            val distance = loc.distance(loc.world.spawnLocation)

            if (distance >= 1) {
                // 최대 체력 설정
                val newMaxHealth =
                    (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: ((20.0 - 10) + distance)) * 0.0014
                val finalMaxHealth = if (newMaxHealth > maxHealth) maxHealth.toDouble() else newMaxHealth
                entity.maxHealth = finalMaxHealth
                entity.health = finalMaxHealth

                // 이동 속도 설정
                val newSpeed = (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.value ?: 0.0) + finalMaxHealth * 0.0008
                val finalSpeed = if (newSpeed > maxSpeed) maxSpeed else newSpeed
                entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = finalSpeed
            }
            delay(20)
            if ((entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.value ?: 0.0) > maxSpeed) {
                entity.remove()
            }
        }
    }
}
val Location.serialize : HashMap<String,String> get() {
    val map = hashMapOf<String,String>()
    map["x"] = this.x.toString()
    map["y"] = this.y.toString()
    map["z"] = this.z.toString()
    map["world"] = this.world.name
    return map
}
val HashMap<String,String>.toLocation : Location get() {
    return Location(
        Bukkit.getWorld(this["world"]?:""),
        this["x"]!!.toDouble(),
        this["y"]!!.toDouble(),
        this["z"]!!.toDouble()
    )
}


fun ConfigurationSection.toItemstacks(): MutableList<ItemStack> {
    val keys = this.getKeys(false)

    return keys.map {
        this.getItemStack(it)!!
    }.toMutableList()
}
fun ConfigurationSection.ItemStackKey(itemStack: ItemStack): String? {
    val keys = this.getKeys(false)

    return keys.find {
        this.getItemStack(it) == itemStack
    }
}

fun makeItem(num : Int): ItemStack {
    return item(
        Material.BLACK_STAINED_GLASS_PANE,

    ){
        setDisplayName("&c&l${num}".translateColor())
        lore = listOf("&7클릭 시 금액에 추가됩니다.".translateColor())
        ItemFlag.values().forEach {
            addItemFlags(it)
        }
    }
}
