package com.lmf.finpro.integration.support;

import org.springframework.http.HttpHeaders;

public record TestUser(Long userId, String email, String token) {

    public HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }
}
