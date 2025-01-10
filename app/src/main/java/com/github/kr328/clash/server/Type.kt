package com.github.kr328.clash.server

import com.google.gson.Gson

data class Log(
    val remote: String,
    val rule: String,
    val payload: String,
    val chain: String,
    val size: List<Long>,
    val time: String,
    val protocol: String,
    val err: String)


data class HttpRequest(
    val method: String,
    val path: String,
    val version: String,
    val headers: Map<String, String>,
    val body: String,
    val params: Map<String, String>
)

data class HttpResponse(
    val code: Int,
    val message: String,
    val data: Any?) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
    companion object {
        fun success(data: Any?): HttpResponse {
            return HttpResponse(0, "success", data)
        }
    }
}