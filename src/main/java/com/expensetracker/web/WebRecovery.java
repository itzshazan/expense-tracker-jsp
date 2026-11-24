package com.expensetracker.web;

import com.expensetracker.service.ServiceException;
import com.expensetracker.supabase.SupabaseClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

final class WebRecovery {
    private final SupabaseClient client;
    private final ObjectMapper json = new ObjectMapper();
    private final String callback;

    WebRecovery(SupabaseClient client, String callback) {
        this.client = client;
        this.callback = callback;
    }

    Pkce send(String email) {
        Pkce pkce = Pkce.create(Duration.ofHours(1));
        var payload = json.createObjectNode().put("email", email)
                .put("code_challenge", pkce.challenge()).put("code_challenge_method", "s256");
        var response = client.send("/auth/v1/recover?redirect_to="
                + URLEncoder.encode(callback, StandardCharsets.UTF_8), "POST", payload.toString(), null);
        if (response.statusCode() / 100 != 2) {
            throw new ServiceException("Could not send the reset email. Wait a moment and try again.");
        }
        return pkce;
    }

    void reset(Pkce challenge, String code, char[] password) {
        String accessToken = null;
        try {
            if (challenge == null || challenge.expired()) {
                throw new ServiceException("Request a new reset email and open it in this browser.");
            }
            var payload = json.createObjectNode().put("auth_code", code)
                    .put("code_verifier", challenge.verifier());
            var response = client.send("/auth/v1/token?grant_type=pkce", "POST", payload.toString(), null);
            if (response.statusCode() / 100 != 2) {
                throw new ServiceException("The recovery link expired or was already used.");
            }
            accessToken = json.readTree(response.body()).path("access_token").asText("");
            if (accessToken.isBlank()) {
                throw new ServiceException("The recovery link did not establish a session.");
            }
            var result = client.send("/auth/v1/user", "PUT",
                    json.createObjectNode().put("password", new String(password)).toString(), accessToken);
            if (result.statusCode() / 100 != 2) {
                throw new ServiceException("The password could not be changed. Request a new reset link.");
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new ServiceException("The recovery response could not be read.", exception);
        } finally {
            Arrays.fill(password, '\0');
            if (accessToken != null && !accessToken.isBlank()) {
                try {
                    client.send("/auth/v1/logout?scope=local", "POST", "{}", accessToken);
                } catch (ServiceException ignored) {
                    // Do not replace the password update result with a sign-out error.
                }
            }
        }
    }
}
