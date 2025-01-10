package com.github.kr328.clash

import androidx.activity.OnBackPressedCallback
import com.github.kr328.clash.core.model.Rule
import com.github.kr328.clash.design.NewRuleDesign
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.showExceptionToast
import com.github.kr328.clash.util.withRule
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class NewRuleActivity : BaseActivity<NewRuleDesign>() {
    private var isNew: Boolean = false

    private lateinit var oldRule: Rule

    override suspend fun main() {
        setResult(RESULT_CANCELED)

        val original = intent.getParcelableExtra("rule_item") ?:
        Rule(Rule.Type.Domain.value.uppercase(), "", "")

        isNew = intent.getBooleanExtra("rule_is_new", false)

        val design = NewRuleDesign(this)

        design.rule = original
        oldRule = original
        withRule {
            design.proxies = getProxyGroup()
        }

        setContentDesign(design)


        while (isActive) {
            select {
                events.onReceive {
                    when (it) {
                        Event.ServiceRecreated -> {
                            finish()
                        }
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    if (it == NewRuleDesign.Request.Commit) {
                        design.verifyAndCommit()
                    }
                }
            }
        }
        onBackPressedListener()
    }

    private fun onBackPressedListener() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                design?.apply {
                    launch {
                        if (requestExitWithoutSaving())
                            finish()
                    }
                }
            }
        })
    }

    private suspend fun NewRuleDesign.verifyAndCommit() {
        when {
            rule.type.isBlank() -> {
                showToast(R.string.empty_type, ToastDuration.Long)
            }
            rule.payload.isBlank() -> {
                showToast("Empty payload", ToastDuration.Long)
            }
            rule.proxy.isBlank() -> {
                showToast("Empty proxy", ToastDuration.Long)
            }
            else -> {
                try {
                    withRule {
                        if (isNew) {
                            create(rule)
                        } else {
                            update(oldRule, rule)
                        }
                        coroutineScope {
                            apply()
                        }
                    }
                    setResult(RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    showExceptionToast(e)
                }
            }
        }
    }
}