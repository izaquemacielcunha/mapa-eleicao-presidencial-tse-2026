package com.github.izaquemacielcunha.service.tse;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipio;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;

@Singleton
public class TseUrlBuilder {

    private static final String BASE_URL = "https://resultados.tse.jus.br/oficial";

    private final EleicaoConfig config;

    @Inject
    public TseUrlBuilder(EleicaoConfig config) {
        this.config = config;
    }

    public String buildUrlMunicipioConfig() {
        String ufLower = config.uf().toLowerCase();
        return String.format(
                "%s/%s/arquivo-urna/%s/config/%s/%s-p00%s-cs.json",
                BASE_URL,
                config.cicloEleitoral(),
                config.codigoPleito(),
                ufLower,
                ufLower,
                config.codigoPleito()
        );
    }

    public String buildUrlSecaoMetadados(LinhaMunicipio linha) {
        String ufLower = linha.unidadeFederativa().toLowerCase();
        return String.format(
                "%s/%s/arquivo-urna/%s/dados/%s/%s/%s/%s/p00%s-%s-m%s-z%s-s%s-aux.json",
                BASE_URL,
                config.cicloEleitoral(),
                config.codigoPleito(),
                ufLower,
                linha.codigoMunicipio(),
                linha.numeroZona(),
                linha.numeroSecao(),
                config.codigoPleito(),
                ufLower,
                linha.codigoMunicipio(),
                linha.numeroZona(),
                linha.numeroSecao()
        );
    }

    public String buildUrlBoletimUrna(LinhaMunicipioEnriquecida linha) {
        return String.format(
                "%s/%s/arquivo-urna/%s/dados/%s/%s/%s/%s/%s/%s",
                BASE_URL,
                config.cicloEleitoral(),
                config.codigoPleito(),
                linha.unidadeFederativa().toLowerCase(),
                linha.codigoMunicipio(),
                linha.numeroZona(),
                linha.numeroSecao(),
                linha.hash(),
                linha.boletimUrna()
        );
    }
}
