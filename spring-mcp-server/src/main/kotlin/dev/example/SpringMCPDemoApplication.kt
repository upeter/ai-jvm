package dev.example

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class SpringMCPDemoApplication

fun main(args: Array<String>) {
    SpringApplication.run(SpringMCPDemoApplication::class.java, *args)
}
