package com.github.kr328.clash

import androidx.activity.OnBackPressedCallback
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.uuid
import com.github.kr328.clash.design.PropertiesDesign
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.showExceptionToast
import com.github.kr328.clash.service.data.ImportedDao
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.HashMap

class PropertiesActivity : BaseActivity<PropertiesDesign>() {
    private var canceled: Boolean = false

    override suspend fun main() {
        setResult(RESULT_CANCELED)

        val uuid = intent.uuid ?: return finish()
        val design = PropertiesDesign(this)

        println("=====================PropertiesActivity======================")
        println(uuid)

        val original = withProfile { queryByUUID(uuid) } ?: return finish()

        design.profile = original

        setContentDesign(design)

        defer {
            canceled = true

            withProfile { release(uuid) }
        }

        while (isActive) {
            select {
                events.onReceive {
                    when (it) {
                        Event.ActivityStop -> {
                            val profile = design.profile

                            if (!canceled && profile != original) {
                                withProfile {
                                    patch(profile.uuid, profile.name, profile.source, profile.interval)
                                }
                            }
                        }
                        Event.ServiceRecreated -> {
                            finish()
                        }
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        PropertiesDesign.Request.BrowseFiles -> {
                            startActivity(FilesActivity::class.intent.setUUID(uuid))
                        }
                        PropertiesDesign.Request.Commit -> {
                            design.verifyAndCommit()
                        }
                    }
                }
            }
        }
        onBackPress()
    }

    private fun onBackPress() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                design?.apply {
                    launch {
                        if (!progressing) {
                            if (requestExitWithoutSaving())
                                finish()
                        }
                    }
                }
            }
        })
    }

    private suspend fun PropertiesDesign.verifyAndCommit() {
        when {
            profile.name.isBlank() -> {
                showToast(R.string.empty_name, ToastDuration.Long)
            }
            profile.type != Profile.Type.File && profile.source.isBlank() -> {
                showToast(R.string.invalid_url, ToastDuration.Long)
            }
            else -> {
                try {
                    withProcessing { updateStatus ->
                        withProfile {
                            patch(profile.uuid, profile.name, profile.source, profile.interval)

                            coroutineScope {
                                commit(profile.uuid) {
                                    launch {
                                        val imported = ImportedDao().queryByUUID(profile.uuid)
                                        it.header?.get("Subscription-Userinfo")?.let { userInfo ->
                                            if (userInfo.isNotEmpty() && imported != null) {
                                                val items = userInfo[0].split("; ")
                                                val mp = HashMap<String, Long>()
                                                for (tc in items) {
                                                    tc.split("=").let { spc ->
                                                        if (spc.size == 2) {
                                                            mp[spc[0].trim()] = spc[1].trim().toLong()
                                                        }
                                                    }
                                                }
                                                val used = (mp["upload"]?: 0) + (mp["download"] ?: 0)
                                                val total = mp["total"]?: 0
                                                val expire = mp["expire"]?: 0
                                                val inc = imported.copy(used = used, total = total, expire = expire)
                                                ImportedDao().update(inc)
                                            }
                                        }
                                        updateStatus(it)
                                    }
                                }
                            }
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