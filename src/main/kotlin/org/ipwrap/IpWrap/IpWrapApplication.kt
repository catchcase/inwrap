package org.ipwrap.IpWrap

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class IpWrapApplication

fun main(args: Array<String>) {
    SpringApplication.run(IpWrapApplication::class.java, *args)
}
