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
import com.github.izaquemacielcunha.service.csv.CsvService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaDownloadService;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaService;

@Singleton
public class EleicaoPipelineService {
    private static final Logger logger = LoggerFactory.getLogger(EleicaoPipelineService.class);

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

    public void executar() {
        Instant inicioGeral = Instant.now();
        logger.info("Iniciando pipeline de coleta de votos do TSE para {}/{}...", config.uf(), config.codigoMunicipio());

        validarPreCondicoes();
        executarFase1MunicipioConfig();
        executarFase2Enriquecimento();
        executarFase3DownloadBoletins();
        executarFase4ApuracaoVotos();
        executarFase5MergeVotos();
        executarFase6ExportacaoCarto();

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

    private void executarFase1MunicipioConfig() {
        Instant inicio = Instant.now();
        logger.info("[Fase 1/6] Iniciando obtenção de configuração do município {}/{}...", config.uf(), config.codigoMunicipio());

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
        csvService.gerarCsv(List.of(abrangencia));

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "[Fase 1/6] Configuração municipal concluída em {} ms. Total de seções mapeadas: {}.",
                decorrido.toMillis(), totalSecoes);
    }

    private void executarFase2Enriquecimento() {
        Instant inicio = Instant.now();
        logger.info("[Fase 2/6] Iniciando enriquecimento de seções eleitorais...");

        CsvEnriquecedorService.ResultadoEnriquecimento resultado = csvEnriquecedorService.enriquecer();

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "[Fase 2/6] Enriquecimento concluído em {} ms. Total seções: {} (ativas: {},  inoperantes/agregadas: {}).",
                decorrido.toMillis(), resultado.totalSecoes(), resultado.totalAtivas(), resultado.totalInoperantes());
    }

    private void executarFase3DownloadBoletins() {
        Instant inicio = Instant.now();
        logger.info("[Fase 3/6] Iniciando download dos Boletins de Urna (BUs)...");

        BoletimUrnaDownloadService.ResultadoDownload resultado = boletimUrnaDownloader.baixarBoletins();

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "[Fase 3/6] Download dos boletins concluído em {} ms. Total baixados: {}, inoperantes ignorados: {}.",
                decorrido.toMillis(), resultado.totalBaixados(), resultado.totalInoperantesIgnorados());
    }

    private void executarFase4ApuracaoVotos() {
        Instant inicio = Instant.now();
        logger.info("[Fase 4/6] Iniciando decodificação ASN.1 e apuração de votos para Presidente...");

        List<ResumoVotacao> votacao = boletimUrnaService.processarBoletins(config.diretorioBoletins());
        csvBoletimUrnaService.processar(votacao);

        int totalVotos = votacao.stream().mapToInt(ResumoVotacao::votos).sum();
        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "[Fase 4/6] Apuração concluída em {} ms. Total de registros: {}, total de votos apurados: {}.",
                decorrido.toMillis(), votacao.size(), totalVotos);
    }

    private void executarFase5MergeVotos() {
        Instant inicio = Instant.now();
        logger.info("[Fase 5/6] Iniciando mesclagem dos votos com os dados territoriais...");

        try {
            int totalMesclados = csvMescladorBoletimService.mesclar();
            Duration decorrido = Duration.between(inicio, Instant.now());
            logger.info(
                    "[Fase 5/6] Mesclagem de votos concluída em {} ms. Total de registros mesclados: {}.",
                    decorrido.toMillis(), totalMesclados);
        } catch (IOException e) {
            logger.error("Falha na mesclagem de votos com dados municipais", e);
            throw new EleicaoDataProcessingException("Erro ao mesclar votos com dados dos municípios", e);
        }
    }

    private void executarFase6ExportacaoCarto() {
        Instant inicio = Instant.now();
        logger.info("[Fase 6/6] Iniciando cruzamento com coordenadas e exportação para o Carto.com...");

        try {
            CsvCartoExportadorService.ResultadoExportacaoCarto resultado = csvCartoExportadorService.exportar(
                    config.arquivoCoordenadasTse(),
                    config.arquivoResultadoMescladoCsv(),
                    config.arquivoCartoFinal(),
                    config.codigoMunicipio()
            );

            Duration decorrido = Duration.between(inicio, Instant.now());
            logger.info(
                    "[Fase 6/6] Exportação concluída em {} ms. Total exportados: {} (sem coordenadas: {}). Arquivo final: {}",
                    decorrido.toMillis(), resultado.totalExportados(), resultado.totalSemCoordenadas(), config.arquivoCartoFinal().toAbsolutePath());
        } catch (IOException e) {
            logger.error("Falha na exportação do dataset final para o Carto", e);
            throw new EleicaoDataProcessingException("Erro ao exportar dataset final para o Carto", e);
        }
    }
}
