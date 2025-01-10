package com.github.kr328.clash.design.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.databinding.AdapterProfileBinding
import com.github.kr328.clash.design.ui.ObservableCurrentTime
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.service.model.Profile
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class ProfileAdapter(
    private val context: Context,
    private val onClicked: (Profile) -> Unit,
    private val onMenuClicked: (Profile) -> Unit,
) : RecyclerView.Adapter<ProfileAdapter.Holder>() {
    class Holder(val binding: AdapterProfileBinding) : RecyclerView.ViewHolder(binding.root)

    private val currentTime = ObservableCurrentTime()

    var profiles: List<Profile> = emptyList()

    fun updateElapsed() {
        currentTime.update()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            AdapterProfileBinding
                .inflate(context.layoutInflater, parent, false)
                .also { it.currentTime = currentTime }
        )
    }

    private fun formatFlow(scaled: Long?): String {
        if (scaled == null || scaled <= 0) return "-"
        val mb = 1048576.0
        val gb = 1073741824.0
        val tb = 1099511627776.0
        return when {
            scaled > tb  -> {
                val data = scaled / tb
                String.format(Locale.getDefault(),"%.2f TB", data)
            }
            scaled > gb -> {
                val data = scaled / gb
                String.format(Locale.getDefault(), "%.2f GB", data)
            }
            scaled > mb -> {
                val data = scaled / mb
                String.format(Locale.getDefault(), "%.2f MB", data)
            }
            scaled > 1024 -> {
                val data = scaled / 1024.0
                String.format(Locale.getDefault(), "%.2f KB", data)
            }
            else -> {
                "$scaled B"
            }
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val current = profiles[position]
        val binding = holder.binding

        if (current === binding.profile)
            return

        binding.profile = current
        binding.setClicked {
            onClicked(current)
        }
        binding.setMenu {
            onMenuClicked(current)
        }
        val isShowFlow: Boolean = current.let {
            it.used != null && it.total != null && it.expire != null
        }
        binding.profileFlow.visibility = if (isShowFlow) View.VISIBLE else View.GONE
        if (isShowFlow) {
            val u = formatFlow(current.used)
            val t = formatFlow(current.total)
            val expire = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(current.expire!! * 1000))

            binding.profileFlow.text = context.getString(R.string.profile_flow, u, t, expire)
        }
    }

    override fun getItemCount(): Int {
        return profiles.size
    }
}