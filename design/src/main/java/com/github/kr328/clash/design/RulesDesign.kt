package com.github.kr328.clash.design

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.github.kr328.clash.core.model.Rule
import com.github.kr328.clash.design.adapter.RulesAdapter
import com.github.kr328.clash.design.databinding.DesignRulesBinding
import com.github.kr328.clash.design.databinding.DialogRuleMenuBinding
import com.github.kr328.clash.design.dialog.AppBottomSheetDialog
import com.github.kr328.clash.design.dialog.requestModelTextInput
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.applyLinearAdapter
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.patchDataSet
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.launch

class RulesDesign(context: Context) : Design<RulesDesign.Request>(context) {
    sealed class Request {
        object Create : Request()
        data class Edit(val rule: Rule) : Request()
        data class Delete(val rule: Rule) : Request()
        data class Search(val keywords: String) : Request()
    }

    private val binding = DesignRulesBinding
        .inflate(context.layoutInflater, context.root, false)
    private val adapter = RulesAdapter(context, this::showMenu)

    private var rules = emptyList<Rule>()

    private var keyword = ""

    override val root: View
        get() = binding.root

    suspend fun patchRules(rules: List<Rule>, isRep: Boolean = true) {
        if (isRep) this.rules = rules
        adapter.apply {
            patchDataSet(this::rules, rules)
        }
    }

    suspend fun filterPayload(keyword: String) {
        val newRules = if (keyword.isEmpty()) rules else rules.filter { it.payload.contains(keyword) }
        patchRules(newRules, false)
    }

    fun updateElapsed() {
        adapter.updateElapsed()
    }

    init {
        binding.self = this

        binding.activityBarLayout.applyFrom(context)

        binding.mainList.recyclerList.also {
            it.bindAppBarElevation(binding.activityBarLayout)
            it.applyLinearAdapter(context, adapter)
        }
    }

    private fun showMenu(rule: Rule) {
        val dialog = AppBottomSheetDialog(context)

        val binding = DialogRuleMenuBinding
            .inflate(context.layoutInflater, dialog.window?.decorView as ViewGroup?, false)

        binding.master = this
        binding.self = dialog
        binding.rule = rule

        dialog.setContentView(binding.root)
        dialog.show()
    }

    fun requestCreate() {
        requests.trySend(Request.Create)
    }

    fun requestSearch() {
        launch {
            val search = context.requestModelTextInput(
                initial = keyword,
                title = context.getString(R.string.search)
            )
            this@RulesDesign.keyword = search
            requests.trySend(Request.Search(search))
        }
    }

    fun requestEdit(dialog: Dialog, rule: Rule) {
        requests.trySend(Request.Edit(rule))

        dialog.dismiss()
    }

    fun requestDelete(dialog: Dialog, rule: Rule) {
        requests.trySend(Request.Delete(rule))

        dialog.dismiss()
    }
}