package xyz.candycrawler.authservice.domain.permission.exception

class ApiPermissionNotFoundException(id: Long) : RuntimeException("ApiPermission not found: id=$id")
