package com.github.izaquemacielcunha.dependencyinjection;

import java.net.http.HttpClient;

import jakarta.inject.Singleton;

import dagger.Component;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.csv.CsvMapper;

import io.github.resilience4j.ratelimiter.RateLimiter;

@Singleton
@Component(modules = {NetworkModule.class, MapperModule.class})
public interface ApplicationComponent {

    HttpClient httpClient();
    RateLimiter rateLimiter();
    ObjectMapper objectMapper();
    CsvMapper csvMapper();

}// end of interface