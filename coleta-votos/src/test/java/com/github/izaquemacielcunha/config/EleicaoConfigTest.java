package com.github.izaquemacielcunha.config;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EleicaoConfigTest {

    @Test
    @DisplayName("Deve inicializar configuração padrão corretamente")
    void deveInicializarConfiguracaoPadrao() {
        EleicaoConfig config = EleicaoConfig.padrao();

        assertEquals("PR", config.uf());
        assertEquals("75353", config.codigoMunicipio());
        assertEquals("3220", config.codigoPleito());
        assertEquals("ele2026", config.cicloEleitoral());
        assertEquals(Path.of("temp-files"), config.diretorioTemp());
        assertEquals(Path.of("temp-files/boletins-urnas"), config.diretorioBoletins());

        assertEquals(Path.of("temp-files/temp.csv"), config.arquivoTempCsv());
        assertEquals(Path.of("temp-files/arquivo-enriquecido.csv"), config.arquivoEnriquecidoCsv());
        assertEquals(Path.of("temp-files/arquivo-boletim-urna.csv"), config.arquivoBoletimUrnaCsv());
        assertEquals(Path.of("temp-files/resultado-mesclado.csv"), config.arquivoResultadoMescladoCsv());
        assertEquals(Path.of("dados/eleitorado_local_votacao_2026_PR.csv"), config.arquivoCoordenadasTse());
        assertEquals(Path.of("carto-dataset-final-eleic-presi-2026-1-turn-pr.csv"), config.arquivoCartoFinal());
    }

    @Test
    @DisplayName("Deve extrair UF, Município e caminho do arquivo de coordenadas a partir dos argumentos da CLI")
    void deveExtrairParametrosCli() {
        EleicaoConfig config = EleicaoConfig.fromArgs(new String[]{"sc", "80000", "meus-dados/coords_sc.csv"});

        assertEquals("SC", config.uf());
        assertEquals("80000", config.codigoMunicipio());
        assertEquals(Path.of("meus-dados/coords_sc.csv"), config.arquivoCoordenadasTse());
        assertEquals(Path.of("carto-dataset-final-eleic-presi-2026-1-turn-sc.csv"), config.arquivoCartoFinal());
    }

    @Test
    @DisplayName("Deve lançar exceção se argumentos insuficientes forem passados na CLI")
    void deveLancarExcecaoQuandoArgumentosInsuficientes() {
        assertThrows(IllegalArgumentException.class, () -> EleicaoConfig.fromArgs(null));
        assertThrows(IllegalArgumentException.class, () -> EleicaoConfig.fromArgs(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> EleicaoConfig.fromArgs(new String[]{"PR"}));
        assertThrows(IllegalArgumentException.class, () -> EleicaoConfig.fromArgs(new String[]{"PR", "75353"}));
    }

    @Test
    @DisplayName("Compact constructor deve validar parâmetros nulos e em branco")
    void deveValidarCamposNoConstrutorCompacto() {
        assertThrows(NullPointerException.class, () -> new EleicaoConfig(
                null, "75353", "3220", "ele2026", Path.of("temp"), Path.of("bu"), Path.of("coords.csv")));
        assertThrows(IllegalArgumentException.class, () -> new EleicaoConfig(
                "  ", "75353", "3220", "ele2026", Path.of("temp"), Path.of("bu"), Path.of("coords.csv")));
        assertThrows(IllegalArgumentException.class, () -> new EleicaoConfig(
                "PR", "   ", "3220", "ele2026", Path.of("temp"), Path.of("bu"), Path.of("coords.csv")));
    }
}
