package io.github.starwishsama.cleardrop.module

import cn.nukkit.Player
import cn.nukkit.event.player.PlayerChatEvent
import io.github.starwishsama.cleardrop.Config
import io.github.starwishsama.cleardrop.WClearDropPlugin
import io.github.starwishsama.cleardrop.utils.runCleanTask
import top.wetabq.easyapi.api.defaults.AsyncListenerAPI
import top.wetabq.easyapi.api.defaults.CommandAPI
import top.wetabq.easyapi.api.defaults.ConfigAPI
import top.wetabq.easyapi.api.defaults.SimplePluginTaskAPI
import top.wetabq.easyapi.config.encoder.advance.SimpleCodecEasyConfig
import top.wetabq.easyapi.listener.AsyncListener
import top.wetabq.easyapi.module.ModuleInfo
import top.wetabq.easyapi.module.ModuleVersion
import top.wetabq.easyapi.module.SimpleEasyAPIModule

object WClearDropModule : SimpleEasyAPIModule() {

    private const val MODULE_NAME = "ClearDropModule"
    private const val AUTHOR = "StarWishsama"

    private const val SIMPLE_CONFIG = "clearDropSimpleConfig"
    private var nextRequestTime = mutableMapOf<Player, Long>()

    lateinit var simpleConfig: SimpleCodecEasyConfig<Config>

    override fun getModuleInfo(): ModuleInfo = ModuleInfo(
            WClearDropPlugin.instance,
            MODULE_NAME,
            AUTHOR,
            ModuleVersion(1, 0, 0)
    )

    override fun moduleRegister() {
        // Setup config

        simpleConfig = object : SimpleCodecEasyConfig<Config>("clearDrop", WClearDropPlugin.instance, Config::class.java, Config()) {}

        simpleConfig.init()

        if (!simpleConfig.simpleConfig.containsKey("clearDrop")) {
            simpleConfig.simpleConfig["clearDrop"] = simpleConfig.getDefaultValue()
            simpleConfig.save()
        }

        AsyncListenerAPI.add(object : AsyncListener {
            override fun onPlayerChatEvent(event: PlayerChatEvent) {
                event.player.sendMessage(isCoolDown(event.player).toString())
                if (event.message.contains(simpleConfig.safeGetData("clearDrop").requestMessage) && !isCoolDown(event.player)) {
                    runCleanTask(WClearDropPlugin.instance.server)
                }
            }
        })


        SimplePluginTaskAPI.delayRepeating(20 * simpleConfig.safeGetData("clearDrop").clearDropCD, 20 * simpleConfig.safeGetData("clearDrop").clearDropCD) { _, _ ->
            runCleanTask(WClearDropPlugin.instance.server)
        }

        this.registerAPI(SIMPLE_CONFIG, ConfigAPI())
                .add(simpleConfig)

        this.registerAPI("clearDropCommand", CommandAPI())
                .add(WClearDropCommand)
    }

    override fun moduleDisable() {
    }

    fun isCoolDown(player: Player): Boolean {
        if (nextRequestTime.containsKey(player)) {
            nextRequestTime[player]?.let { time ->
                return System.currentTimeMillis() - time > simpleConfig.safeGetData("clearDrop").clearDropCD
            }
        } else {
            nextRequestTime[player] = System.currentTimeMillis()
            return false
        }
        return false
    }
}