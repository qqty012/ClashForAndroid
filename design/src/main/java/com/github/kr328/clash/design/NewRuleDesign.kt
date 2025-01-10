package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.core.model.Rule
import com.github.kr328.clash.design.databinding.DesignNewRuleBinding
import com.github.kr328.clash.design.dialog.requestModelSpinner
import com.github.kr328.clash.design.dialog.requestModelTextInput
import com.github.kr328.clash.design.util.*
import com.github.kr328.clash.service.remote.IRuleManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class NewRuleDesign(context: Context) : Design<NewRuleDesign.Request>(context) {
    sealed class Request {
        object Commit : Request()
    }

    private val binding = DesignNewRuleBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    var rule: Rule
        get() = binding.rule!!
        set(value) {
            binding.rule = value
        }

    private var _proxies: List<IRuleManager.ProxyGroup>? = null

    var proxies: List<IRuleManager.ProxyGroup>
        get() = _proxies ?: emptyList()
        set(value) {
            _proxies = value
        }


    suspend fun requestExitWithoutSaving(): Boolean {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { ctx ->
                val dialog = MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.exit_without_save)
                    .setMessage(R.string.exit_without_save_warning)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok) { _, _ -> ctx.resume(true) }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .setOnDismissListener { if (!ctx.isCompleted) ctx.resume(false) }
                    .show()

                ctx.invokeOnCancellation { dialog.dismiss() }
            }
        }
    }

    init {
        binding.self = this
        binding.ipcidr = Rule.Type.IPCIDR.value

        binding.activityBarLayout.applyFrom(context)

        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)
    }

    fun selectType() {
        launch {
            val type = context.requestModelSpinner(
                initial = rule.type,
                title = "Type",
                dropDownList = Rule.Type.values().map { it.value },
                reset = null
            )
            if (type != rule.type) {
                rule = rule.copy(type = type!!)
            }
        }
    }

    fun inputPayload() {
        launch {
            val payload = context.requestModelTextInput(
                initial = rule.payload,
                title = "Payload",
            )

            if (payload != rule.payload) {
                rule = rule.copy(payload = payload)
            }
        }
    }

    fun selectProxy() {
        launch {
            val dropList = ArrayList<String>()
            dropList.add("DIRECT")
            dropList.add("REJECT")
            dropList.addAll(proxies.map { it.name })
            val proxy = context.requestModelSpinner(
                initial = rule.proxy,
                title = "Proxy",
                dropDownList = dropList,
                reset = null
            )
            if (proxy != rule.proxy) {
                rule = rule.copy(proxy = proxy!!)
            }
        }
    }

    fun selectResolve() {
        launch {
            val dropList = ArrayList<String>()
            dropList.add("resolve")
            dropList.add("no-resolve")
            val proxy = context.requestModelSpinner(
                initial = rule.proxy,
                title = "Proxy",
                dropDownList = dropList,
                reset = null
            )
            rule = rule.copy(resolve = proxy == "no-resolve")
        }
    }


    fun requestCommit() {
        requests.trySend(Request.Commit)
    }
}