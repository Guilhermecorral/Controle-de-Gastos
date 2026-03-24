package com.controledegastos.backend.auth.dto;

public record AuthResponseDTO (

        String accessToken,     // short-term JWT — sent in the Authorization header
        String refreshToken,    // long-term JWT — to renew the access token
        String name,            // user's name to display in the interface
        String email,           // logged-in user's email
        String role             // "USER" or "ADMIN" — for access control on the front end

) {}
