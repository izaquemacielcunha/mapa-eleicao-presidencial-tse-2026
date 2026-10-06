package com.github.izaquemacielcunha.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Zona(
        @JsonProperty("cd")
        String codigo,
        @JsonProperty("sec")
        List<Secao> secoes
) { }