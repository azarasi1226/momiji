package jp.momiji.feature

data class Error(
  val message: String,
)

class CommandResult private constructor(
  val success: Boolean,
  val error: Error? = null
) {
  companion object {
    fun success(): CommandResult {
      return CommandResult(success = true)
    }

    fun fail(error: Error): CommandResult {
      return CommandResult(success = false, error = error)
    }
  }

  override fun toString(): String {
    return "CommandResult(success=$success, error=$error)"
  }
}

class UseCaseException(val error: Error) : Exception( "message:[${error.message}]")

fun CommandResult.throwIfError() {
  if (!this.success) throw UseCaseException(this.error!!)
}