package com.github.izaquemacielcunha.service.csv;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipio;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;
import com.github.izaquemacielcunha.model.urna.Arquivo;
import com.github.izaquemacielcunha.model.urna.Hash;
import com.github.izaquemacielcunha.model.urna.Metadados;
import com.github.izaquemacielcunha.model.urna.StatusSecaoEleitoral;
import com.github.izaquemacielcunha.service.ExternalService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CsvEnriquecedorServiceTest {

    @Test
    @DisplayName("Deve gerar CSV enriquecido para seções ativas (bu/busa) e inoperantes (Optional.empty)")
    void deveGerarCsvEnriquecidoComSucesso(@TempDir Path tempDir) throws Exception {
        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                tempDir,
                tempDir.resolve("boletins")
        );

        CsvMapper mapper = new CsvMapper();
        ExternalService externalService = mock(ExternalService.class);
        TseUrlBuilder urlBuilder = new TseUrlBuilder(config);
        CsvEnriquecedorServiceImpl service = new CsvEnriquecedorServiceImpl(externalService, mapper, urlBuilder, config);

        LinhaMunicipio linha1 = new LinhaMunicipio("PR", "75353", "CURITIBA", "1", "10");
        LinhaMunicipio linha2 = new LinhaMunicipio("PR", "75353", "CURITIBA", "1", "20");
        LinhaMunicipio linha3 = new LinhaMunicipio("PR", "75353", "CURITIBA", "1", "30");

        // Cria temp.csv com 3 seções: 10 (bu), 20 (busa), 30 (inoperante)
        List<LinhaMunicipio> linhasIniciais = List.of(linha1, linha2, linha3);
        CsvSchema schemaInicial = mapper.schemaFor(LinhaMunicipio.class).withHeader();
        mapper.writer(schemaInicial).writeValue(config.arquivoTempCsv().toFile(), linhasIniciais);

        // Mock das respostas: Arquivo(nome, tipo)
        Metadados metaBu = new Metadados(List.of(
                new Hash("hash-bu-10", List.of(new Arquivo("o00407-10.bu", "bu")))
        ));
        Metadados metaBusa = new Metadados(List.of(
                new Hash("hash-busa-20", List.of(new Arquivo("o00407-20.busa", "busa")))
        ));

        when(externalService.buscarMetadadosSecao(urlBuilder.buildUrlSecaoMetadados(linha1)))
                .thenReturn(Optional.of(metaBu));
        when(externalService.buscarMetadadosSecao(urlBuilder.buildUrlSecaoMetadados(linha2)))
                .thenReturn(Optional.of(metaBusa));
        when(externalService.buscarMetadadosSecao(urlBuilder.buildUrlSecaoMetadados(linha3)))
                .thenReturn(Optional.empty());

        service.enriquecerMetadadosSecoes();

        assertTrue(Files.exists(config.arquivoEnriquecidoCsv()));

        // Lê arquivo enriquecido e valida campos
        CsvSchema schemaEnriquecido = mapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        List<LinhaMunicipioEnriquecida> linhasEnriquecidas;
        try (MappingIterator<LinhaMunicipioEnriquecida> it = mapper.readerFor(LinhaMunicipioEnriquecida.class)
                .with(schemaEnriquecido)
                .readValues(config.arquivoEnriquecidoCsv().toFile())) {
            linhasEnriquecidas = it.readAll();
        }

        assertEquals(3, linhasEnriquecidas.size());

        LinhaMunicipioEnriquecida l1 = linhasEnriquecidas.stream()
                .filter(l -> "10".equals(l.numeroSecao())).findFirst().orElseThrow();
        assertEquals("hash-bu-10", l1.hash());
        assertEquals("o00407-10.bu", l1.boletimUrna());

        LinhaMunicipioEnriquecida l2 = linhasEnriquecidas.stream()
                .filter(l -> "20".equals(l.numeroSecao())).findFirst().orElseThrow();
        assertEquals("hash-busa-20", l2.hash());
        assertEquals("o00407-20.busa", l2.boletimUrna());

        LinhaMunicipioEnriquecida l3 = linhasEnriquecidas.stream()
                .filter(l -> "30".equals(l.numeroSecao())).findFirst().orElseThrow();
        assertEquals(StatusSecaoEleitoral.STATUS_INOPERANTE_AGREGADA, l3.hash());
        assertEquals(StatusSecaoEleitoral.STATUS_INOPERANTE_AGREGADA, l3.boletimUrna());
    }
}
