package jp.momiji.domain

class ValidationException(
    val errors: List<ValidationError>,
) : Exception(
        errors.joinToString(separator = " / ") { "[${it.field}] ${it.message}" },
    )
