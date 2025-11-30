package com.br.frigg

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform