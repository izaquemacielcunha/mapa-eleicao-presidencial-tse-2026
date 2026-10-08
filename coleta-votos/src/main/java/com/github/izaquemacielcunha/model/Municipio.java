package com.github.izaquemacielcunha.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Municipio(
        @JsonProperty("cd") String codigo,
        @JsonProperty("nm") String nome,
        @JsonProperty("zon") List<Zona> zonas
) { }// end of class