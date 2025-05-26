package dev.example

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class SpringMCPServerApplication

fun main(args: Array<String>) {
    SpringApplication.run(SpringMCPServerApplication::class.java, *args)
}
