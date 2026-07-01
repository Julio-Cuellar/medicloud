package com.jclinical.users.infra.adapters.in.web;

import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.ports.in.RegisterUserUseCase;
import com.jclinical.users.domain.ports.in.RegisterUserUseCase.RegisterUserCommand;
import com.jclinical.users.domain.ports.in.VerifyUserEmailUseCase;
import com.jclinical.users.infra.adapters.in.web.dto.RegisterUserRequest;
import com.jclinical.users.infra.adapters.in.web.dto.UserResponse;
import com.jclinical.users.infra.adapters.in.web.dto.VerifyEmailRequest;
import com.jclinical.users.infra.adapters.out.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyUserEmailUseCase verifyUserEmailUseCase;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterUserRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(
                request.email(),
                request.password(),
                request.fullName(),
                request.clinicName()
        );

        User registeredUser = registerUserUseCase.registerUser(command);
        UserResponse response = userMapper.toResponse(registeredUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody VerifyEmailRequest request) {
        boolean verified = verifyUserEmailUseCase.verifyEmail(request.token());
        if (verified) {
            return ResponseEntity.ok(Map.of("message", "Correo verificado exitosamente y cuenta activada"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Token de verificación inválido o expirado"));
        }
    }
}
