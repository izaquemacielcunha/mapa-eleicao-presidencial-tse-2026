package com.github.izaquemacielcunha.service.csv;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.dataformat.csv.CsvMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvBoletimUrnaServiceTest {

    @Test
    @DisplayName("Deve gravar o arquivo de boletim de urna no diretório configurado pelo EleicaoConfig usando exportarVotosParaCsv")
    void deveGravarBoletimUrnaNoDiretorioConfigurado(@TempDir Path baseDir) {
        Path customTempDir = baseDir.resolve("temp-boletins");
        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                customTempDir,
                customTempDir.resolve("boletins")
        );

        CsvMapper mapper = new CsvMapper();
        CsvBoletimUrnaService service = new CsvBoletimUrnaServiceImpl(mapper, config);

        List<ResumoVotacao> votacoes = List.of(
                new ResumoVotacao("13", 100, "bu-1.dat"),
                new ResumoVotacao("22", 80, "bu-1.dat")
        );

        service.exportarVotosParaCsv(votacoes);

        assertTrue(Files.exists(config.arquivoBoletimUrnaCsv()), "O arquivo de votos deve existir no caminho de config");
    }

    @Test
    @DisplayName("Deve exportar votos com apenas um registro com sucesso")
    void deveExportarVotosRegistroUnico(@TempDir Path baseDir) {
        Path customTempDir = baseDir.resolve("temp-boletins-2");
        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                customTempDir,
                customTempDir.resolve("boletins")
        );

        CsvMapper mapper = new CsvMapper();
        CsvBoletimUrnaService service = new CsvBoletimUrnaServiceImpl(mapper, config);

        List<ResumoVotacao> votacoes = List.of(new ResumoVotacao("13", 50, "bu-2.dat"));
        service.exportarVotosParaCsv(votacoes);

        assertTrue(Files.exists(config.arquivoBoletimUrnaCsv()), "O arquivo de votos deve existir");
    }
}
