package com.nilasoft.kotlinstarter.shared

import com.github.javafaker.Faker
import com.nilasoft.kotlinstarter.core.*
import com.nilasoft.kotlinstarter.security.*
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.ApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.*
import javax.persistence.*

object AssetConstant {

  const val FEATURE = "assets"

}

@Entity
@Table(name = "assets")
class AssetEntity : PropertyEntity() {

  @Column
  @Enumerated(EnumType.STRING)
  var mode: Mode? = null

  @Column
  var type: String? = null

  @Column
  var name: String? = null

  @Column(columnDefinition = "text")
  var uri: String? = null

  enum class Mode {

    INTERNAL,

    EXTERNAL,

    BASE64

  }

}

@ConstructorBinding
@ConfigurationProperties("assets")
data class AssetProperties(val storage: String, val cdn: String)

@Repository
interface AssetRepository : BaseRepository<AssetEntity>

interface AssetService : PropertyService<AssetEntity, AssetRepository> {

  fun createOne(file: MultipartFile): AssetEntity

}

@Service
class AssetServiceImp @Autowired constructor(
  context: ApplicationContext,
  repository: AssetRepository,
  private val properties: AssetProperties,
  private val userService: UserService,
  private val faker: Faker
) : PropertyServiceImp<AssetEntity, AssetRepository>(context, repository), AssetService {

  @Transactional
  override fun populate() {
    super.populate()
    val anonymous = userService.getAnonymousUser()
    repeat(10) {
      val asset = buildOf<AssetEntity> {
        owner = anonymous
        mode = AssetEntity.Mode.EXTERNAL
        name = faker.name().title()
        type = "image/*"
        uri = faker.internet().image()
      }
      createOne(asset)
    }
  }

  override fun createOne(file: MultipartFile): AssetEntity {
    val asset = store(file)
    return createOne(asset)
  }

  private fun store(multipart: MultipartFile): AssetEntity {
    val uuid = UUID.randomUUID()
    val ext = FilenameUtils.getExtension(multipart.originalFilename)
    val filename = "$uuid.$ext"
    val file = File(properties.storage, filename)
    FileUtils.copyInputStreamToFile(multipart.inputStream, file)
    return buildOf {
      mode = AssetEntity.Mode.INTERNAL
      type = multipart.contentType
      name = multipart.originalFilename
      uri = filename
    }
  }

}

interface AssetController : BaseController<AssetEntity, AssetService> {

  fun createOne(file: MultipartFile): ResponseEntity<AssetEntity>

}

@Permitted
@Authenticated
@RestController
@RequestMapping("/assets")
@Feature(AssetConstant.FEATURE)
class AssetControllerImp @Autowired constructor(service: AssetService) :
  BaseControllerImp<AssetEntity, AssetService>(service), AssetController {

  @Action(CommonConstant.CREATE)
  @PostMapping("/create/one/file")
  override fun createOne(@RequestParam file: MultipartFile) = !-service.createOne(file)

}
