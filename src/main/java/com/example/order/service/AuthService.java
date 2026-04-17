package com.example.order.service;

import com.example.order.domain.Member;
import com.example.order.domain.Role;
import com.example.order.dto.auth.LoginRequest;
import com.example.order.dto.auth.RegisterRequest;
import com.example.order.dto.auth.TokenResponse;
import com.example.order.repository.MemberRepository;
import com.example.order.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Authentication service — handles register, login, refresh, logout.
 *
 * Refresh Token strategy:
 *   - Stored in Redis: key = "refresh:{memberId}", value = refresh token string
 *   - TTL = refresh token expiration (7 days)
 *   - On refresh: validates token + checks it matches Redis → issues new pair (rotation)
 *   - On logout: deletes Redis key → refresh token becomes invalid
 *   - Access token is stateless (cannot be revoked), but has short TTL (30min)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already registered: " + request.getEmail());
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member = Member.createMember(
                request.getName(), request.getEmail(), encodedPassword, Role.USER);
        memberRepository.save(member);

        log.info("[Auth] Registered: memberId={}, email={}", member.getId(), member.getEmail());
        return issueTokens(member);
    }

    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("[Auth] Login: memberId={}, email={}", member.getId(), member.getEmail());
        return issueTokens(member);
    }

    /**
     * Refresh token rotation:
     *   1. Validate the refresh token (JWT signature + expiration)
     *   2. Compare with stored token in Redis (prevents reuse of old tokens)
     *   3. Issue a new access + refresh token pair
     *   4. Store the new refresh token in Redis (replaces old one)
     */
    public TokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        String redisKey = REFRESH_KEY_PREFIX + memberId;

        // Verify stored token matches — prevents reuse of rotated-out tokens
        String storedToken = stringRedisTemplate.opsForValue().get(redisKey);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            // Token reuse detected → revoke all sessions for this user
            stringRedisTemplate.delete(redisKey);
            throw new IllegalArgumentException("Refresh token mismatch. Re-login required.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        log.info("[Auth] Token refreshed: memberId={}", memberId);
        return issueTokens(member);
    }

    /**
     * Logout — delete refresh token from Redis.
     * Access token remains valid until it expires (stateless),
     * but the user cannot get a new access token without re-logging in.
     */
    public void logout(Long memberId) {
        stringRedisTemplate.delete(REFRESH_KEY_PREFIX + memberId);
        log.info("[Auth] Logout: memberId={}", memberId);
    }

    // === Internal === //

    private TokenResponse issueTokens(Member member) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                member.getId(), member.getEmail(), member.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

        // Store refresh token in Redis with TTL
        stringRedisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + member.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS);

        return TokenResponse.of(accessToken, refreshToken, member.getId(), member.getRole().name());
    }
}
