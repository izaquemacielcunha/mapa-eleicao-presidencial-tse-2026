package com.github.izaquemacielcunha.service;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.model.Abrangencia;
import com.github.izaquemacielcunha.model.Municipio;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;
import com.github.izaquemacielcunha.service.csv.CsvBoletimUrnaService;
import com.github.izaquemacielcunha.service.csv.CsvCartoExportadorService;
import com.github.izaquemacielcunha.service.csv.CsvEnriquecedorService;
import com.github.izaquemacielcunha.service.csv.CsvMescladorBoletimService;
import com.github.izaquemacielcunha.service.csv.CsvService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaDownloadService;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaService;

@Singleton
public class EleicaoPipelineService {

    private final EleicaoConfig config;
    private final ExternalService externalService;
    private final TseUrlBuilder tseUrlBuilder;
    private final CsvService csvService;
    private final CsvEnriquecedorService csvEnriquecedorService;
    private final BoletimUrnaDownloadService boletimUrnaDownloader;
    private final BoletimUrnaService boletimUrnaService;
    private final CsvBoletimUrnaService csvBoletimUrnaService;
    private final CsvMescladorBoletimService csvMescladorBoletimService;
    private final CsvCartoExportadorService csvCartoExportadorService;

    @Inject
    public EleicaoPipelineService(
            EleicaoConfig config,
            ExternalService externalService,
            TseUrlBuilder tseUrlBuilder,
            CsvService csvService,
            CsvEnriquecedorService csvEnriquecedorService,
            BoletimUrnaDownloadService boletimUrnaDownloader,
            BoletimUrnaService boletimUrnaService,
            CsvBoletimUrnaService csvBoletimUrnaService,
            CsvMescladorBoletimService csvMescladorBoletimService,
            CsvCartoExportadorService csvCartoExportadorService
    ) {
        this.config = config;
        this.externalService = externalService;
        this.tseUrlBuilder = tseUrlBuilder;
        this.csvService = csvService;
        this.csvEnriquecedorService = csvEnriquecedorService;
        this.boletimUrnaDownloader = boletimUrnaDownloader;
        this.boletimUrnaService = boletimUrnaService;
        this.csvBoletimUrnaService = csvBoletimUrnaService;
        this.csvMescladorBoletimService = csvMescladorBoletimService;
        this.csvCartoExportadorService = csvCartoExportadorService;
    }

    public void executar() throws Exception {
        executarFase1MunicipioConfig();
        executarFase2Enriquecimento();
        executarFase3DownloadBoletins();
        executarFase4ApuracaoVotos();
        executarFase5MergeVotos();
        executarFase6ExportacaoCarto();
        System.out.println("Arquivo gerado e pronto para o Carto.com!");
    }

    private void executarFase1MunicipioConfig() throws Exception {
        String urlConfig = tseUrlBuilder.buildUrlMunicipioConfig();
        Optional<Municipio> resultado = externalService.callMunicipioConfig(urlConfig, config.codigoMunicipio());
        if (resultado.isEmpty()) {
            throw new IllegalStateException("Município não encontrado para o código: " + config.codigoMunicipio());
        }
        Abrangencia abrangencia = new Abrangencia(config.uf(), resultado.get());
        csvService.gerarCsv(List.of(abrangencia));
    }

    private void executarFase2Enriquecimento() throws Exception {
        csvEnriquecedorService.enriquecer();
    }

    private void executarFase3DownloadBoletins() throws Exception {
        boletimUrnaDownloader.download();
    }

    private void executarFase4ApuracaoVotos() {
        List<ResumoVotacao> votacao = boletimUrnaService.processarDiretorio(config.diretorioBoletins().toString());
        csvBoletimUrnaService.process(votacao);
    }

    private void executarFase5MergeVotos() throws Exception {
        csvMescladorBoletimService.mesclar();
    }

    private void executarFase6ExportacaoCarto() throws Exception {
        csvCartoExportadorService.exportar(
                config.arquivoCoordenadasTse(),
                config.arquivoResultadoMescladoCsv(),
                config.arquivoCartoFinal(),
                config.codigoMunicipio()
        );
    }
}
