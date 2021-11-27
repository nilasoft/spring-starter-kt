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
import javax.persistence.*

object CommentConstant {

  const val FEATURE = "comments"

}

@Entity
@Table(name = "comments")
class CommentEntity : PropertyEntity() {

  @JoinColumn
  @ManyToOne(fetch = FetchType.LAZY)
  var post: PostEntity? = null

  @Column(columnDefinition = "text")
  var content: String? = null

}

@Repository
interface CommentRepository : BaseRepository<CommentEntity>

@Service
class CommentService @Autowired constructor(
  context: ApplicationContext,
  repository: CommentRepository,
  private val userService: UserService,
  private val postService: PostService,
  private val faker: Faker
) : PropertyServiceImp<CommentEntity, CommentRepository>(context, repository) {

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
        value(UserConstant.AUTHOR)
        value(UserConstant.USER)
      }
    }
    val posts = postService.readAll()
    for (post in posts) {
      repeat(4) {
        val comment = buildOf<CommentEntity> {
          owner = faker.options().nextElement(users)
          this.post = post
          content = faker.lorem().sentence()
        }
        createOne(comment)
      }
    }
  }

}

@Permitted
@Authenticated
@RestController
@RequestMapping("/comments")
@Feature(CommentConstant.FEATURE)
class CommentController @Autowired constructor(service: CommentService) :
  BaseControllerImp<CommentEntity, CommentService>(service)
