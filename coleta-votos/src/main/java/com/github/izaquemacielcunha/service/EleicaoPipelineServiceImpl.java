package com.github.izaquemacielcunha.service;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.exception.EleicaoDataProcessingException;
import com.github.izaquemacielcunha.model.Abrangencia;
import com.github.izaquemacielcunha.model.Municipio;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;
import com.github.izaquemacielcunha.service.csv.CsvBoletimUrnaService;
import com.github.izaquemacielcunha.service.csv.CsvCartoExportadorService;
import com.github.izaquemacielcunha.service.csv.CsvEnriquecedorService;
import com.github.izaquemacielcunha.service.csv.CsvMescladorBoletimService;
import com.github.izaquemacielcunha.service.csv.CsvSecaoService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaDownloadService;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaService;

@Singleton
public class EleicaoPipelineServiceImpl implements EleicaoPipelineService {
    private static final Logger logger = LoggerFactory.getLogger(EleicaoPipelineServiceImpl.class);

    private final EleicaoConfig config;
    private final ExternalService externalService;
    private final TseUrlBuilder tseUrlBuilder;
    private final CsvSecaoService csvSecaoService;
    private final CsvEnriquecedorService csvEnriquecedorService;
    private final BoletimUrnaDownloadService boletimUrnaDownloader;
    private final BoletimUrnaService boletimUrnaService;
    private final CsvBoletimUrnaService csvBoletimUrnaService;
    private final CsvMescladorBoletimService csvMescladorBoletimService;
    private final CsvCartoExportadorService csvCartoExportadorService;
    private final LimpezaArquivosTemporariosService limpezaArquivosTemporariosService;

    @Inject
    public EleicaoPipelineServiceImpl(
            EleicaoConfig config,
            ExternalService externalService,
            TseUrlBuilder tseUrlBuilder,
            CsvSecaoService csvSecaoService,
            CsvEnriquecedorService csvEnriquecedorService,
            BoletimUrnaDownloadService boletimUrnaDownloader,
            BoletimUrnaService boletimUrnaService,
            CsvBoletimUrnaService csvBoletimUrnaService,
            CsvMescladorBoletimService csvMescladorBoletimService,
            CsvCartoExportadorService csvCartoExportadorService,
            LimpezaArquivosTemporariosService limpezaArquivosTemporariosService
    ) {
        this.config = Objects.requireNonNull(config, "EleicaoConfig não pode ser nulo");
        this.externalService = Objects.requireNonNull(externalService, "ExternalService não pode ser nulo");
        this.tseUrlBuilder = Objects.requireNonNull(tseUrlBuilder, "TseUrlBuilder não pode ser nulo");
        this.csvSecaoService = Objects.requireNonNull(csvSecaoService, "CsvSecaoService não pode ser nulo");
        this.csvEnriquecedorService = Objects.requireNonNull(csvEnriquecedorService, "CsvEnriquecedorService não pode ser nulo");
        this.boletimUrnaDownloader = Objects.requireNonNull(boletimUrnaDownloader, "BoletimUrnaDownloadService não pode ser nulo");
        this.boletimUrnaService = Objects.requireNonNull(boletimUrnaService, "BoletimUrnaService não pode ser nulo");
        this.csvBoletimUrnaService = Objects.requireNonNull(csvBoletimUrnaService, "CsvBoletimUrnaService não pode ser nulo");
        this.csvMescladorBoletimService = Objects.requireNonNull(csvMescladorBoletimService, "CsvMescladorBoletimService não pode ser nulo");
        this.csvCartoExportadorService = Objects.requireNonNull(csvCartoExportadorService, "CsvCartoExportadorService não pode ser nulo");
        this.limpezaArquivosTemporariosService = Objects.requireNonNull(limpezaArquivosTemporariosService, "LimpezaArquivosTemporariosService não pode ser nulo");
    }

    @Override
    public void executar() {
        Instant inicioGeral = Instant.now();
        logger.info("Iniciando pipeline de coleta de votos do TSE para {}/{}...", config.uf(), config.codigoMunicipio());

        validarPreCondicoes();
        gerarEstruturaSecoesMunicipais();
        enriquecerMetadadosSecoes();
        baixarBoletinsUrna();
        apurarVotosBoletins();
        mesclarVotosComSecoes();
        exportarDatasetGeorreferenciadoParaCarto();
        limparArquivosTemporarios();

        Duration tempoTotal = Duration.between(inicioGeral, Instant.now());
        logger.info(
                "Pipeline eleitoral concluído com sucesso em {} ms! Arquivo pronto para o Carto.com em: {}",
                tempoTotal.toMillis(), config.arquivoCartoFinal().toAbsolutePath());
    }

    private void validarPreCondicoes() {
        Objects.requireNonNull(config, "Configuração eleitoral não pode ser nula");
        if (!Files.exists(config.arquivoCoordenadasTse())) {
            throw new IllegalStateException("Arquivo de coordenadas do TSE não encontrado: "
                    + config.arquivoCoordenadasTse().toAbsolutePath()
                    + ". Certifique-se de que o arquivo está presente no diretório configurado antes de executar o pipeline.");
        }
    }

    private void gerarEstruturaSecoesMunicipais() {
        Instant inicio = Instant.now();
        logger.info("[Fase 1/7 - Estrutura Seções] Iniciando obtenção de configuração do município {}/{}...", config.uf(), config.codigoMunicipio());

        try {
            if (!Files.exists(config.diretorioTemp())) {
                Files.createDirectories(config.diretorioTemp());
            }
        } catch (IOException e) {
            throw new EleicaoDataProcessingException("Erro ao criar diretório temporário: " + config.diretorioTemp(), e);
        }

        String urlConfig = tseUrlBuilder.buildUrlMunicipioConfig();
        Optional<Municipio> resultado = externalService.buscarConfiguracaoMunicipio(urlConfig, config.codigoMunicipio());
        if (resultado.isEmpty()) {
            throw new IllegalStateException("Município não encontrado para o código: " + config.codigoMunicipio());
        }

        Municipio municipio = resultado.get();
        int totalSecoes = (municipio.zonas() != null)
                ? municipio.zonas().stream().mapToInt(z -> z.secoes() != null ? z.secoes().size() : 0).sum()
                : 0;

        Abrangencia abrangencia = new Abrangencia(config.uf(), municipio);
        csvSecaoService.exportarSecoesMunicipaisParaCsv(List.of(abrangencia));

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "[Fase 1/7 - Estrutura Seções] Configuração municipal concluída em {} ms. Total de seções mapeadas: {}.",
                decorrido.toMillis(), totalSecoes);
    }

    private void enriquecerMetadadosSecoes() {
        Instant inicio = Instant.now();
        logger.info("[Fase 2/7 - Enriquecimento Metadados] Iniciando enriquecimento de seções eleitorais...");

        csvEnriquecedorService.enriquecerMetadadosSecoes();

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info("[Fase 2/7 - Enriquecimento Metadados] Enriquecimento concluído em {} ms.", decorrido.toMillis());
    }

    private void baixarBoletinsUrna() {
        Instant inicio = Instant.now();
        logger.info("[Fase 3/7 - Download Boletins] Iniciando download dos Boletins de Urna (BUs)...");

        boletimUrnaDownloader.baixarBoletinsUrna();

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info("[Fase 3/7 - Download Boletins] Download dos boletins concluído em {} ms.", decorrido.toMillis());
    }

    private void apurarVotosBoletins() {
        Instant inicio = Instant.now();
        logger.info("[Fase 4/7 - Apuração Votos] Iniciando decodificação ASN.1 e apuração de votos para Presidente...");

        List<ResumoVotacao> votacao = boletimUrnaService.processarDiretorio(config.diretorioBoletins());
        csvBoletimUrnaService.exportarVotosParaCsv(votacao);

        int totalVotos = votacao.stream().mapToInt(ResumoVotacao::votos).sum();
        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "[Fase 4/7 - Apuração Votos] Apuração concluída em {} ms. Total de registros: {}, total de votos apurados: {}.",
                decorrido.toMillis(), votacao.size(), totalVotos);
    }

    private void mesclarVotosComSecoes() {
        Instant inicio = Instant.now();
        logger.info("[Fase 5/7 - Mesclagem Votos] Iniciando mesclagem dos votos com os dados territoriais...");

        csvMescladorBoletimService.mesclarBoletinsComMunicipios();
        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info("[Fase 5/7 - Mesclagem Votos] Mesclagem de votos concluída em {} ms.", decorrido.toMillis());
    }

    private void exportarDatasetGeorreferenciadoParaCarto() {
        Instant inicio = Instant.now();
        logger.info("[Fase 6/7 - Exportação Carto] Iniciando cruzamento com coordenadas e exportação para o Carto.com...");

        csvCartoExportadorService.exportarDatasetCarto(
                config.arquivoCoordenadasTse(),
                config.arquivoResultadoMescladoCsv(),
                config.arquivoCartoFinal(),
                config.codigoMunicipio()
        );

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "[Fase 6/7 - Exportação Carto] Exportação concluída em {} ms. Arquivo final: {}",
                decorrido.toMillis(), config.arquivoCartoFinal().toAbsolutePath());
    }

    private void limparArquivosTemporarios() {
        Instant inicio = Instant.now();
        logger.info("[Fase 7/7 - Limpeza Temporários] Iniciando limpeza de arquivos temporários...");

        limpezaArquivosTemporariosService.limparArquivosTemporarios();

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info("[Fase 7/7 - Limpeza Temporários] Limpeza concluída em {} ms.", decorrido.toMillis());
    }
}
