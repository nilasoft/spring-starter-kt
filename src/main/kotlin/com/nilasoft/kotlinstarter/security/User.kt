package com.nilasoft.kotlinstarter.security

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonManagedReference
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.javafaker.Faker
import com.nilasoft.kotlinstarter.core.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.persistence.*

object UserConstant {

  const val FEATURE = "users"

  const val ROLE = "roles"

  const val PERMISSION = "permissions"

  const val ADMIN = "admin"

  const val AUTHOR = "author"

  const val USER = "user"

  const val GHOST = "ghost"

  const val ANONYMOUS = "anonymous"

  const val ACTIVE = "active"

  const val INACTIVE = "inactive"

}

@Entity
@Table(name = "users")
class UserEntity : BaseEntity() {

  @JoinColumn
  @ManyToOne(fetch = FetchType.LAZY)
  var role: RoleEntity? = null

  @Column(unique = true)
  var email: String? = null

  @Column(nullable = false, unique = true)
  var username: String? = null

  @Column
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  var password: String? = null

  @Column
  var status: String? = null

}

@Entity
@Table(name = "roles")
class RoleEntity : BaseEntity() {

  @Column
  var name: String? = null

  @JsonManagedReference
  @JoinColumn(name = "role_id")
  @OneToMany(targetEntity = RolePermissionEntity::class, cascade = [CascadeType.ALL], orphanRemoval = true)
  var permissions: List<RolePermissionEntity>? = null

}

@Entity
@Table(name = "permissions")
class PermissionEntity : BaseEntity() {

  @Column
  var feature: String? = null

  @Column
  var action: String? = null

  @Column
  var limited: Boolean? = null

  @Column
  var granted: Boolean? = null

  operator fun not() = buildOf<RolePermissionEntity> { permission = this@PermissionEntity }

}

@Entity
@Table(name = "roles_permissions")
class RolePermissionEntity : BaseEntity() {

  @JoinColumn
  @JsonBackReference
  @ManyToOne(fetch = FetchType.LAZY)
  var role: RoleEntity? = null

  @JoinColumn
  @ManyToOne(fetch = FetchType.LAZY)
  var permission: PermissionEntity? = null

  @Column(name = "rp_order")
  var order: Int? = null

  infix fun order(o: Int?) = apply { order = o }

}

@Repository
interface UserRepository : BaseRepository<UserEntity>

@Repository
interface RoleRepository : BaseRepository<RoleEntity>

@Repository
interface PermissionRepository : BaseRepository<PermissionEntity>

interface UserService : BaseService<UserEntity, UserRepository> {

  fun build(block: UserEntity.() -> Unit): UserEntity

  fun passwordEncode(user: UserEntity): UserEntity

  fun passwordMatches(password: String, user: UserEntity): Boolean

  fun readOneByUsername(username: String): UserEntity?

  fun getAnonymousUser(): UserEntity?

}

@Service
class UserServiceImp @Autowired constructor(
  context: ApplicationContext,
  repository: UserRepository,
  private val roleService: RoleService,
  private val passwordEncoder: PasswordEncoder,
  private val faker: Faker
) : BaseServiceImp<UserEntity, UserRepository>(context, repository), UserService {

  @Transactional
  override fun populate() {
    super.populate()
    val roles = with(roleService) {
      object {
        val admin = build(UserConstant.ADMIN)
        val author = build(UserConstant.AUTHOR)
        val user = build(UserConstant.USER)
        val ghost = build(UserConstant.GHOST)
      }
    }
    val pass = "12345678"
    build {
      role = roles.ghost
      username = UserConstant.ANONYMOUS
    }
    build {
      role = roles.admin
      email = "info@admin.com"
      username = "admin"
      password = pass
    }
    repeat(6) {
      build {
        role = if (it < 2) roles.author else roles.user
        email = faker.internet().emailAddress()
        username = faker.name().username()
        password = pass
      }
    }
  }

  override fun create(e: UserEntity): UserEntity {
    passwordEncode(e)
    e.status = e.status ?: UserConstant.INACTIVE
    return super.create(e)
  }

  override fun update(s: UserEntity, e: UserEntity): UserEntity {
    if (e.password == null)
      e.password = s.password
    else
      passwordEncode(e)
    return super.update(s, e)
  }

  override fun build(block: UserEntity.() -> Unit): UserEntity {
    val user = buildOf(block)
    return createOne(user)
  }

  override fun passwordEncode(user: UserEntity): UserEntity {
    val password = user.password ?: return user
    user.password = passwordEncoder.encode(password)
    return user
  }

  override fun passwordMatches(password: String, user: UserEntity): Boolean {
    val hash = user.password ?: return false
    return passwordEncoder.matches(password, hash)
  }

  override fun readOneByUsername(username: String) = try {
    readOneByExp {
      or {
        restriction {
          equal {
            x {
              attribute {
                path(UserEntity::username.name)
              }
            }
            y {
              literal {
                value(username)
              }
            }
          }
        }
        restriction {
          equal {
            x {
              attribute {
                path(UserEntity::email.name)
              }
            }
            y {
              literal {
                value(username)
              }
            }
          }
        }
      }
    }
  } catch (exc: NoSuchElementException) {
    null
  }

  override fun getAnonymousUser(): UserEntity? = readOneByUsername(UserConstant.ANONYMOUS)

}

@Service
class RoleService @Autowired constructor(context: ApplicationContext, repository: RoleRepository) :
  BaseServiceImp<RoleEntity, RoleRepository>(context, repository) {

  fun build(name: String, vararg permissions: RolePermissionEntity): RoleEntity {
    val role = buildOf<RoleEntity> {
      this.name = name
      this.permissions = permissions.ordered()
    }
    return createOne(role)
  }

  fun readOneByName(name: String) = readOneByExp {
    equal {
      x {
        attribute {
          path(RoleEntity::name.name)
        }
      }
      y {
        literal {
          value(name)
        }
      }
    }
  }

  fun updateOneByName(name: String, vararg permissions: RolePermissionEntity): RoleEntity {
    val role = readOneByName(name)
    role.permissions = permissions.ordered()
    return writeOne(role)
  }

  private fun Array<out RolePermissionEntity>.ordered() = mapIndexed { i, p -> p.order = p.order ?: i; p; }

}

@Service
class PermissionService @Autowired constructor(context: ApplicationContext, repository: PermissionRepository) :
  BaseServiceImp<PermissionEntity, PermissionRepository>(context, repository) {

  fun build(feature: String, action: String, limited: Boolean = false, granted: Boolean = true): PermissionEntity {
    val permission = buildOf<PermissionEntity> {
      this.feature = feature
      this.action = action
      this.limited = limited
      this.granted = granted
    }
    return createOne(permission)
  }

}

@Authenticated
@RestController
@RequestMapping("/users")
@WithRole(UserConstant.ADMIN)
@Feature(UserConstant.FEATURE)
class UserController @Autowired constructor(service: UserService) : BaseControllerImp<UserEntity, UserService>(service)

@Authenticated
@RestController
@Feature(UserConstant.ROLE)
@RequestMapping("/roles")
@WithRole(UserConstant.ADMIN)
class RoleController @Autowired constructor(service: RoleService) : BaseControllerImp<RoleEntity, RoleService>(service)

@Authenticated
@RestController
@WithRole(UserConstant.ADMIN)
@Feature(UserConstant.PERMISSION)
@RequestMapping("/permissions")
class PermissionController @Autowired constructor(service: PermissionService) :
  BaseControllerImp<PermissionEntity, PermissionService>(service)
