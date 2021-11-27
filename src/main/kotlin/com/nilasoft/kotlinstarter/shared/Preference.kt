package com.nilasoft.kotlinstarter.shared

import com.fasterxml.jackson.databind.ObjectMapper
import com.nilasoft.kotlinstarter.core.buildOf
import org.apache.commons.lang3.ClassUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import kotlin.reflect.KClass

@Entity
@Table(name = "preferences")
class PreferenceEntity {

  @Id
  var key: String? = null

  @Column
  var type: String? = null

  @Column(columnDefinition = "text")
  var value: String? = null

}

@Repository
interface PreferenceRepository : JpaRepository<PreferenceEntity, String>

interface PreferenceService {

  fun <T : Any> write(key: String, type: KClass<T>, value: T?): T?

  fun <T : Any> write(key: String, value: T): T

  fun <T : Any> read(key: String, type: KClass<T>, def: () -> T?): T?

  fun <T : Any> read(key: String, type: KClass<T>, def: T?): T?

  fun <T : Any> read(key: String, def: () -> T): T

  fun <T : Any> read(key: String, def: T): T

  fun <T : Any> read(key: String): T?

  fun keys(): List<String>

  fun has(key: String): Boolean

  fun delete(key: String)

  operator fun <T : Any> set(key: String, type: KClass<T>, value: T?) = write(key, type, value)

  operator fun <T : Any> set(key: String, value: T) = write(key, value)

  operator fun <T : Any> get(key: String, type: KClass<T>, def: () -> T?) = read(key, type, def)

  operator fun <T : Any> get(key: String, type: KClass<T>, def: T?) = read(key, type, def)

  operator fun <T : Any> get(key: String, def: () -> T) = read(key, def)

  operator fun <T : Any> get(key: String, def: T) = read(key, def)

  operator fun <T : Any> get(key: String) = read<T>(key)

  operator fun contains(key: String) = has(key)

  operator fun minusAssign(key: String) = delete(key)

}

@Service
@Suppress("FunctionName")
class PreferenceServiceImp @Autowired constructor(
  private val repository: PreferenceRepository,
  private val mapper: ObjectMapper
) : PreferenceService {

  override fun <T : Any> write(key: String, type: KClass<T>, value: T?): T? {
    _write(key, type, value)
    return value
  }

  override fun <T : Any> write(key: String, value: T): T {
    _write(key, value::class, value)
    return value
  }

  override fun <T : Any> read(key: String, type: KClass<T>, def: () -> T?) = _read<T?>(key) {
    val value = def()
    _write(key, type, value)
  }

  override fun <T : Any> read(key: String, type: KClass<T>, def: T?) = _read<T?>(key) { _write(key, type, def) }

  override fun <T : Any> read(key: String, def: () -> T) = _read<T>(key) {
    val value = def()
    _write(key, value::class, value)
  }

  override fun <T : Any> read(key: String, def: T) = _read<T>(key) { _write(key, def::class, def) }

  override fun <T : Any> read(key: String) = _read<T?>(key) { null }

  override fun keys(): List<String> {
    val preferences = repository.findAll()
    return preferences.map { it.key!! }
  }

  override fun has(key: String) = repository.existsById(key)

  override fun delete(key: String) = repository.deleteById(key)

  private fun <T : Any?> _write(key: String, type: KClass<*>, value: T): PreferenceEntity {
    val klass = ClassUtils.primitiveToWrapper(type.java)
    val preference = buildOf<PreferenceEntity> {
      this.key = key
      this.type = klass.name
      this.value = mapper.writeValueAsString(value)
    }
    return repository.save(preference)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T : Any?> _read(key: String, fallback: () -> PreferenceEntity?): T {
    val optional = repository.findById(key)
    val preference = optional.orElseGet(fallback) ?: return null as T
    val klass = Class.forName(preference.type) as Class<T>
    return mapper.readValue(preference.value, klass)
  }

}
