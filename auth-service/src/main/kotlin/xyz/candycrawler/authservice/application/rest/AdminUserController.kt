package xyz.candycrawler.authservice.application.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import xyz.candycrawler.authservice.application.rest.dto.request.CreateAdminUserRequest
import xyz.candycrawler.authservice.application.rest.dto.response.UserResponse
import xyz.candycrawler.authservice.application.service.AdminUserService
import xyz.candycrawler.authservice.domain.user.model.UserFilter
import xyz.candycrawler.common.pagination.PageRequest
import xyz.candycrawler.common.pagination.PageResponse
import xyz.candycrawler.common.pagination.SortDir

@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController(private val adminUserService: AdminUserService) {

    companion object {
        private val ALLOWED_SORT_FIELDS = setOf("createdAt", "email")
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_api:admin:users:read')")
    fun listUsers(
        @RequestParam email: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "DESC") sortDir: SortDir,
    ): PageResponse<UserResponse> {
        if (sortBy !in ALLOWED_SORT_FIELDS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "sortBy must be one of $ALLOWED_SORT_FIELDS")
        }
        val filter = UserFilter(email)
        val pageRequest = PageRequest(page = page, size = size, sortBy = sortBy, sortDir = sortDir)
        return adminUserService.listUsers(filter, pageRequest).let { p ->
            PageResponse(
                items = p.items.map { it.toResponse() },
                page = p.page,
                size = p.size,
                totalItems = p.totalItems,
                totalPages = p.totalPages,
            )
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_api:admin:users:create')")
    fun createAdminUser(@RequestBody request: CreateAdminUserRequest): ResponseEntity<UserResponse> {
        val user = adminUserService.createAdminUser(request.email, request.username, request.password)
        return ResponseEntity.status(HttpStatus.CREATED).body(user.toResponse())
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAuthority('PERM_api:admin:users:block')")
    fun blockUser(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        adminUserService.blockUser(id, authentication.name)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasAuthority('PERM_api:admin:users:block')")
    fun unblockUser(@PathVariable id: Long): ResponseEntity<Void> {
        adminUserService.unblockUser(id)
        return ResponseEntity.noContent().build()
    }

    private fun xyz.candycrawler.authservice.domain.user.model.User.toResponse() = UserResponse(
        id = id!!,
        email = email,
        username = username,
        enabled = enabled,
        createdAt = createdAt,
    )
}
