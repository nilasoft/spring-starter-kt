package com.nilasoft.kotlinstarter.app

import com.github.javafaker.Faker
import com.nilasoft.kotlinstarter.core.BaseControllerImp
import com.nilasoft.kotlinstarter.core.BaseRepository
import com.nilasoft.kotlinstarter.core.Feature
import com.nilasoft.kotlinstarter.core.buildOf
import com.nilasoft.kotlinstarter.security.*
import com.nilasoft.kotlinstarter.shared.AssetEntity
import com.nilasoft.kotlinstarter.shared.AssetService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.persistence.*

object ProfileConstant {

  const val FEATURE = "profiles"

}

@Entity
@Table(name = "profiles")
class ProfileEntity : PropertyEntity() {

  @Column
  var firstName: String? = null

  @Column
  var lastName: String? = null

  @Column
  var birthDate: Date? = null

  @JoinColumn
  @OneToOne(fetch = FetchType.LAZY)
  var avatar: AssetEntity? = null

}

@Repository
interface ProfileRepository : BaseRepository<ProfileEntity>

@Service
class ProfileService @Autowired constructor(
  context: ApplicationContext,
  repository: ProfileRepository,
  private val userService: UserService,
  private val assetService: AssetService,
  private val faker: Faker
) : PropertyServiceImp<ProfileEntity, ProfileRepository>(context, repository) {

  @Transactional
  override fun populate() {
    super.populate()
    val users = userService.readAllByExp {
      `in` {
        x {
          attribute {
            path("${UserEntity::role.name}.${RoleEntity::name.name}")
          }
        }
        value(UserConstant.ADMIN)
        value(UserConstant.AUTHOR)
        value(UserConstant.USER)
      }
    }
    val images = assetService.readAllByExp {
      like {
        x {
          attribute {
            path(AssetEntity::type.name)
          }
        }
        pattern {
          literal {
            value("image/%")
          }
        }
      }
    }
    for (user in users) {
      val profile = buildOf<ProfileEntity> {
        owner = user
        firstName = faker.name().firstName()
        lastName = faker.name().lastName()
        birthDate = faker.date().birthday()
        avatar = faker.options().nextElement(images)
      }
      createOne(profile)
    }
  }

}

@Permitted
@Authenticated
@RestController
@RequestMapping("/profiles")
@Feature(ProfileConstant.FEATURE)
class ProfileController @Autowired constructor(service: ProfileService) :
  BaseControllerImp<ProfileEntity, ProfileService>(service)
