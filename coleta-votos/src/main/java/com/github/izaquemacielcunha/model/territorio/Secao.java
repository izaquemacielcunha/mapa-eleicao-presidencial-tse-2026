package com.github.izaquemacielcunha.model.territorio;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Secao(
        @JsonProperty("ns") String numero
) { }
