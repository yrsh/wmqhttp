package main

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

object Helpers {
    private val mapper = ObjectMapper().registerModule(KotlinModule())

    @Throws(Exception::class)
    fun <T> parseJSON(data: String, type: Class<T>): T {
        return mapper.readValue(data, type)
    }

}
