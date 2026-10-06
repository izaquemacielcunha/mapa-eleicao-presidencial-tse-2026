package com.github.izaquemacielcunha.dependencyinjection;

import java.net.http.HttpClient;
import java.time.Duration;

import jakarta.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Module
public class NetworkModule {

    @Provides
    @Singleton
    public static HttpClient provideHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    @Provides
    @Singleton
    public static RateLimiter provideRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        return RateLimiter.of("api-limiter", config);
    }

    @Provides
    @Singleton
    public static ObjectMapper provideObjectMapper() {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false)
                .build();
    }

}// end of class
