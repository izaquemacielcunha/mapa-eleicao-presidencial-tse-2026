package com.github.izaquemacielcunha.service.tse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipio;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TseUrlBuilderTest {

    private final EleicaoConfig config = EleicaoConfig.padrao();
    private final TseUrlBuilder urlBuilder = new TseUrlBuilder(config);

    @Test
    @DisplayName("Deve construir URL de configuração de município corretamente")
    void deveConstruirUrlMunicipioConfig() {
        String url = urlBuilder.buildUrlMunicipioConfig();

        assertEquals(
                "https://resultados.tse.jus.br/oficial/ele2026/arquivo-urna/3220/config/pr/pr-p003220-cs.json",
                url
        );
    }

    @Test
    @DisplayName("Deve construir URL de metadados da seção (-aux.json) corretamente")
    void deveConstruirUrlSecaoMetadados() {
        LinhaMunicipio linha = new LinhaMunicipio("PR", "75353", "CURITIBA", "0001", "0010");

        String url = urlBuilder.buildUrlSecaoMetadados(linha);

        assertEquals(
                "https://resultados.tse.jus.br/oficial/ele2026/arquivo-urna/3220/dados/pr/75353/0001/0010/p003220-pr-m75353-z0001-s0010-aux.json",
                url
        );
    }

    @Test
    @DisplayName("Deve construir URL de download do Boletim de Urna (.dat) corretamente")
    void deveConstruirUrlBoletimUrna() {
        LinhaMunicipioEnriquecida linha = new LinhaMunicipioEnriquecida(
                "PR", "75353", "CURITIBA", "0001", "0010", "hash123", "o03220-bu.dat"
        );

        String url = urlBuilder.buildUrlBoletimUrna(linha);

        assertEquals(
                "https://resultados.tse.jus.br/oficial/ele2026/arquivo-urna/3220/dados/pr/75353/0001/0010/hash123/o03220-bu.dat",
                url
        );
    }
}
