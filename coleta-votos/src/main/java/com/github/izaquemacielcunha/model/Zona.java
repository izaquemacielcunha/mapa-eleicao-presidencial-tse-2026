package com.github.izaquemacielcunha.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Zona(
        @JsonProperty("cd") String codigo,
        @JsonProperty("sec") List<Secao> secoes
) { }// end of class