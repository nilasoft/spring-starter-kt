package com.nilasoft.kotlinstarter.core

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import org.springframework.data.jpa.domain.Specification
import javax.persistence.criteria.*
import kotlin.reflect.full.createInstance

fun <T> Expressions?.toSpec() = Specification<T> { r, _, cb -> this?.predicate(r, cb) as Predicate? }

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed class Expressions {

  abstract fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*>

  @JsonTypeName("attribute")
  class Attribute(val path: String) : Expressions() {

    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      var p: Path<*> = root
      val parts = path.split(".")
      for (part in parts)
        p = p.get<Any>(part)
      return p
    }

  }

  @JsonTypeName("literal")
  class Literal(val value: Any?) : Expressions() {

    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      return if (value == null)
        cb.nullLiteral(Any::class.java)
      else
        cb.literal(value)
    }

  }

  @JsonTypeName("not")
  class Not(val restriction: Expressions) : Expressions() {

    @Suppress("UNCHECKED_CAST")
    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      val e = restriction.predicate(root, cb) as Expression<Boolean>
      return cb.not(e)
    }

  }

  @JsonTypeName("and")
  class And(val restrictions: List<Expressions>) : Expressions() {

    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      val l = restrictions.map { it.predicate(root, cb) as Predicate }
      val a = l.toTypedArray()
      return cb.and(*a)
    }

  }

  @JsonTypeName("or")
  class Or(val restrictions: List<Expressions>) : Expressions() {

    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      val l = restrictions.map { it.predicate(root, cb) as Predicate }
      val a = l.toTypedArray()
      return cb.or(*a)
    }

  }

  @JsonTypeName("in")
  class In(val x: Expressions, val values: List<Any?>) : Expressions() {

    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      val a = x.predicate(root, cb)
      val v = values.toTypedArray()
      return a.`in`(*v)
    }

  }

  @JsonTypeName("equal")
  class Equal(val x: Expressions, val y: Expressions) : Expressions() {

    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      val a = x.predicate(root, cb)
      val b = y.predicate(root, cb)
      return cb.equal(a, b)
    }

  }

  @JsonTypeName("greater")
  class Greater(val x: Expressions, val y: Expressions) : Expressions() {

    @Suppress("UNCHECKED_CAST")
    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      val a = x.predicate(root, cb) as Expression<Comparable<Any>>
      val b = y.predicate(root, cb) as Expression<Comparable<Any>>
      return cb.greaterThan(a, b)
    }

  }

  @JsonTypeName("greater-equal")
  class GreaterEqual(val x: Expressions, val y: Expressions) : Expressions() {

    @Suppress("UNCHECKED_CAST")
    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      val a = x.predicate(root, cb) as Expression<Comparable<Any>>
      val b = y.predicate(root, cb) as Expression<Comparable<Any>>
      return cb.greaterThanOrEqualTo(a, b)
    }

  }

  @JsonTypeName("less")
  class Less(val x: Expressions, val y: Expressions) : Expressions() {

    @Suppress("UNCHECKED_CAST")
    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      val a = x.predicate(root, cb) as Expression<Comparable<Any>>
      val b = y.predicate(root, cb) as Expression<Comparable<Any>>
      return cb.lessThan(a, b)
    }

  }

  @JsonTypeName("less-equal")
  class LessEqual(val x: Expressions, val y: Expressions) : Expressions() {

    @Suppress("UNCHECKED_CAST")
    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      val a = x.predicate(root, cb) as Expression<Comparable<Any>>
      val b = y.predicate(root, cb) as Expression<Comparable<Any>>
      return cb.lessThanOrEqualTo(a, b)
    }

  }

  @JsonTypeName("between")
  class Between(val v: Expressions, val x: Expressions, val y: Expressions) : Expressions() {

    @Suppress("UNCHECKED_CAST")
    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      val a = v.predicate(root, cb) as Expression<Comparable<Any>>
      val b = x.predicate(root, cb) as Expression<Comparable<Any>>
      val c = y.predicate(root, cb) as Expression<Comparable<Any>>
      return cb.between(a, b, c)
    }

  }

  @JsonTypeName("like")
  class Like(val x: Expressions, val pattern: Expressions) : Expressions() {

    @Suppress("UNCHECKED_CAST")
    override fun predicate(root: Root<*>, cb: CriteriaBuilder): Expression<*> {
      val a = x.predicate(root, cb) as Expression<String>
      val b = pattern.predicate(root, cb) as Expression<String>
      return cb.like(a, b)
    }

  }

}

typealias ExpressionBlock = ExpressionsBuilder.() -> Unit

inline fun expression(crossinline block: ExpressionBlock): Expressions {
  val builder = ExpressionsBuilder()
  builder.apply(block)
  return builder()
}

@DslMarker
annotation class ExpressionMarker

@ExpressionMarker
fun interface ExpressionBuilder<out T : Expressions> {

  operator fun invoke(): T

}

class ExpressionsBuilder : ExpressionBuilder<Expressions> {

  private lateinit var expression: Expressions

  fun attribute(block: AttributeBuilder.() -> Unit) = build(block)

  fun literal(block: LiteralBuilder.() -> Unit) = build(block)

  fun not(block: NotBuilder.() -> Unit) = build(block)

  fun and(block: AndBuilder.() -> Unit) = build(block)

  fun or(block: OrBuilder.() -> Unit) = build(block)

  fun `in`(block: InBuilder.() -> Unit) = build(block)

  fun equal(block: EqualBuilder.() -> Unit) = build(block)

  fun greater(block: GreaterBuilder.() -> Unit) = build(block)

  fun greaterEqual(block: GreaterEqualBuilder.() -> Unit) = build(block)

  fun less(block: LessBuilder.() -> Unit) = build(block)

  fun lessEqual(block: LessEqualBuilder.() -> Unit) = build(block)

  fun between(block: BetweenBuilder.() -> Unit) = build(block)

  fun like(block: LikeBuilder.() -> Unit) = build(block)

  private inline fun <T : Expressions, reified B : ExpressionBuilder<T>> build(block: B.() -> Unit) {
    val builder = B::class.createInstance()
    builder.apply(block)
    expression = builder()
  }

  override fun invoke() = expression

}

class AttributeBuilder : ExpressionBuilder<Expressions.Attribute> {

  private lateinit var path: String

  fun path(p: String) {
    path = p
  }

  override operator fun invoke() = Expressions.Attribute(path)

}

class LiteralBuilder : ExpressionBuilder<Expressions.Literal> {

  private var value: Any? = null

  fun value(v: Any?) {
    value = v
  }

  override operator fun invoke() = Expressions.Literal(value)

}

class NotBuilder : ExpressionBuilder<Expressions.Not> {

  private lateinit var restriction: Expressions

  fun restriction(block: ExpressionBlock) {
    restriction = expression(block)
  }

  override operator fun invoke() = Expressions.Not(restriction)

}

class AndBuilder : ExpressionBuilder<Expressions.And> {

  private val restrictions = mutableListOf<Expressions>()

  fun restriction(block: ExpressionBlock) {
    val restriction = expression(block)
    restrictions.add(restriction)
  }

  override operator fun invoke() = Expressions.And(restrictions)

}

class OrBuilder : ExpressionBuilder<Expressions.Or> {

  private val restrictions = mutableListOf<Expressions>()

  fun restriction(block: ExpressionBlock) {
    val restriction = expression(block)
    restrictions.add(restriction)
  }

  override operator fun invoke() = Expressions.Or(restrictions)

}

class InBuilder : ExpressionBuilder<Expressions.In> {

  private lateinit var x: Expressions

  private var values = mutableListOf<Any?>()

  fun x(block: ExpressionBlock) {
    x = expression(block)
  }

  fun value(v: Any?) {
    values.add(v)
  }

  override operator fun invoke() = Expressions.In(x, values)

}

class EqualBuilder : ExpressionBuilder<Expressions.Equal> {

  private lateinit var x: Expressions

  private lateinit var y: Expressions

  fun x(block: ExpressionBlock) {
    x = expression(block)
  }

  fun y(block: ExpressionBlock) {
    y = expression(block)
  }

  override operator fun invoke() = Expressions.Equal(x, y)

}

class GreaterBuilder : ExpressionBuilder<Expressions.Greater> {

  private lateinit var x: Expressions

  private lateinit var y: Expressions

  fun x(block: ExpressionBlock) {
    x = expression(block)
  }

  fun y(block: ExpressionBlock) {
    y = expression(block)
  }

  override operator fun invoke() = Expressions.Greater(x, y)

}

class GreaterEqualBuilder : ExpressionBuilder<Expressions.GreaterEqual> {

  private lateinit var x: Expressions

  private lateinit var y: Expressions

  fun x(block: ExpressionBlock) {
    x = expression(block)
  }

  fun y(block: ExpressionBlock) {
    y = expression(block)
  }

  override operator fun invoke() = Expressions.GreaterEqual(x, y)

}

class LessBuilder : ExpressionBuilder<Expressions.Less> {

  private lateinit var x: Expressions

  private lateinit var y: Expressions

  fun x(block: ExpressionBlock) {
    x = expression(block)
  }

  fun y(block: ExpressionBlock) {
    y = expression(block)
  }

  override operator fun invoke() = Expressions.Less(x, y)

}

class LessEqualBuilder : ExpressionBuilder<Expressions.LessEqual> {

  private lateinit var x: Expressions

  private lateinit var y: Expressions

  fun x(block: ExpressionBlock) {
    x = expression(block)
  }

  fun y(block: ExpressionBlock) {
    y = expression(block)
  }

  override operator fun invoke() = Expressions.LessEqual(x, y)

}

class BetweenBuilder : ExpressionBuilder<Expressions.Between> {

  private lateinit var v: Expressions

  private lateinit var x: Expressions

  private lateinit var y: Expressions

  fun v(block: ExpressionBlock) {
    v = expression(block)
  }

  fun x(block: ExpressionBlock) {
    x = expression(block)
  }

  fun y(block: ExpressionBlock) {
    y = expression(block)
  }

  override operator fun invoke() = Expressions.Between(v, x, y)

}

class LikeBuilder : ExpressionBuilder<Expressions.Like> {

  private lateinit var x: Expressions

  private lateinit var pattern: Expressions

  fun x(block: ExpressionBlock) {
    x = expression(block)
  }

  fun pattern(block: ExpressionBlock) {
    pattern = expression(block)
  }

  override operator fun invoke() = Expressions.Like(x, pattern)

}
