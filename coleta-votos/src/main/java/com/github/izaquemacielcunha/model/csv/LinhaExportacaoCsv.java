package com.github.izaquemacielcunha.model.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"SG_UF", "CD_MUNICIPIO", "NM_MUNICIPIO", "NR_ZONA", "NR_SECAO"})
public record LinhaExportacaoCsv(
        @JsonProperty("SG_UF") String unidadeFederativa,
        @JsonProperty("CD_MUNICIPIO") String codigoMunicipio,
        @JsonProperty("NM_MUNICIPIO") String nomeMunicipio,
        @JsonProperty("NR_ZONA") String numeroZona,
        @JsonProperty("NR_SECAO") String numeroSecao
) { }// end of class