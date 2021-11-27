package com.nilasoft.kotlinstarter.app

import com.nilasoft.kotlinstarter.core.CommonConstant
import com.nilasoft.kotlinstarter.security.*
import com.nilasoft.kotlinstarter.shared.AssetConstant
import com.nilasoft.kotlinstarter.shared.AssetService
import com.nilasoft.kotlinstarter.shared.PreferenceService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

object AppConstant {

  const val INITIALIZED = "initialized"

}

@Configuration
class AppConfig @Autowired constructor(private val authInterceptor: AuthInterceptor) : WebMvcConfigurer {

  override fun addInterceptors(registry: InterceptorRegistry) {
    val registration = registry.addInterceptor(authInterceptor)
    registration.order(Ordered.HIGHEST_PRECEDENCE)
  }

}

@Service
class AppService @Autowired constructor(
  private val preferenceService: PreferenceService,
  private val userService: UserService,
  private val roleService: RoleService,
  private val permissionService: PermissionService,
  private val assetService: AssetService,
  private val profileService: ProfileService,
  private val postService: PostService,
  private val commentService: CommentService
) : ApplicationRunner {

  private val log = LoggerFactory.getLogger(javaClass)

  override fun run(args: ApplicationArguments) {
    init()
    log.info("app started")
  }

  private fun init() {
    if (preferenceService[AppConstant.INITIALIZED, false])
      return
    userService.populate()
    definePermissions()
    assetService.populate()
    profileService.populate()
    postService.populate()
    commentService.populate()
    preferenceService[AppConstant.INITIALIZED] = true
  }

  private fun definePermissions() {
    log.info("defining permissions...")
    val permissions = with(permissionService) {
      object {
        val god = build(CommonConstant.ALL, CommonConstant.ALL)
        val reader = build(CommonConstant.ALL, CommonConstant.READ)
        val assetAllLimited = build(AssetConstant.FEATURE, CommonConstant.ALL, true)
        val profileAllLimited = build(ProfileConstant.FEATURE, CommonConstant.ALL, true)
        val postAllLimited = build(PostConstant.FEATURE, CommonConstant.ALL, true)
        val commentAllLimited = build(CommentConstant.FEATURE, CommonConstant.ALL, true)
      }
    }
    with(roleService) {
      with(permissions) {
        updateOneByName(
          UserConstant.ADMIN,
          !god
        )
        updateOneByName(
          UserConstant.AUTHOR,
          !reader,
          !assetAllLimited,
          !profileAllLimited,
          !postAllLimited,
          !commentAllLimited
        )
        updateOneByName(
          UserConstant.USER,
          !reader,
          !assetAllLimited,
          !profileAllLimited,
          !commentAllLimited
        )
      }
    }
  }

}

@RestController
@RequestMapping("/app")
class AppController {

  @Authenticated
  @GetMapping("/greet")
  @Suppress("unused")
  fun greet() = object {
    val message = "Hello, ${AuthContext.user!!.username}!"
  }

}
