package com.example

import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
import io.ktor.server.cio.*
import io.ktor.server.engine.*

fun main() {
    Util.connect()
    embeddedServer(CIO, port = System.getenv("PORT").toInt()) {
        configureRouting()
        configureSerialization()
//        configureSockets()
    }.start(wait = true)

}



