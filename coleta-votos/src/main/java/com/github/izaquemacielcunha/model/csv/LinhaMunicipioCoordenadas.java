package com.github.izaquemacielcunha.model.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"SG_UF", "CD_MUNICIPIO", "NM_MUNICIPIO", "NR_ZONA", "NR_SECAO", "NM_BAIRRO", "NR_LATITUDE", "NR_LONGITUDE", "NM_LOCAL_VOTACAO"})
public record LinhaMunicipioCoordenadas(
        @JsonProperty("SG_UF") String unidadeFederativa,
        @JsonProperty("CD_MUNICIPIO") String codigoMunicipio,
        @JsonProperty("NM_MUNICIPIO") String nomeMunicipio,
        @JsonProperty("NR_ZONA") String numeroZona,
        @JsonProperty("NR_SECAO") String numeroSecao,
        @JsonProperty("NM_BAIRRO") String nomeBairro,
        @JsonProperty("NR_LATITUDE") String latitude,
        @JsonProperty("NR_LONGITUDE") String longitude,
        @JsonProperty("NM_LOCAL_VOTACAO") String localVotacao
) implements ChaveSecaoEleitoral {
}
