package com.youversion.platform.helpers

class FixtureLoadingException(
    message: String,
) : Exception(message)

class FixtureLoader {
    fun loadFixtureString(filename: String): String =
        this::class.java.classLoader
            ?.getResourceAsStream("$filename.json")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw FixtureLoadingException("Could not load resource")
}
