package com.github.izaquemacielcunha.model.urnas;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Arquivo(
        @JsonProperty("nm")
        String nome,
        @JsonProperty("tp")
        String tipo
) { }// end of class