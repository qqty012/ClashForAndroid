package com.github.kr328.clash.design.adapter

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.databinding.AdapterLogMessageBinding
import com.github.kr328.clash.design.util.layoutInflater

class LogMessageAdapter(
    private val context: Context,
    private val copy: (LogMessage) -> Unit,
) :
    RecyclerView.Adapter<LogMessageAdapter.Holder>() {
    class Holder(val binding: AdapterLogMessageBinding) : RecyclerView.ViewHolder(binding.root)

    var messages: List<LogMessage> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            AdapterLogMessageBinding
                .inflate(context.layoutInflater, parent, false)
        )
    }

    @Suppress("Deprecation")
    private fun getColor(resId: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.resources.getColor(resId, null)
        } else {
            context.resources.getColor(resId)
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val current = messages[position]

        holder.binding.message = current

        val msg = current.message

        val color: Int = when (current.level) {
            LogMessage.Level.Info -> {
                if (msg.startsWith("[TCP]") || msg.startsWith("[UDP]")) {
                    getColor(R.color.color_log_success)
                } else {
                    getColor(R.color.color_log_info)
                }
            }
            LogMessage.Level.Warning -> {
                getColor(R.color.color_log_error)
            }
            else -> getColor(R.color.color_log_info)
        }

        holder.binding.logMessage.setTextColor(color)

        holder.binding.root.setOnLongClickListener {
            copy(current)

            true
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }
}