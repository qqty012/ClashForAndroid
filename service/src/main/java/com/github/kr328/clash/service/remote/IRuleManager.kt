package com.github.kr328.clash.service.remote

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.core.model.Rule
import com.github.kr328.clash.core.util.Parcelizer
import com.github.kr328.kaidl.BinderInterface
import kotlinx.serialization.Serializable

@BinderInterface
interface IRuleManager {

    @Serializable
    data class ProxyGroup(val name: String, val type: String, val proxies: List<String>) : Parcelable {
        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            Parcelizer.encodeToParcel(serializer(), dest, this)
        }

        companion object CREATOR : Parcelable.Creator<ProxyGroup> {
            override fun createFromParcel(parcel: Parcel): ProxyGroup {
                return Parcelizer.decodeFromParcel(serializer(), parcel)
            }

            override fun newArray(size: Int): Array<ProxyGroup?> {
                return arrayOfNulls(size)
            }
        }
    }

    suspend fun create(rule: Rule)
    suspend fun delete(rule: Rule)
    suspend fun update(old: Rule, new: Rule)
    suspend fun queryAll(): List<Rule>
    suspend fun getRules(): List<Rule>
    suspend fun getProxyGroup(): List<ProxyGroup>
    suspend fun apply()
}