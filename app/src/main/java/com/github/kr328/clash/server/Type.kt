package com.github.kr328.clash.server

data class TrafficFlow(val proxy: String, val remote: String, val upload: Long, val uploadTotal: String, val download: Long, val total: String)

data class Log(
    val remote: String,
    val rule: String,
    val payload: String,
    val chain: String,
    val size: List<Long>,
    val time: String,
    val protocol: String,
    val err: String)