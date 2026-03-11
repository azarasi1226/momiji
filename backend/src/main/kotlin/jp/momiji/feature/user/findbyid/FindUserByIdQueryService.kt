package jp.momiji.feature.user.findbyid

import iss.jooq.generated.tables.references.USERS
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.LocalDateTime

data class UserView(
  val id: String,
  val email: String,
  val name: String,
  val phoneNumber: String,
  val postalCode: String,
  val address1: String,
  val address2: String,
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
        USERS.PHONE_NUMBER,
        USERS.POSTAL_CODE,
        USERS.ADDRESS1,
        USERS.ADDRESS2,
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
          phoneNumber = record[USERS.PHONE_NUMBER]!!,
          postalCode = record[USERS.POSTAL_CODE]!!,
          address1 = record[USERS.ADDRESS1]!!,
          address2 = record[USERS.ADDRESS2]!!,
          createdAt = record[USERS.CREATED_AT]!!,
          updatedAt = record[USERS.UPDATED_AT]!!,
        )
      }
  }
}
