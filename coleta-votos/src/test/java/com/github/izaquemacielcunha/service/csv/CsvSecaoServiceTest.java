package com.github.izaquemacielcunha.service.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.model.Abrangencia;
import com.github.izaquemacielcunha.model.Municipio;
import com.github.izaquemacielcunha.model.Secao;
import com.github.izaquemacielcunha.model.Zona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.dataformat.csv.CsvMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvSecaoServiceTest {

    @Test
    @DisplayName("Deve criar o diretório temp automaticamente e gravar o arquivo temp.csv usando exportarSecoesMunicipaisParaCsv")
    void deveCriarDiretorioTempEGravarCsv(@TempDir Path baseDir) {
        Path customTempDir = baseDir.resolve("sub/diretorio/temp");
        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                customTempDir,
                customTempDir.resolve("boletins")
        );

        CsvMapper mapper = new CsvMapper();
        CsvSecaoService csvService = new CsvSecaoServiceImpl(mapper, config);

        Municipio municipio = new Municipio("75353", "CURITIBA", List.of(
                new Zona("1", List.of(new Secao("10"), new Secao("20")))
        ));
        Abrangencia abrangencia = new Abrangencia("PR", municipio);

        csvService.exportarSecoesMunicipaisParaCsv(List.of(abrangencia));

        assertTrue(Files.exists(customTempDir), "O diretório temp deve ser criado automaticamente");
        assertTrue(Files.exists(config.arquivoTempCsv()), "O arquivo temp.csv deve existir");
    }
}
