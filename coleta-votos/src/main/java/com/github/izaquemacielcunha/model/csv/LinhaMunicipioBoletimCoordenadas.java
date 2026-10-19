package com.github.izaquemacielcunha.model.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "SG_UF", "CD_MUNICIPIO", "NM_MUNICIPIO", "NR_ZONA", "NR_SECAO",
        "NM_BAIRRO", "NM_LOCAL_VOTACAO", "latitude", "longitude",
        "CANDIDATO", "QTD_VOTOS", "BOLETIM_URNA"
})
public record LinhaMunicipioBoletimCoordenadas(
        @JsonProperty("SG_UF") String unidadeFederativa,
        @JsonProperty("CD_MUNICIPIO") String codigoMunicipio,
        @JsonProperty("NM_MUNICIPIO") String nomeMunicipio,
        @JsonProperty("NR_ZONA") String numeroZona,
        @JsonProperty("NR_SECAO") String numeroSecao,
        @JsonProperty("NM_BAIRRO") String nomeBairro,
        @JsonProperty("NM_LOCAL_VOTACAO") String localVotacao,
        @JsonProperty("latitude") String latitude,
        @JsonProperty("longitude") String longitude,
        @JsonProperty("CANDIDATO") String candidato,
        @JsonProperty("QTD_VOTOS") int votos,
        @JsonProperty("BOLETIM_URNA") String boletimUrna

) { }// end of class
