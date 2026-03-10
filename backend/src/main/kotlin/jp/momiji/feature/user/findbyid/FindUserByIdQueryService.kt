package jp.momiji.feature.user.findbyid

import iss.jooq.generated.tables.references.USERS
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.LocalDateTime

data class UserView(
  val id: String,
  val email: String,
  val name: String,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
)

@Component
class FindUserByIdQueryService(
  private val dsl: DSLContext
) {
  fun findById(id: String): UserView? {
    return dsl.select(
        USERS.ID,
        USERS.EMAIL,
        USERS.NAME,
        USERS.CREATED_AT,
        USERS.UPDATED_AT,
      )
      .from(USERS)
      .where(USERS.ID.eq(id))
      .fetchOne { record ->
        UserView(
          id = record[USERS.ID]!!,
          email = record[USERS.EMAIL]!!,
          name = record[USERS.NAME]!!,
          createdAt = record[USERS.CREATED_AT]!!,
          updatedAt = record[USERS.UPDATED_AT]!!,
        )
      }
  }
}
