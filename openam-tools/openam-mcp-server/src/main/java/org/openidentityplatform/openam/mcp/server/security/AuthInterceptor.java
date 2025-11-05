package org.openidentityplatform.openam.mcp.server.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String LOGIN_PASSWORD_TOKEN_KEY = "login-password-token";

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final RestClient openAMRestClient;

    private final OpenAMConfig openAMConfig;


    private final Cache<String, String> tokenCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES).build();

    public AuthInterceptor(RestClient openAMRestClient, OpenAMConfig openAMConfig) {
        this.openAMRestClient = openAMRestClient;
        this.openAMConfig = openAMConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(openAMConfig.useOAuthForAuthentication()) {
            return preHandleOAuth(request, response);
        } else {
            return preHandleUsernamePassword(request);
        }
    }

    private long tokenValidSeconds(String tokenId) {
        String sessionUri = "/json/sessions/?_action=getSessionInfo";
        try {
            Map<String, String> tokenProps = openAMRestClient.post()
                    .uri(sessionUri)
                    .header(openAMConfig.tokenHeader(), tokenId)
                    .retrieve().body(new ParameterizedTypeReference<>() {
                    });
            String expTime = tokenProps.get("maxIdleExpirationTime");
            ZonedDateTime dateTime = ZonedDateTime.parse(expTime);
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            Duration duration = Duration.between(now, dateTime);
            return duration.getSeconds();
        } catch (Exception e) {
            log.warn("error getting token properties: ", e);
            return -1;
        }
    }

    private String getUserNamePasswordToken() {
        Map<String, String> tokenResponse = openAMRestClient.post().uri("/json/authenticate")
                .header("X-OpenAM-Username", openAMConfig.username())
                .header("X-OpenAM-Password", openAMConfig.password())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return tokenResponse.get("tokenId");
    }

    private String getTokenIdFromAccessToken(String accessToken) {
        Map<String, String> tokenResponse = openAMRestClient.post()
                .uri("/json/authenticate?authIndexType=service&authIndexValue=".concat(openAMConfig.oidcAuthChain()))
                .header(openAMConfig.oidcAuthHeader(), accessToken)
                .body("{}")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return tokenResponse.get("tokenId");
    }

    private boolean preHandleUsernamePassword(HttpServletRequest request) {
        final String token;
        try {
            token = tokenCache.get(LOGIN_PASSWORD_TOKEN_KEY, (k) -> getUserNamePasswordToken());
        } catch (Exception e) {
            log.warn("preHandleUsernamePassword: error getting token:", e);
            throw e;
        }
        long seconds = tokenValidSeconds(token);
        if(seconds > 1) {
            request.setAttribute("tokenId", token);
            return true;
        }
        log.info("preHandleUsernamePassword: token {} is about to expire in {} s", token, seconds);
        tokenCache.invalidate(LOGIN_PASSWORD_TOKEN_KEY);
        return preHandleUsernamePassword(request);
    }

    private boolean preHandleOAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String accessToken = authHeader.substring(7);

        if (!accessTokenValid(accessToken)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader("WWW-Authenticate", "Bearer realm=\"OpenAM\"");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return false;
        }
        final String token;
        try {
            token = tokenCache.get(accessToken, this::getTokenIdFromAccessToken);
        } catch (Exception e) {
            log.warn("error getting token:", e);
            throw e;
        }
        long seconds = tokenValidSeconds(token);
        if(seconds > 1) {
            request.setAttribute("tokenId", token);
            return true;
        }
        log.info("preHandleOAuth: token {} is about to expire in {} s", token, seconds);
        tokenCache.invalidate(accessToken);
        return preHandleOAuth(request, response);
    }

    private boolean accessTokenValid(String accessToken) {

        //validate access token
        try {
            Map<String, String> response = openAMRestClient.get()
                    .uri("/oauth2/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if(response.containsKey("name")) {
                return true;
            } else {
                log.warn("got invalid response: {} for access token: {}", response, accessToken);
                return false;
            }
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
