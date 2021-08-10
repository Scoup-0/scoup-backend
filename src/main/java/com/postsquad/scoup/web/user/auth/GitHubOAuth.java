package com.postsquad.scoup.web.user.auth;

import com.postsquad.scoup.web.user.auth.exception.AccessTokenNotFoundException;
import com.postsquad.scoup.web.user.auth.exception.GitHubUserNotFoundException;
import com.postsquad.scoup.web.user.auth.exception.InvalidGitHubRequestException;
import com.postsquad.scoup.web.user.auth.request.AccessTokenRequest;
import com.postsquad.scoup.web.user.auth.response.AccessTokenResponse;
import com.postsquad.scoup.web.user.auth.response.OAuthUserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GitHubOAuth implements OAuth {

    private final Logger logger = LoggerFactory.getLogger(GitHubOAuth.class);
    private static final String TOKEN = "token";
    private final WebClient webClient;
    private final String accessTokenUri;
    private final String userUri;
    private final String clientId;
    private final String clientSecret;

    public GitHubOAuth(WebClient webClient,
                       @Value("${github.access.token.uri}") String accessTokenUri,
                       @Value("${github.user.uri}") String userUri,
                       @Value("${github.web.client.id}") String clientId,
                       @Value("${github.web.client.secret}") String clientSecret) {
        this.webClient = webClient;
        this.accessTokenUri = accessTokenUri;
        this.userUri = userUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public AccessTokenResponse getToken(String code) {
        logger.debug("Authorization code: {}", code);
        AccessTokenRequest accessTokenRequest = AccessTokenRequest.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .code(code)
                .build();

        return webClient.post()
                .uri(accessTokenUri)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(accessTokenRequest)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, error -> Mono.error(InvalidGitHubRequestException::new))
                .bodyToMono(AccessTokenResponse.class)
                .blockOptional()
                .orElseThrow(AccessTokenNotFoundException::new);
    }

    @Override
    public OAuthUserResponse getUserInfo(String accessToken) {
        logger.debug("Access token: {}", accessToken);
        return webClient.get()
                .uri(userUri)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, TOKEN + " " + accessToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, error -> Mono.error(InvalidGitHubRequestException::new))
                .bodyToMono(OAuthUserResponse.class)
                .blockOptional()
                .orElseThrow(GitHubUserNotFoundException::new);
    }
}
