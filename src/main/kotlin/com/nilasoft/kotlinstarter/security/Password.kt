package com.nilasoft.kotlinstarter.security

import com.password4j.Password
import org.springframework.stereotype.Component

interface PasswordEncoder {

  fun encode(plain: String): String

  fun matches(plain: String, hash: String): Boolean

}

@Component
class PasswordEncoderImp : PasswordEncoder {

  override fun encode(plain: String): String {
    val builder = Password.hash(plain)
    val hash = builder.withArgon2()
    return hash.result
  }

  override fun matches(plain: String, hash: String): Boolean {
    val checker = Password.check(plain, hash)
    return checker.withArgon2()
  }

}
