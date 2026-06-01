package jp.momiji.domain

class BusinessException(
    val error: BusinessError,
) : Exception("message:[${error.message}]")
