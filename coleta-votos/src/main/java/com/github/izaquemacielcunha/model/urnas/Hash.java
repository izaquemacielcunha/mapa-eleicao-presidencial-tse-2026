package com.github.izaquemacielcunha.model.urnas;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Hash(
        String hash,
        @JsonProperty("arq")
        List<Arquivo> arquivos
) { }// end of class