package com.github.izaquemacielcunha.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Secao(
        @JsonProperty("ns")
        String numero
) { }