package dev.example

import org.springframework.context.ApplicationContext

import org.slf4j.LoggerFactory

inline val <reified T> T.logger
    get() = LoggerFactory.getLogger(T::class.java)


inline fun <reified T> ApplicationContext.beanOf(name:String):T = this.getBean(name, T::class.java)

inline fun <reified T> ApplicationContext.beanOf():T = this.getBean(T::class.java)