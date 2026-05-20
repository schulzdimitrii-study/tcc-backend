package br.inatel.tcc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class TccApplication

fun main(args: Array<String>) {
    runApplication<TccApplication>(*args)
}
