package com.github.kr328.clash.design.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.core.model.Rule
import com.github.kr328.clash.design.databinding.AdapterRuleBinding
import com.github.kr328.clash.design.util.layoutInflater

class RulesAdapter(
    private val context: Context,
    private val onMenuClicked: (Rule) -> Unit,
) : RecyclerView.Adapter<RulesAdapter.Holder>() {
    class Holder(val binding: AdapterRuleBinding) : RecyclerView.ViewHolder(binding.root)

    var rules: List<Rule> = emptyList()

    fun updateElapsed() {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            AdapterRuleBinding
                .inflate(context.layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val current = rules[position]
        val binding = holder.binding

        if (current === binding.rule)
            return

        binding.rule = current
        binding.setMenu {
            onMenuClicked(current)
        }
    }

    override fun getItemCount(): Int {
        return rules.size
    }
}