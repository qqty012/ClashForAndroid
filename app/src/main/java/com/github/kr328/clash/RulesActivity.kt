package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.design.RulesDesign
import com.github.kr328.clash.util.withRule
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import java.util.concurrent.TimeUnit

class RulesActivity : BaseActivity<RulesDesign>() {
    override suspend fun main() {
        val design = RulesDesign(this)

        setContentDesign(design)

        val ticker = ticker(TimeUnit.MINUTES.toMillis(1))

        while (isActive) {
            select {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart, Event.ProfileChanged -> {
                            design.fetch()
                        }
                        else -> Unit
                    }
                }
                design.requests.onReceive {

                    when (it) {
                        RulesDesign.Request.Create ->{
                            val newRule = NewRuleActivity::class.intent
                            newRule.putExtra("rule_is_new", true)
                            startActivity(newRule)
                        }
                        is RulesDesign.Request.Delete ->
                            withRule {
                                delete(it.rule)
                                design.patchRules(getRules())
                            }
                        is RulesDesign.Request.Edit -> {
                            val updateRule = NewRuleActivity::class.intent
                            updateRule.putExtra("rule_is_new", false)
                            updateRule.putExtra("rule_item", it.rule)
                            startActivity(updateRule)
                        }
                        is RulesDesign.Request.Search -> {
                            design.filterPayload(it.keywords)
                        }
                    }
                }
                if (activityStarted) {
                    ticker.onReceive {
                        design.updateElapsed()
                    }
                }
            }
        }
    }

    private suspend fun RulesDesign.fetch() {
        withRule {
            patchRules(queryAll())
        }
    }
}