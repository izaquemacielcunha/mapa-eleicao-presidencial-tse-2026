package com.github.izaquemacielcunha.dependencyinjection;

import dagger.Component;
import io.github.resilience4j.ratelimiter.RateLimiter;
import jakarta.inject.Singleton;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

@Singleton
@Component(modules = {NetworkModule.class})
public interface ApplicationComponent {

    HttpClient httpClient();
    RateLimiter rateLimiter();
    ObjectMapper objectMapper();

}// end of interface