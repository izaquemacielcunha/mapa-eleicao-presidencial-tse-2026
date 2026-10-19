package com.github.izaquemacielcunha.model.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"SG_UF", "CD_MUNICIPIO", "NM_MUNICIPIO", "NR_ZONA", "NR_SECAO", "CANDIDATO", "QTD_VOTOS", "HASH", "BOLETIM_URNA"})
public record LinhaMescladaMunicipioBoletim(
        @JsonProperty("SG_UF") String unidadeFederativa,
        @JsonProperty("CD_MUNICIPIO") String codigoMunicipio,
        @JsonProperty("NM_MUNICIPIO") String nomeMunicipio,
        @JsonProperty("NR_ZONA") String numeroZona,
        @JsonProperty("NR_SECAO") String numeroSecao,
        @JsonProperty("CANDIDATO") String candidato,
        @JsonProperty("QTD_VOTOS") int votos,
        @JsonProperty("HASH") String hash,
        @JsonProperty("BOLETIM_URNA") String boletimUrna
) {
    public String gerarChaveComposta() {
        int zona = (numeroZona != null && !numeroZona.isBlank()) ? Integer.parseInt(numeroZona.trim()) : 0;
        int secao = (numeroSecao != null && !numeroSecao.isBlank()) ? Integer.parseInt(numeroSecao.trim()) : 0;
        return unidadeFederativa + "-" + codigoMunicipio + "-" + zona + "-" + secao;
    }
}
