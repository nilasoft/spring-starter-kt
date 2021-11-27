package com.nilasoft.kotlinstarter.security

import com.nilasoft.kotlinstarter.core.*
import org.springframework.context.ApplicationContext
import org.springframework.data.jpa.domain.Specification
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class PropertyEntity : BaseEntity() {

  @JoinColumn
  @ManyToOne(fetch = FetchType.LAZY)
  var owner: UserEntity? = null

}

interface PropertyService<P : PropertyEntity, R : BaseRepository<P>> : BaseService<P, R> {

  fun limitation(): Specification<P>?

}

abstract class PropertyServiceImp<P : PropertyEntity, R : BaseRepository<P>>(
  context: ApplicationContext,
  repository: R
) : BaseServiceImp<P, R>(context, repository), PropertyService<P, R> {

  override fun create(e: P): P {
    e.owner = e.owner ?: AuthContext.user
    return super.create(e)
  }

  override fun update(s: P, e: P): P {
    e.owner = s.owner
    return super.update(s, e)
  }

  override fun where(spec: Specification<P>?): Specification<P> {
    val w = super.where(spec)
    return if (AuthContext.permission?.limited == true) w * limitation() else w
  }

  override fun limitation() = !expression {
    equal {
      x {
        attribute {
          path(PropertyEntity::owner.name)
        }
      }
      y {
        literal {
          value(AuthContext.user)
        }
      }
    }
  }

}
