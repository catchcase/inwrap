package org.inwrap

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class InWrapApplication

fun main(args: Array<String>) {
    SpringApplication.run(InWrapApplication::class.java, *args)
}
