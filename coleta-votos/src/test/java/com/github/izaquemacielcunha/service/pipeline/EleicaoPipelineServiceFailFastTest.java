package com.github.izaquemacielcunha.service.pipeline;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.service.pipeline.checkpoint.PipelineCheckpointService;
import com.github.izaquemacielcunha.service.csv.CsvBoletimUrnaService;
import com.github.izaquemacielcunha.service.csv.CsvCartoExportadorService;
import com.github.izaquemacielcunha.service.csv.CsvEnriquecedorService;
import com.github.izaquemacielcunha.service.csv.CsvMescladorBoletimService;
import com.github.izaquemacielcunha.service.csv.CsvSecaoService;
import com.github.izaquemacielcunha.service.tse.ExternalService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaDownloadService;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EleicaoPipelineServiceFailFastTest {

    @Test
    @DisplayName("executar deve lançar IllegalStateException quando arquivo de coordenadas do TSE não existe")
    void executarDeveFalharQuandoArquivoCoordenadasNaoExiste() {
        EleicaoConfig configInexistente = new EleicaoConfig(
                "ZZ",
                "99999",
                "3220",
                "ele2026",
                Path.of("temp-files-inexistente"),
                Path.of("temp-files-inexistente/boletins"),
                Path.of("dados-inexistentes/coords_zz.csv")
        );

        LimpezaArquivosTemporariosService limpezaService = mock(LimpezaArquivosTemporariosService.class);
        PipelineCheckpointService checkpointService = mock(PipelineCheckpointService.class);

        EleicaoPipelineService service = new EleicaoPipelineServiceImpl(
                configInexistente,
                mock(ExternalService.class),
                mock(TseUrlBuilder.class),
                mock(CsvSecaoService.class),
                mock(CsvEnriquecedorService.class),
                mock(BoletimUrnaDownloadService.class),
                mock(BoletimUrnaService.class),
                mock(CsvBoletimUrnaService.class),
                mock(CsvMescladorBoletimService.class),
                mock(CsvCartoExportadorService.class),
                limpezaService,
                checkpointService
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::executar);
        assertTrue(exception.getMessage().contains("Arquivo de coordenadas do TSE não encontrado"));
        verify(limpezaService, never()).limparArquivosTemporarios();
    }
}
