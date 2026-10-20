package com.github.izaquemacielcunha.service.urna.boletim;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;
import com.github.izaquemacielcunha.model.urna.StatusSecaoEleitoral;
import com.github.izaquemacielcunha.service.ExternalService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class BoletimUrnaDownloadServiceTest {

    @Test
    @DisplayName("Deve ignorar seções inoperantes/agregadas e baixar apenas arquivos válidos")
    void deveIgnorarSecoesInoperantes(@TempDir Path tempDir) throws Exception {
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
        BoletimUrnaDownloadService service = new BoletimUrnaDownloadServiceImpl(externalService, mapper, urlBuilder, config);

        // Prepara arquivo enriquecido com uma seção ativa e uma inoperante
        List<LinhaMunicipioEnriquecida> linhas = List.of(
                new LinhaMunicipioEnriquecida("PR", "75353", "CURITIBA", "1", "10", "hash1", "o00407-7535300010010.bu"),
                new LinhaMunicipioEnriquecida("PR", "75353", "CURITIBA", "1", "20", StatusSecaoEleitoral.STATUS_INOPERANTE_AGREGADA, StatusSecaoEleitoral.STATUS_INOPERANTE_AGREGADA)
        );

        CsvSchema schema = mapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        mapper.writer(schema).writeValue(config.arquivoEnriquecidoCsv().toFile(), linhas);

        when(externalService.baixarArquivoBoletimUrnaDat(any(), any()))
                .thenReturn(Optional.of(tempDir.resolve("o00407-7535300010010.bu")));

        service.baixarBoletinsUrna();

        // Verifica que baixarArquivoBoletimUrnaDat foi chamado apenas 1 vez (para a seção ativa)
        verify(externalService, times(1)).baixarArquivoBoletimUrnaDat(contains("o00407-7535300010010.bu"), eq(config.diretorioBoletins()));
        verify(externalService, never()).baixarArquivoBoletimUrnaDat(contains(StatusSecaoEleitoral.STATUS_INOPERANTE_AGREGADA), any());
    }
}
