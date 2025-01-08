package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.core.util.Parcelizer
import kotlinx.serialization.Serializable

// {"type":"IPCIDR","payload":"106.14.255.172/32","proxy":"DIRECT"}
@Serializable
data class Rule(
    val type: String,
    val payload: String,
    val proxy: String,
    val resolve: Boolean = false
): Parcelable {

    enum class Type(val value: String) {
        Domain("DOMAIN"),
        DomainSuffix("DOMAIN-SUFFIX"),
        DomainKeyword("DOMAIN-KEYWORD"),
        GEOIP("GEOIP"),
        IPCIDR("IP-CIDR"),
        SrcIPCIDR("SRC-IP-CIDR"),
        SrcPort("SRC-PORT"),
        DstPort("DST-PORT"),
        InboundPort("INBOUND-PORT"),
        Process("PROCESS-NAME"),
        IPSet("IPSET"),
        MATCH("MATCH")
    }

    fun getNoResolve(): String {
        return if (resolve) "no-resolve" else ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return """{"type": "$type", "payload": "$payload", "proxy": "$proxy", "resolve": $resolve}""""
    }

    companion object CREATOR : Parcelable.Creator<Rule> {
        override fun createFromParcel(parcel: Parcel): Rule {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<Rule?> {
            return arrayOfNulls(size)
        }
    }
}