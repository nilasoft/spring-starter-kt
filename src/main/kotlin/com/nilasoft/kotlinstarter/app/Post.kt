package com.nilasoft.kotlinstarter.app

import com.github.javafaker.Faker
import com.nilasoft.kotlinstarter.core.BaseControllerImp
import com.nilasoft.kotlinstarter.core.BaseRepository
import com.nilasoft.kotlinstarter.core.Feature
import com.nilasoft.kotlinstarter.core.buildOf
import com.nilasoft.kotlinstarter.security.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object PostConstant {

  const val FEATURE = "posts"

}

@Entity
@Table(name = "posts")
class PostEntity : PropertyEntity() {

  @Column
  var title: String? = null

  @Column(columnDefinition = "text")
  var body: String? = null

}

@Repository
interface PostRepository : BaseRepository<PostEntity>

@Service
class PostService @Autowired constructor(
  context: ApplicationContext,
  repository: PostRepository,
  private val userService: UserService,
  private val faker: Faker
) : PropertyServiceImp<PostEntity, PostRepository>(context, repository) {

  @Transactional
  override fun populate() {
    super.populate()
    val authors = userService.readAllByExp {
      equal {
        x {
          attribute {
            path("${UserEntity::role.name}.${RoleEntity::name.name}")
          }
        }
        y {
          literal {
            value(UserConstant.AUTHOR)
          }
        }
      }
    }
    repeat(10) {
      val post = buildOf<PostEntity> {
        owner = faker.options().nextElement(authors)
        title = faker.name().title()
        body = faker.lorem().sentence()
      }
      createOne(post)
    }
  }

}

@Permitted
@Authenticated
@RestController
@RequestMapping("/posts")
@Feature(PostConstant.FEATURE)
class PostController @Autowired constructor(service: PostService) :
  BaseControllerImp<PostEntity, PostService>(service)
