package com.nilasoft.kotlinstarter.security

import com.nilasoft.kotlinstarter.core.*
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.hibernate.Hibernate
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import javax.crypto.SecretKey
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.KClass

@Suppress("ObjectPropertyName")
object AuthContext {

  private val _user = ThreadLocal<UserEntity>()

  private val _permission = ThreadLocal<PermissionEntity>()

  var user: UserEntity?
    get() = _user.get()
    set(value) = _user.set(value)

  var permission: PermissionEntity?
    get() = _permission.get()
    set(value) = _permission.set(value)

}

@Retention
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Authenticated(val value: Boolean = true)

@Retention
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class WithRole(vararg val value: String)

@Retention
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class WithStatus(vararg val value: String)

@Retention
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Permitted(val value: Boolean = true)

data class RegisterRequest(val role: String, val email: String, val username: String, val password: String)

data class LoginRequest(val role: String, val username: String, val password: String)

data class UserUpdateRequest(val email: String?, val username: String?, val password: String?)

data class Authorization(val prefix: String, val token: String)

@ConstructorBinding
@ConfigurationProperties("auth")
data class AuthProperties(val prefix: String, val expiration: Long, val secret: String)

interface AuthService {

  fun register(request: RegisterRequest): Authorization

  fun login(request: LoginRequest): Authorization

  fun readUser(): UserEntity

  fun updateUser(request: UserUpdateRequest): UserEntity

  fun sign(user: UserEntity): Authorization

  fun verify(authorization: Authorization?): UserEntity?

}

@Service
class AuthServiceImp(
  private val userService: UserService,
  private val roleService: RoleService,
  private val properties: AuthProperties
) : AuthService {

  private val key: SecretKey
    get() {
      val bytes = properties.secret.toByteArray()
      return Keys.hmacShaKeyFor(bytes)
    }

  override fun register(request: RegisterRequest): Authorization {
    if (request.role == UserConstant.ADMIN)
      throw ApiException(HttpStatus.FORBIDDEN, ErrorConstant.FORBIDDEN, "oops!")
    val user = userService.build {
      this.role = roleService.readOneByName(request.role)
      email = request.email
      username = request.username
      password = request.password
      status = UserConstant.ACTIVE
    }
    return sign(user)
  }

  override fun login(request: LoginRequest): Authorization {
    val user = userService.readOneByUsername(request.username)
    if (user == null || !userService.passwordMatches(request.password, user))
      throw ApiException(HttpStatus.UNAUTHORIZED, ErrorConstant.FORBIDDEN, "wrong credential")
    if (user.role?.name != request.role)
      throw ApiException(HttpStatus.FORBIDDEN, ErrorConstant.FORBIDDEN, "role doesn't match")
    return sign(user)
  }

  override fun readUser(): UserEntity {
    val user = userService.readOneById(AuthContext.user!!.id!!)
    Hibernate.initialize(user.role)
    Hibernate.initialize(user.role!!.permissions)
    user.role!!.permissions?.forEach { Hibernate.initialize(it.permission) }
    return user
  }

  override fun updateUser(request: UserUpdateRequest): UserEntity {
    val user = readUser()
    if (user.username == UserConstant.ANONYMOUS)
      return user
    user.apply {
      email = request.email ?: email
      username = request.username ?: username
      password = request.password ?: password
    }
    return userService.updateOneById(user.id!!, user)
  }

  override fun sign(user: UserEntity): Authorization {
    operator fun DateTime.not() = toDate()
    val now = DateTime()
    val expiration = now.plus(properties.expiration)
    val subject = "${user.id}"
    val token = Jwts.builder()
      .setIssuedAt(!now)
      .setExpiration(!expiration)
      .signWith(key)
      .setSubject(subject)
      .compact()
    return Authorization(properties.prefix, token)
  }

  override fun verify(authorization: Authorization?): UserEntity? {
    if (authorization == null)
      return userService.getAnonymousUser()
    if (authorization.prefix != properties.prefix)
      throw ApiException(HttpStatus.UNAUTHORIZED, ErrorConstant.FORBIDDEN, "invalid authorization")
    val claim = Jwts.parserBuilder()
      .setSigningKey(key)
      .build()
      .parseClaimsJws(authorization.token)
    val id = claim.body.subject.toLong()
    return userService.readOneById(id)
  }

}

@RestController
@RequestMapping("/auth")
class AuthController @Autowired constructor(private val service: AuthService) {

  @PostMapping("/register")
  fun register(@RequestBody request: RegisterRequest) = service.register(request) response HttpStatus.OK

  @PostMapping("/login")
  fun login(@RequestBody request: LoginRequest) = service.login(request) response HttpStatus.OK

  @Authenticated
  @GetMapping("/user")
  fun user() = service.readUser() response HttpStatus.OK

  @Authenticated
  @PostMapping("/user")
  fun user(@RequestBody request: UserUpdateRequest) = service.updateUser(request) response HttpStatus.OK

}

@Component
class AuthInterceptor @Autowired constructor(private val service: AuthService) : HandlerInterceptor {

  @Transactional(readOnly = true)
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (handler !is HandlerMethod)
      return true
    val checker = AuthChecker(service, request, handler)
    return checker.check()
  }

}

class AuthChecker(
  private val service: AuthService,
  private val request: HttpServletRequest,
  private val handler: HandlerMethod
) {

  fun check() = checkAuthentication() || checkRole() && checkStatus() && checkPermission()

  private fun checkAuthentication(): Boolean {
    val authenticated = getAnnotation(Authenticated::class)
    if (authenticated?.value != true)
      return true
    val authorization = getAuthorization()
    AuthContext.user = service.verify(authorization) ?: throw ApiException(
      HttpStatus.UNAUTHORIZED,
      ErrorConstant.FORBIDDEN,
      "user not found"
    )
    return false
  }

  private fun getAuthorization(): Authorization? {
    val header = request.getHeader(HttpHeaders.AUTHORIZATION)
    val parts = header?.split(" ")
    if (parts?.size != 2)
      return null
    val prefix = parts[0]
    val token = parts[1]
    return Authorization(prefix, token)
  }

  private fun checkRole(): Boolean {
    val role = getAnnotation(WithRole::class)
    if (role == null || AuthContext.user!!.role!!.name in role.value)
      return true
    throw ApiException(HttpStatus.UNAUTHORIZED, ErrorConstant.FORBIDDEN, "role not allowed")
  }

  private fun checkStatus(): Boolean {
    val status = getAnnotation(WithStatus::class)
    if (status == null || AuthContext.user!!.status in status.value)
      return true
    throw ApiException(HttpStatus.FORBIDDEN, ErrorConstant.FORBIDDEN, "status not allowed")
  }

  private fun checkPermission(): Boolean {
    val permitted = getAnnotation(Permitted::class)
    if (permitted?.value != true)
      return true
    val feature = getAnnotation(Feature::class) ?: throw Exception("feature not defined")
    val action = getAnnotation(Action::class) ?: throw Exception("action not defined")
    val permission = findPermission(feature.value, action.value)
    if (permission?.granted != true)
      throw ApiException(HttpStatus.FORBIDDEN, ErrorConstant.FORBIDDEN, "permission denied")
    AuthContext.permission = permission
    return true
  }

  private fun findPermission(feature: String, action: String): PermissionEntity? {
    val permissions = AuthContext.user!!.role!!.permissions
    return permissions
      ?.sortedBy { it.order }
      ?.map { it.permission!! }
      ?.find {
        (it.feature == feature || it.feature == CommonConstant.ALL) &&
          (it.action == action || it.action == CommonConstant.ALL)
      }
  }

  private fun <T : Annotation> getAnnotation(klass: KClass<T>): T? = handler.run {
    method.getDeclaredAnnotation(klass.java) ?: beanType.getDeclaredAnnotation(klass.java)
  }

}
