package com.nilasoft.kotlinstarter.core

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.github.javafaker.Faker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import kotlin.reflect.full.createInstance

object CommonConstant {

  const val ALL = "*"

  const val CREATE = "create"

  const val READ = "read"

  const val UPDATE = "update"

  const val DELETE = "delete"

}

@Configuration
class CommonProvider {

  @Bean
  fun hibernate5Module(): Hibernate5Module {
    val module = Hibernate5Module()
    module.enable(Hibernate5Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS)
    module.disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION)
    return module
  }

  @Bean
  fun jodaModule() = JodaModule()

  @Bean
  fun faker() = Faker()

}

@Retention
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Feature(val value: String)

@Retention
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Action(val value: String)

inline fun <reified T : Any> buildOf(crossinline block: T.() -> Unit): T {
  val t = T::class.createInstance()
  t.apply(block)
  return t
}

infix fun <T> T.response(status: HttpStatus): ResponseEntity<T> {
  val builder = ResponseEntity.status(status)
  return builder.body(this)
}

fun <T> T.ok() = this response HttpStatus.OK

operator fun <T> Specification<T>?.not() = Specification.where(this)

operator fun <T> Specification<T>.times(that: Specification<T>?) = and(that)

operator fun <T> Specification<T>.plus(that: Specification<T>?) = or(that)
