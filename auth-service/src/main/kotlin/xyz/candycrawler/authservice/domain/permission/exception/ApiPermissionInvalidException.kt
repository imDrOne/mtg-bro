package xyz.candycrawler.authservice.domain.permission.exception

class ApiPermissionInvalidException(reason: String) : RuntimeException("ApiPermission invalid: $reason")
