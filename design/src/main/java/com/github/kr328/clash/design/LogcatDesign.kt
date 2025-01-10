package com.github.kr328.clash.design

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.core.model.Rule
import com.github.kr328.clash.design.adapter.LogMessageAdapter
import com.github.kr328.clash.design.databinding.DesignLogcatBinding
import com.github.kr328.clash.design.databinding.DialogLogcatMenuBinding
import com.github.kr328.clash.design.dialog.AppBottomSheetDialog
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogcatDesign(
    context: Context,
    private val streaming: Boolean,
) : Design<LogcatDesign.Request>(context) {
    sealed class Request {
        object Close: Request()
        object Delete: Request()
        object Export: Request()
        data class OpenNewRule(val rule: Rule): Request()
    }

    private val binding = DesignLogcatBinding
        .inflate(context.layoutInflater, context.root, false)
    private val adapter = LogMessageAdapter(context) { obj ->
        val dialog = AppBottomSheetDialog(context)

        val dialogBinding = DialogLogcatMenuBinding.inflate(context.layoutInflater, context.root, false)
        dialogBinding.master = this@LogcatDesign

        dialogBinding.logcatAdd.setOnClickListener {
            val sub = obj.message.let { msg ->
                if (!msg.startsWith("[TCP]") && !msg.startsWith("[UDP]")) {
                    return@let emptyList()
                }

                var lastIndex: Int
                if (obj.level == LogMessage.Level.Warning) {
                    val startStr = ") to"
                    lastIndex = msg.indexOf(startStr)
                    if (lastIndex != -1) {
                        lastIndex += startStr.length
                    }
                } else if (obj.level == LogMessage.Level.Info) {
                    val startStr = "-->"
                    lastIndex = msg.indexOf(startStr)
                    if (lastIndex != -1) {
                        lastIndex += startStr.length
                    }
                } else {
                    return@let emptyList()
                }
                msg.substring(lastIndex).trim().split(" ")
            }
            launch {
                if (sub.isNotEmpty()) {
                    val host = sub[0].split(":")
                    requests.trySend(Request.OpenNewRule(Rule(Rule.Type.DomainKeyword.value, host[0], "DIRECT")))
                } else {
                    showToast(R.string.invalid_content, ToastDuration.Short)
                }
                dialog.dismiss()
            }
        }

        dialogBinding.logcatDuplicate.setOnClickListener {
            launch {
                val data = ClipData.newPlainText("log_message", obj.message)

                context.getSystemService<ClipboardManager>()?.setPrimaryClip(data)
                showToast(R.string.copied, ToastDuration.Short)
                dialog.dismiss()
            }
        }
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    suspend fun patchMessages(messages: List<LogMessage>, removed: Int, appended: Int) {
        withContext(Dispatchers.Main) {
            adapter.messages = messages

            adapter.notifyItemRangeInserted(adapter.messages.size, appended)
            adapter.notifyItemRangeRemoved(0, removed)

            if (streaming && binding.recyclerList.isTop) {
                binding.recyclerList.scrollToPosition(messages.size - 1)
            }
        }
    }

    fun tryDelete() {
        requests.trySend(Request.Delete)
    }

    fun tryExport() {
        requests.trySend(Request.Export)
    }

    fun tryClose() {
        requests.trySend(Request.Close)
    }

    override val root: View
        get() = binding.root

    init {
        binding.self = this
        binding.streaming = streaming

        binding.activityBarLayout.applyFrom(context)

        binding.recyclerList.bindAppBarElevation(binding.activityBarLayout)

        binding.recyclerList.layoutManager = LinearLayoutManager(context).apply {
            if (streaming) {
                reverseLayout = true
                stackFromEnd = true
            }
        }
        binding.recyclerList.adapter = adapter
    }
}