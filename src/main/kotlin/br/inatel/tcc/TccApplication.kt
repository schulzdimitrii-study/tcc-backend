package br.inatel.tcc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TccApplication

fun main(args: Array<String>) {
    runApplication<TccApplication>(*args)
}
