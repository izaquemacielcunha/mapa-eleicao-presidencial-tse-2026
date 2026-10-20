package com.github.izaquemacielcunha.model.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"CANDIDATO", "QTD_VOTOS", "BOLETIM_URNA"})
public record LinhaBoletimUrna(
        @JsonProperty("CANDIDATO") String candidato,
        @JsonProperty("QTD_VOTOS") int votos,
        @JsonProperty("BOLETIM_URNA") String boletimUrna
) { }