package com.example.bt2.service;

import com.example.bt2.config.SpotifyConfig;
import com.example.bt2.entity.UserToken;
import com.example.bt2.model.SpotifyUser;
import com.example.bt2.model.TokenResponse;
import com.example.bt2.repository.UserTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Service
public class SpotifyAuthService {

    @Autowired
    private SpotifyConfig config;

    @Autowired
    private UserTokenRepository tokenRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public String buildAuthorizationUri() {
        return UriComponentsBuilder.fromUriString(config.getAuthorizationUri())
                .queryParam("client_id", config.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("scope", config.getScope())
                .queryParam("show_dialog", true) // Force user to see auth dialog
                .build()
                .toUriString();
    }

    public UserToken exchangeCodeForToken(String code) {
        TokenResponse tokenResponse = requestTokens(code, "authorization_code", null);
        SpotifyUser user = getUserProfile(tokenResponse.getAccessToken());
        UserToken userToken = saveOrUpdateToken(user.getId(), tokenResponse);
        return userToken;
    }

    public UserToken refreshAccessToken(String userId) {
        Optional<UserToken> tokenOpt = tokenRepository.findByUserId(userId);

        if (tokenOpt.isEmpty()) {
            throw new RuntimeException("No token found for user: " + userId);
        }

        UserToken existingToken = tokenOpt.get();

        if (existingToken.getRefreshToken() == null) {
            throw new RuntimeException("No refresh token available for user: " + userId);
        }

        // Request new access token using refresh token
        TokenResponse tokenResponse = requestTokens(
                existingToken.getRefreshToken(),
                "refresh_token",
                null
        );

        // Update existing token
        existingToken.updateToken(
                tokenResponse.getAccessToken(),
                tokenResponse.getRefreshToken(),
                tokenResponse.getExpiresIn()
        );

        return tokenRepository.save(existingToken);
    }

    public Optional<UserToken> getValidToken(String userId) {
        Optional<UserToken> tokenOpt = tokenRepository.findByUserId(userId);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        UserToken token = tokenOpt.get();

        if (token.isExpired()) {
            try {
                token = refreshAccessToken(userId);
            } catch (Exception e) {
                // If refresh fails, return empty
                return Optional.empty();
            }
        }

        return Optional.of(token);
    }

    private TokenResponse requestTokens(String codeOrRefreshToken, String grantType, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = config.getClientId() + ":" + config.getClientSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", grantType);

        if ("authorization_code".equals(grantType)) {
            form.add("code", codeOrRefreshToken);
            form.add("redirect_uri", config.getRedirectUri());
        } else if ("refresh_token".equals(grantType)) {
            form.add("refresh_token", codeOrRefreshToken);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                config.getTokenUri(), request, TokenResponse.class
        );

        if (response.getBody() == null) {
            throw new RuntimeException("Failed to obtain tokens from Spotify");
        }

        return response.getBody();
    }

    private SpotifyUser getUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<SpotifyUser> response = restTemplate.exchange(
                config.getApiBaseUri() + "/me",
                HttpMethod.GET,
                entity,
                SpotifyUser.class
        );

        if (response.getBody() == null) {
            throw new RuntimeException("Failed to get user profile from Spotify");
        }

        return response.getBody();
    }

    private UserToken saveOrUpdateToken(String userId, TokenResponse tokenResponse) {
        Optional<UserToken> existingTokenOpt = tokenRepository.findByUserId(userId);

        UserToken userToken;
        if (existingTokenOpt.isPresent()) {
            userToken = existingTokenOpt.get();
            userToken.updateToken(
                    tokenResponse.getAccessToken(),
                    tokenResponse.getRefreshToken(),
                    tokenResponse.getExpiresIn()
            );
        } else {
            userToken = new UserToken(
                    userId,
                    tokenResponse.getAccessToken(),
                    tokenResponse.getRefreshToken(),
                    tokenResponse.getExpiresIn()
            );
        }

        return tokenRepository.save(userToken);
    }
}