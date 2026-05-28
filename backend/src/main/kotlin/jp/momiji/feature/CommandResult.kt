package jp.momiji.feature

data class Error(
    val message: String,
)

class CommandResult(
    val success: Boolean,
    val error: Error? = null,
) {
    companion object {
        fun success(): CommandResult = CommandResult(success = true)

        fun fail(error: Error): CommandResult = CommandResult(success = false, error = error)
    }

    override fun toString(): String = "CommandResult(success=$success, error=$error)"
}

class UseCaseException(
    val error: Error,
) : Exception("message:[${error.message}]")

fun CommandResult.throwIfError() {
    if (!this.success) throw UseCaseException(this.error!!)
}
