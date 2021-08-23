package com.postsquad.scoup.web.auth.service;

import com.postsquad.scoup.web.auth.controller.request.AccessTokenRequestWithGrantType;
import com.postsquad.scoup.web.auth.controller.response.AccessTokenResponse;
import com.postsquad.scoup.web.auth.controller.response.GoogleUserResponse;
import com.postsquad.scoup.web.auth.controller.response.SocialAuthenticationResponse;
import com.postsquad.scoup.web.auth.controller.response.SocialAuthenticationResponseMapper;
import com.postsquad.scoup.web.auth.exception.AccessTokenNotFoundException;
import com.postsquad.scoup.web.auth.exception.OAuthRequestNotValidException;
import com.postsquad.scoup.web.auth.exception.OAuthUserNotFoundException;
import com.postsquad.scoup.web.auth.exception.OAuthException;
import com.postsquad.scoup.web.auth.property.OAuthProperties;
import com.postsquad.scoup.web.auth.property.OAuthProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component("google")
public class GoogleOAuth extends OAuth {

    public GoogleOAuth(WebClient webClient, OAuthProperties oAuthProperties) {
        super(webClient, oAuthProperties);
    }

    protected AccessTokenResponse getToken(OAuthProperty oAuthProperty, String code) {
        MultiValueMap<String, String> accessTokenRequest = AccessTokenRequestWithGrantType.of(oAuthProperty, code);
        return webClient.post()
                        .uri(oAuthProperty.getAccessTokenUri())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .bodyValue(accessTokenRequest)
                        .retrieve()
                        .onStatus(HttpStatus::is4xxClientError, error -> Mono.error(() -> new OAuthException(new OAuthRequestNotValidException())))
                        .bodyToMono(AccessTokenResponse.class)
                        .blockOptional()
                        .orElseThrow(() -> new OAuthException(new AccessTokenNotFoundException()));
    }

    @Override
    protected SocialAuthenticationResponse getOAuthUserInfo(OAuthProperty oAuthProperty, String accessToken, String type) {
        GoogleUserResponse googleUserResponse = webClient.post()
                                                         .uri(oAuthProperty.getUserUri())
                                                         .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                                         .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                                         .retrieve()
                                                         .onStatus(HttpStatus::is4xxClientError, error -> Mono.error(() -> new OAuthException(new OAuthRequestNotValidException())))
                                                         .bodyToMono(GoogleUserResponse.class)
                                                         .blockOptional()
                                                         .orElseThrow(() -> new OAuthException(new OAuthUserNotFoundException()));
        return SocialAuthenticationResponseMapper.INSTANCE.GoogleUserResponseToSocialAuthenticationResponse(googleUserResponse, type);
    }
}
