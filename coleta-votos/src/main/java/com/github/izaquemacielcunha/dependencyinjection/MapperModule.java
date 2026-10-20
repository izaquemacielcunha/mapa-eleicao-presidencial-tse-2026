package com.github.izaquemacielcunha.dependencyinjection;

import dagger.Module;
import dagger.Provides;

import jakarta.inject.Singleton;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;

@Module
public class MapperModule {

    @Provides
    @Singleton
    public static ObjectMapper provideObjectMapper() {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false)
                .build();
    }

    @Provides
    @Singleton
    public static CsvMapper provideCsvMapper() {
        return CsvMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }

}
