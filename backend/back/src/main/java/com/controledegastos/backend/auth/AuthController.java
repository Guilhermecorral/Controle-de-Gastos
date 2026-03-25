package com.controledegastos.backend.auth;

import com.controledegastos.backend.auth.dto.AuthResponseDTO;
import com.controledegastos.backend.auth.dto.LoginRequestDTO;
import com.controledegastos.backend.auth.dto.RegisterRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// @RestController: combines @Controller + @ResponseBody — every response goes as JSON
// @RequestMapping: prefix for all routes of this controller
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/register — new user registration
    // @Valid: triggers the validations of the @NotBlank, @Email in RegisterRequestDTO
    // @RequestBody: reads the JSON from the request body and converts it to the DTO
    // ResponseEntity: allows controlling the HTTP status of the response
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO dto
    ){
        AuthResponseDTO response = authService.register(dto);
        return ResponseEntity
            .status(HttpStatus.CREATED) // 201 Created;
            .body(response);
    }

    //POST /api/auth/login - user authentication existing
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto
    ) {
        AuthResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response); // 200 OK
    }
}
