package com.github.kr328.clash.service

import android.content.Context
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.Rule
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.remote.IRuleManager
import com.github.kr328.clash.service.util.importedDir
import com.github.kr328.clash.service.util.pendingDir
import com.github.kr328.clash.service.util.processingDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.yaml.snakeyaml.Yaml
import java.io.File

class RuleManager(private val context: Context, private val profileManager: ProfileManager) :
    IRuleManager, CoroutineScope by CoroutineScope(Dispatchers.IO) {

    val yaml = Yaml()
    private val rules = ArrayList<Rule>()
    private var proxies = ArrayList<IRuleManager.ProxyGroup>()
    private var source: MutableMap<String, Any>? = null

    override suspend fun create(rule: Rule) {
        val newRules = ArrayList<Rule>()
        newRules.add(rule)
        newRules.addAll(rules)

        rules.clear()
        rules.addAll(newRules)
    }

    override suspend fun delete(rule: Rule) {
        rules.remove(rule)
        apply()
    }

    override suspend fun update(old: Rule, new: Rule) {
        val index = rules.indexOf(old)
        rules[index] = new
    }

    override suspend fun getRules(): List<Rule> {
        return this.rules
    }

    override suspend fun getProxyGroup(): List<IRuleManager.ProxyGroup> {
        if (source == null)
            parse()
        return proxies
    }

    override suspend fun queryAll(): List<Rule> {
        return parse()
    }

    private suspend fun getActiveProfile(): Profile? {
        return profileManager.queryAll().find { it.active }
    }

    private fun getConfigFile(profile: Profile): File {
        return if (profile.imported) context.importedDir
        else if (profile.pending) context.pendingDir
        else context.processingDir
    }

    override suspend fun apply() {
        val data = HashMap<String, Any>()
        this.source?.let {
            it["rules"] = rules.map { "${it.type}, ${it.payload}, ${it.proxy}${if(it.resolve) ", no-resolve" else ""}" }

            for (key in it.keys)
                data[key] = it[key] as Any
        }

        getActiveProfile()?.apply {
            val writer = getConfigFile(this)
                .resolve(this.uuid.toString())
                .resolve(YAML_FILE_NAME)
                .writer(Charsets.UTF_8)
            yaml.dump(data, writer)
        }
        Clash.reset()
    }

    private suspend fun parse(): List<Rule> {
        return getActiveProfile()?.let { prf ->
            val reader = getConfigFile(prf).resolve(prf.uuid.toString()).resolve(YAML_FILE_NAME).reader(Charsets.UTF_8)
            source = yaml.load<Map<String, Any>>(reader).toMutableMap()

            val rules = source!!["rules"] as List<*>
            val names = source!!["proxy-groups"] as List<*>

            proxies.clear()
            this.rules.clear()
            names.forEach { et ->
                val p = et as LinkedHashMap<*,*>
                val pro = (p["proxies"] as List<*>).map { it.toString() }
                proxies.add(IRuleManager.ProxyGroup(p["name"] as String, p["type"] as String, pro))
            }

            val newRules = ArrayList<Rule>()

            for (rule in rules) {
                val ruleSplit = rule.toString().split(",")
                var b = false
                when(ruleSplit.size) {
                    1 -> continue
                    2 -> {
                        newRules.add(Rule(ruleSplit[0].trim(), "", ruleSplit[1].trim()))
                        continue
                    }
                    4 -> {
                        b = ruleSplit[3].trim() == "no-resolve"
                    }
                }

                newRules.add(Rule(ruleSplit[0].trim(), ruleSplit[1].trim(), ruleSplit[2].trim(), b))
            }
            this.rules.addAll(newRules)

            return newRules
        } ?: return emptyList()
    }

    companion object {
        const val YAML_FILE_NAME = "config.yaml"
    }
}