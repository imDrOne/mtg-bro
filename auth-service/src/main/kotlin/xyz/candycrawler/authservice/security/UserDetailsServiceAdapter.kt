package xyz.candycrawler.authservice.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import xyz.candycrawler.authservice.domain.user.repository.UserRepository

@Service
class UserDetailsServiceAdapter(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("Authentication failed")

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.email)
            .password(user.passwordHash)
            .disabled(!user.enabled)
            .authorities(SimpleGrantedAuthority("ROLE_USER"))
            .build()
    }
}
