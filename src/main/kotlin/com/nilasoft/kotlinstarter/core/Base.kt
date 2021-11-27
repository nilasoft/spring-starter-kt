package com.nilasoft.kotlinstarter.core

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass
import javax.validation.ConstraintViolationException
import javax.validation.Validator

@MappedSuperclass
abstract class BaseEntity {

  @Id
  @GeneratedValue
  var id: Long? = null

  @CreationTimestamp
  var created: Date? = null

  @UpdateTimestamp
  var updated: Date? = null

  override fun toString() = "${javaClass.simpleName}#$id"

}

@NoRepositoryBean
interface BaseRepository<E : BaseEntity> : JpaRepository<E, Long>, JpaSpecificationExecutor<E>

interface BaseService<E : BaseEntity, R : BaseRepository<E>> {

  val repository: R

  fun populate()

  fun deserialize(e: E): E

  fun serialize(e: E): E

  fun writeOne(e: E): E

  fun createOne(e: E): E

  fun readOneBySpec(spec: Specification<E>? = null): E

  fun readOneByExp(exp: Expressions? = null): E

  fun readOneByExp(block: ExpressionBlock): E

  fun readOneById(id: Long): E

  fun readOne(): E

  fun readManyBySpec(pageable: Pageable, spec: Specification<E>? = null): Page<E>

  fun readManyByExp(pageable: Pageable, exp: Expressions? = null): Page<E>

  fun readManyByExp(pageable: Pageable, block: ExpressionBlock): Page<E>

  fun readMany(pageable: Pageable): Page<E>

  fun readAllBySpec(spec: Specification<E>? = null): List<E>

  fun readAllByExp(exp: Expressions? = null): List<E>

  fun readAllByExp(block: ExpressionBlock): List<E>

  fun readAll(): List<E>

  fun updateOne(s: E, e: E): E

  fun updateOneById(id: Long, e: E): E

  fun deleteOne(e: E): E

  fun deleteOneById(id: Long): E

}

abstract class BaseServiceImp<E : BaseEntity, R : BaseRepository<E>>(
  protected val context: ApplicationContext,
  override val repository: R
) : BaseService<E, R> {

  protected val log = LoggerFactory.getLogger(javaClass) as Logger

  protected val validator = context.getBean(Validator::class.java)

  override fun populate() = log.info("populating...")

  override fun deserialize(e: E) = validate(e)

  override fun serialize(e: E) = e

  override fun writeOne(e: E) = repository.save(e)

  override fun createOne(e: E): E {
    val c = create(e)
    return writeOne(c)
  }

  override fun readOneBySpec(spec: Specification<E>?): E {
    val o = repository.findOne(!spec)
    return o.orElseThrow()
  }

  override fun readOneByExp(exp: Expressions?) = readOneBySpec(!exp)

  override fun readOneByExp(block: ExpressionBlock): E {
    val exp = expression(block)
    return readOneByExp(exp)
  }

  override fun readOneById(id: Long) = readOneByExp {
    equal {
      x {
        attribute {
          path(BaseEntity::id.name)
        }
      }
      y {
        literal {
          value(id)
        }
      }
    }
  }

  override fun readOne() = readOneBySpec()

  override fun readManyBySpec(pageable: Pageable, spec: Specification<E>?) =
    repository.findAll(!spec, pageable) as Page<E>

  override fun readManyByExp(pageable: Pageable, exp: Expressions?) = readManyBySpec(pageable, !exp)

  override fun readManyByExp(pageable: Pageable, block: ExpressionBlock): Page<E> {
    val exp = expression(block)
    return readManyByExp(pageable, exp)
  }

  override fun readMany(pageable: Pageable) = readManyBySpec(pageable)

  override fun readAllBySpec(spec: Specification<E>?) = repository.findAll(!spec) as List<E>

  override fun readAllByExp(exp: Expressions?) = readAllBySpec(!exp)

  override fun readAllByExp(block: ExpressionBlock): List<E> {
    val exp = expression(block)
    return readAllByExp(exp)
  }

  override fun readAll() = readAllBySpec()

  override fun updateOne(s: E, e: E): E {
    val u = update(s, e)
    return writeOne(u)
  }

  override fun updateOneById(id: Long, e: E): E {
    val s = readOneById(id)
    return updateOne(s, e)
  }

  override fun deleteOne(e: E): E {
    repository.delete(e)
    return e
  }

  override fun deleteOneById(id: Long): E {
    val s = readOneById(id)
    repository.delete(s)
    return s
  }

  protected open fun validate(e: E): E {
    val violations = validator.validate(e)
    if (violations.isEmpty())
      return e
    throw ConstraintViolationException(violations)
  }

  protected open fun create(e: E): E {
    e.id = null
    e.created = null
    e.updated = null
    return e
  }

  protected open fun update(s: E, e: E): E {
    e.id = s.id
    e.created = s.created
    e.updated = s.updated
    return e
  }

  protected open fun where(spec: Specification<E>?) = spec.not<E>()

  protected open operator fun Specification<E>?.not() = where(this)

  protected open operator fun Expressions?.not() = toSpec<E>()

}

interface BaseController<E : BaseEntity, S : BaseService<E, *>> {

  val service: S

  fun createOne(e: E) = !-service.createOne(+e)

  fun readOneByExp(exp: Expressions?) = !-service.readOneByExp(exp)

  fun readOneById(id: Long) = !-service.readOneById(id)

  fun readOne() = !-service.readOne()

  fun readManyByExp(pageable: Pageable, exp: Expressions?) = !-service.readManyByExp(pageable, exp)

  fun readMany(pageable: Pageable) = !-service.readMany(pageable)

  fun readAllByExp(exp: Expressions?) = !-service.readAllByExp(exp)

  fun readAll() = !-service.readAll()

  fun updateOneById(id: Long, e: E) = !-service.updateOneById(id, +e)

  fun deleteOneById(id: Long) = !-service.deleteOneById(id)

  operator fun E.unaryPlus() = service.deserialize(this)

  operator fun E.unaryMinus() = service.serialize(this)

  operator fun Page<E>.unaryMinus() = map(service::serialize)

  operator fun List<E>.unaryMinus() = map(service::serialize)

  operator fun E.not() = ok()

  operator fun Page<E>.not() = ok()

  operator fun List<E>.not() = ok()

}

abstract class BaseControllerImp<E : BaseEntity, S : BaseService<E, *>>(override val service: S) :
  BaseController<E, S> {

  @Action(CommonConstant.CREATE)
  @PostMapping("/create/one")
  override fun createOne(@RequestBody e: E) = super.createOne(e)

  @Action(CommonConstant.READ)
  @PostMapping("/read/one/exp")
  override fun readOneByExp(@RequestBody(required = false) exp: Expressions?) = super.readOneByExp(exp)

  @Action(CommonConstant.READ)
  @GetMapping("/read/one/{id}")
  override fun readOneById(@PathVariable id: Long) = super.readOneById(id)

  @Action(CommonConstant.READ)
  @GetMapping("/read/one")
  override fun readOne() = super.readOne()

  @Action(CommonConstant.READ)
  @PostMapping("/read/many/exp")
  override fun readManyByExp(@PageableDefault pageable: Pageable, @RequestBody(required = false) exp: Expressions?) =
    super.readManyByExp(pageable, exp)

  @Action(CommonConstant.READ)
  @GetMapping("/read/many")
  override fun readMany(@PageableDefault pageable: Pageable) = super.readMany(pageable)

  @Action(CommonConstant.READ)
  @PostMapping("/read/all/exp")
  override fun readAllByExp(@RequestBody(required = false) exp: Expressions?) = super.readAllByExp(exp)

  @Action(CommonConstant.READ)
  @GetMapping("/read/all")
  override fun readAll() = super.readAll()

  @Action(CommonConstant.UPDATE)
  @PutMapping("/update/one/{id}")
  override fun updateOneById(@PathVariable id: Long, @RequestBody e: E) = super.updateOneById(id, e)

  @Action(CommonConstant.DELETE)
  @DeleteMapping("/delete/one/{id}")
  override fun deleteOneById(@PathVariable id: Long) = super.deleteOneById(id)

}
