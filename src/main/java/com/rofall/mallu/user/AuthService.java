package com.rofall.mallu.user;

import com.rofall.mallu.common.BusinessException;
import com.rofall.mallu.security.JwtService;
import com.rofall.mallu.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MallUserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(4001, HttpStatus.CONFLICT, "用户名已存在");
        }
        MallUser user = userRepository.save(new MallUser(request.username(), passwordEncoder.encode(request.password())));
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        MallUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(4011, HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!user.getStatus() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(4011, HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile() {
        MallUser user = userRepository.findById(UserContext.requireUserId())
                .orElseThrow(() -> new BusinessException(4041, HttpStatus.NOT_FOUND, "用户不存在"));
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getPoints());
    }

    private AuthResponse toAuthResponse(MallUser user) {
        return new AuthResponse(user.getId(), user.getUsername(), jwtService.createToken(user.getId()), user.getPoints());
    }
}
