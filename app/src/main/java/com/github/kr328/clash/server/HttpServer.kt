package com.github.kr328.clash.server

import android.content.Context
import android.net.Uri
import com.github.kr328.clash.BaseActivity
import com.github.kr328.clash.util.startClashService
import androidx.activity.result.contract.ActivityResultContracts
import com.github.kr328.clash.R
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket


class HttpServer(private val context: Context, private val port: Int = 6330): CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private var server: ServerSocket? = null
    private lateinit var client: Socket

    private val clashRunning
        get() = Remote.broadcasts.clashRunning

    init {
        launch {
            try {
                if (server != null) return@launch
                server = ServerSocket()
                server?.let {
                    it.bind(InetSocketAddress("0.0.0.0", port))
                    while (true) {
                        val client = it.accept()
                        Thread {
                            runBlocking {
                                handleRequest(client)
                            }
                        }.start()
                    }
                }
            } catch (e: Exception) {
                com.github.kr328.clash.common.log.Log.e(e.message ?: "", e.fillInStackTrace())
            }
        }
    }

    // 解析 http 请求头
    private fun parseRequest(request: String): HttpRequest {
        val lines = request.split("\n")
        val firstLine = lines[0].split(" ")
        val method = firstLine[0]
        val srcPath = if (firstLine.size >=2) firstLine[1] else "/"
        val version = if (firstLine.size >=3) firstLine[2] else "HTTP/1.1"
        val uri = Uri.parse("http://localhost${srcPath}")
        val path = uri.path ?: "/"
        // 解析参数
        val params = mutableMapOf<String, String>()

        uri.queryParameterNames.forEach {
            params[it] = uri.getQueryParameter(it)!!
        }

        // 解析 headers

        val headers = mutableMapOf<String, String>()

        for (i in 1 until lines.size) {

            val line = lines[i]

            if (line.isEmpty()) break

            val header = line.split(":")

            headers[header[0].trim()] = if (header.size > 1) header[1].trim() else ""
        }
        // 解析body
        val body = lines.last()

        return HttpRequest(method, path, version, headers, body, params)
    }

    private fun sendResponse(code: Int, mineType: String, data: String?) {
        val response = StringBuilder()

        val status = when(code) {
            200 -> "OK"
            404 -> "Not Found"
            else -> "Internal Server Error"
        }

        // 构建响应状态
        response.append("HTTP/1.1 $code $status\r\n")
        // 构建响应头
        response.append("Content-Type: $mineType; charset=utf-8\r\n")
        response.append("Content-Length: ${data?.length ?: 0}\r\n")
        // 跨域支持
        response.append("Access-Control-Allow-Origin: *\r\n")
        response.append("Access-Control-Allow-Methods: GET, POST\r\n")
        response.append("Access-Control-Allow-Headers: *\r\n")

        response.append("Server: ClashForAndroid\r\n")
        response.append("Connection: close\r\n\r\n")
        // 构建响应体
        response.append(data)

        PrintWriter(client.getOutputStream(), true).use {
            it.println(response)
            it.flush()
        }
    }

    private fun sendJson(response: HttpResponse) {
        sendResponse(200, "application/json", response.toJson())
    }

    private fun success(data: Any? = null) {
        sendJson(HttpResponse.success(data))
    }

    private suspend fun handleRequest(socket: Socket) {
        BufferedReader(InputStreamReader(withContext(Dispatchers.IO) {
            socket.getInputStream()
        })).use { reader ->
            var line: String?
            val request = StringBuilder()
            try {
                while (reader.readLine().also { line = it } != null) {
                    request.append(line).append("\n")
                    if (line!!.isEmpty()) break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val req = parseRequest(request.toString())

            dispatch(socket, req)
        }
    }

    private suspend fun dispatch(socket: Socket, req: HttpRequest) {
        client = socket

        when (req.path) {
            "/clash/start" -> clashStart()
            "/clash/stop" -> clashStop()
            "/clash/status" -> clashStatus()

            "/config/add" -> clashConfigAdd(req)
            "/config/delete" -> clashConfigDelete(req)
            "/config/delete-all" -> clashConfigDelete(req, true)
            "/config/update" -> clashConfigUpdate(req)
            "/config/update-all" -> clashConfigUpdate(req, true)
            "/config/select" -> clashConfigSelect(req)

            "/proxy/select" -> clashProxySelect(req)
            "/proxy/query-flow" -> clashProxyQueryFlow()

            "/config/log" -> clashLogs()
            else -> notPath(request = req)
        }

    }

    private suspend fun clashStart() {
        val active = withProfile { queryActive() }
        if (active == null || !active.imported) {
            sendJson(HttpResponse(-1, "No active profile", null))
            return
        }
        val vpnRequest = context.startClashService()
        if (vpnRequest != null) {
            val result = (context as BaseActivity<*>).startActivityForResult(
                ActivityResultContracts.StartActivityForResult(),
                vpnRequest)
            if (result.resultCode == -1) {
                context.startClashService()
            }
        }
        success()
    }

    private fun clashStop() {
        context.stopClashService()
        success()
    }

    private fun clashStatus() {
        sendJson(HttpResponse(0, if (clashRunning) "running" else "stopped", clashRunning))
    }

    private suspend fun clashConfigAdd(request: HttpRequest) {
        withProfile {
            val name = request.params["name"] ?: context.getString(R.string.new_profile)
            val url = request.params["url"] ?: ""
            val uuid = queryAll().find { it.name == name }?.uuid ?: create(Profile.Type.Url, name)

            if (url == "") {
                sendJson(HttpResponse(-2, "params url is null", null))
                return@withProfile
            }
            try {
                patch(uuid, name, url, 0)
                coroutineScope {
                    commit(uuid)
                }
            } catch (e: Exception) {
                delete(uuid)
                sendJson(HttpResponse(-1, e.toString(), null))
                return@withProfile
            }
            success()
            return@withProfile
        }
    }

    private suspend fun clashConfigDelete(request: HttpRequest, all: Boolean = false) {
        val name = request.params["name"]
        if (name == null || name == "") {
            sendJson(HttpResponse(2, "name is null", null))
            return
        }
        withProfile {
            queryAll().forEach {
                if (all) {
                    delete(it.uuid)
                } else if (it.name == name) {
                    delete(it.uuid)
                    success()
                    return@withProfile
                }
            }
            success()
        }
    }

    private suspend fun clashConfigUpdate(request: HttpRequest, all: Boolean = false) {
        val name = request.params["name"]
        if (name == null || name == "") {
            sendJson(HttpResponse(2, "name is null", null))
            return
        }
        withProfile {
            queryAll().forEach {
                if (all) {
                    update(it.uuid)
                } else if (it.name == name) {
                    update(it.uuid)
                    success()
                    return@withProfile
                }
            }
            success()
        }
    }

    private suspend fun clashConfigSelect(request: HttpRequest) {
        val name = request.params["name"]
        if (name == null || name == "") {
            sendJson(HttpResponse(2, "name is null", null))
            return
        }
        withProfile {
            var isOk = false
            for (profile in queryAll()) {
                if (profile.name == name) {
                    setActive(profile)
                    isOk = true
                    break
                }
            }
            if (isOk) {
                success()
            } else {
                sendJson(HttpResponse(-1, "No such profile [name=$name]", null))
            }
        }
    }

    private suspend fun clashProxySelect(request: HttpRequest) {
        if (!clashRunning) {
            sendJson(HttpResponse(-101, "Clash not running", null))
            return
        }
        val name = request.params["name"] ?: ""
        val group = request.params["group"] ?: ""

        val isOk = withClash {
            return@withClash patchSelector(group, name)
        }
        if (!isOk) {
            sendJson(HttpResponse(-100, "failed", null))
        } else {
            success()
        }
    }

    private fun clashProxyQueryFlow() {
        success()
    }

    private fun clashLogs() {
        val list = ArrayList<Log>()
        File(context.filesDir, "clash/clash.log").apply {
            if (exists()) {
                readText().split("\n").filter { it.trim() != "" }.forEach {
                    try {
                        val j = Gson().fromJson(it, Log::class.java)
                        list.add(j)
                    } catch (e: Exception) {
                        e.printStackTrace(System.err)
                    }
                }
            }
        }
        success(list)
    }


    // 404
    private fun notPath(request: HttpRequest) {
        sendResponse(404, "text/html", "[${request.path}]:Not Found")
    }

}