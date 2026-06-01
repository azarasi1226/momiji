package jp.momiji.domain

abstract class ValidationError(
    val field: String,
    val message: String,
)
