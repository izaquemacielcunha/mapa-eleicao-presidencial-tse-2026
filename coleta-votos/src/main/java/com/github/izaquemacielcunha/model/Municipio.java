package com.github.izaquemacielcunha.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Municipio(
        @JsonProperty("cd")
        String codigo,
        @JsonProperty("nm")
        String nome,
        @JsonProperty("zon")
        List<Zona> zonas
) { }
