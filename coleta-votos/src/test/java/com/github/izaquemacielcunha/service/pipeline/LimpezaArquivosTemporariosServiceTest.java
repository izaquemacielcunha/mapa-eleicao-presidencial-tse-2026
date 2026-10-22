package com.github.izaquemacielcunha.service.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.izaquemacielcunha.config.EleicaoConfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimpezaArquivosTemporariosServiceTest {

    @Test
    @DisplayName("Deve excluir recursivamente todos os arquivos temporários e o próprio diretório temporário raiz")
    void deveExcluirArquivosTemporariosERaiz(@TempDir Path tempBaseDir) throws IOException {
        Path dirTemp = tempBaseDir.resolve("temp-files");
        Path dirBoletins = dirTemp.resolve("boletins-urnas");
        Files.createDirectories(dirBoletins);

        Path tempCsv = dirTemp.resolve("temp.csv");
        Path enriquecidoCsv = dirTemp.resolve("arquivo-enriquecido.csv");
        Path boletimUrnaCsv = dirTemp.resolve("arquivo-boletim-urna.csv");
        Path mescladoCsv = dirTemp.resolve("resultado-mesclado.csv");
        Path bu1 = dirBoletins.resolve("bu1.dat");
        Path bu2 = dirBoletins.resolve("bu2.dat");

        Files.writeString(tempCsv, "linha-temp");
        Files.writeString(enriquecidoCsv, "linha-enriquecida");
        Files.writeString(boletimUrnaCsv, "linha-bu");
        Files.writeString(mescladoCsv, "linha-mesclada");
        Files.writeString(bu1, "dat1");
        Files.writeString(bu2, "dat2");

        Path arquivoCoords = tempBaseDir.resolve("dados").resolve("coords.csv");
        Files.createDirectories(arquivoCoords.getParent());
        Files.writeString(arquivoCoords, "coords");

        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                dirTemp,
                dirBoletins,
                arquivoCoords,
                true
        );

        LimpezaArquivosTemporariosService service = new LimpezaArquivosTemporariosServiceImpl(config);
        service.limparArquivosTemporarios();

        assertFalse(Files.exists(tempCsv), "temp.csv deve ter sido excluído");
        assertFalse(Files.exists(enriquecidoCsv), "arquivo-enriquecido.csv deve ter sido excluído");
        assertFalse(Files.exists(boletimUrnaCsv), "arquivo-boletim-urna.csv deve ter sido excluído");
        assertFalse(Files.exists(mescladoCsv), "resultado-mesclado.csv deve ter sido excluído");
        assertFalse(Files.exists(bu1), "bu1.dat deve ter sido excluído");
        assertFalse(Files.exists(bu2), "bu2.dat deve ter sido excluído");
        assertFalse(Files.exists(dirBoletins), "diretorio de boletins deve ter sido excluído");
        assertFalse(Files.exists(dirTemp), "diretorio temporário raiz deve ter sido excluído");

        // Arquivo de entrada externo deve ser preservado
        assertTrue(Files.exists(arquivoCoords), "Arquivo de coordenadas externo deve ser preservado");
    }

    @Test
    @DisplayName("Deve ser idempotente quando o diretório temporário não existir")
    void deveSerIdempotenteQuandoDiretorioNaoExiste(@TempDir Path tempBaseDir) {
        Path dirTempInexistente = tempBaseDir.resolve("temp-inexistente");
        Path arquivoCoords = tempBaseDir.resolve("coords.csv");

        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                dirTempInexistente,
                dirTempInexistente.resolve("boletins"),
                arquivoCoords,
                true
        );

        LimpezaArquivosTemporariosService service = new LimpezaArquivosTemporariosServiceImpl(config);
        assertDoesNotThrow(service::limparArquivosTemporarios);
    }

    @Test
    @DisplayName("Não deve excluir arquivos se a flag limparArquivosTemporarios estiver desativada")
    void naoDeveExcluirQuandoFlagDesativada(@TempDir Path tempBaseDir) throws IOException {
        Path dirTemp = tempBaseDir.resolve("temp-files");
        Files.createDirectories(dirTemp);
        Path tempCsv = dirTemp.resolve("temp.csv");
        Files.writeString(tempCsv, "linha-temp");

        Path arquivoCoords = tempBaseDir.resolve("coords.csv");
        Files.writeString(arquivoCoords, "coords");

        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                dirTemp,
                dirTemp.resolve("boletins"),
                arquivoCoords,
                false
        );

        LimpezaArquivosTemporariosService service = new LimpezaArquivosTemporariosServiceImpl(config);
        service.limparArquivosTemporarios();

        assertTrue(Files.exists(tempCsv), "temp.csv deve ser mantido quando a flag for false");
        assertTrue(Files.exists(dirTemp), "Diretório temporário deve ser mantido quando a flag for false");
    }

    @Test
    @DisplayName("Deve acionar guardrails e lançar IllegalArgumentException se o diretório configurado for inseguro")
    void deveLancarExcecaoQuandoDiretorioInseguro(@TempDir Path tempBaseDir) throws IOException {
        Path diretorioTrabalho = Path.of("").toAbsolutePath();
        Path arquivoCoords = tempBaseDir.resolve("coords.csv");
        Files.writeString(arquivoCoords, "coords");

        EleicaoConfig configDiretorioTrabalho = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                diretorioTrabalho,
                diretorioTrabalho.resolve("boletins"),
                arquivoCoords,
                true
        );

        LimpezaArquivosTemporariosService serviceTrabalho = new LimpezaArquivosTemporariosServiceImpl(configDiretorioTrabalho);
        assertThrows(IllegalArgumentException.class, serviceTrabalho::limparArquivosTemporarios);

        Path dirComCoords = tempBaseDir.resolve("pasta-com-coords");
        Files.createDirectories(dirComCoords);
        Path coordsDentro = dirComCoords.resolve("coords.csv");
        Files.writeString(coordsDentro, "coords");

        EleicaoConfig configComCoordsDentro = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                dirComCoords,
                dirComCoords.resolve("boletins"),
                coordsDentro,
                true
        );

        LimpezaArquivosTemporariosService serviceCoords = new LimpezaArquivosTemporariosServiceImpl(configComCoordsDentro);
        assertThrows(IllegalArgumentException.class, serviceCoords::limparArquivosTemporarios);
    }
}
