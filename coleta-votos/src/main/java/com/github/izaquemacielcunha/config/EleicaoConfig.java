package com.github.izaquemacielcunha.config;

import java.nio.file.Path;
import java.util.Objects;

public record EleicaoConfig(
        String uf,
        String codigoMunicipio,
        String codigoPleito,
        String cicloEleitoral,
        Path diretorioTemp,
        Path diretorioBoletins,
        Path arquivoCoordenadasTse,
        boolean limparArquivosTemporarios
) {
    public static final String DEFAULT_UF = "PR";
    public static final String DEFAULT_COD_MUNICIPIO = "75353";
    public static final String DEFAULT_COD_PLEITO = "3220";
    public static final String DEFAULT_CICLO = "ele2026";
    public static final Path DEFAULT_DIR_TEMP = Path.of("temp-files");
    public static final Path DEFAULT_DIR_BOLETINS = Path.of("temp-files/boletins-urnas");
    public static final Path DEFAULT_ARQUIVO_COORDENADAS = Path.of("dados/eleitorado_local_votacao_2026_PR.csv");
    public static final boolean DEFAULT_LIMPAR_ARQUIVOS_TEMPORARIOS = true;

    public EleicaoConfig {
        Objects.requireNonNull(uf, "UF não pode ser nula");
        Objects.requireNonNull(codigoMunicipio, "Código do município não pode ser nulo");
        Objects.requireNonNull(codigoPleito, "Código do pleito não pode ser nulo");
        Objects.requireNonNull(cicloEleitoral, "Ciclo eleitoral não pode ser nulo");
        Objects.requireNonNull(diretorioTemp, "Diretório temporário não pode ser nulo");
        Objects.requireNonNull(diretorioBoletins, "Diretório de boletins não pode ser nulo");
        Objects.requireNonNull(arquivoCoordenadasTse, "Arquivo de coordenadas do TSE não pode ser nulo");

        uf = uf.trim().toUpperCase();
        codigoMunicipio = codigoMunicipio.trim();
        codigoPleito = codigoPleito.trim();
        cicloEleitoral = cicloEleitoral.trim();

        if (uf.isBlank()) {
            throw new IllegalArgumentException("UF não pode ser vazia");
        }
        if (codigoMunicipio.isBlank()) {
            throw new IllegalArgumentException("Código do município não pode ser vazio");
        }
    }

    public EleicaoConfig(
            String uf,
            String codigoMunicipio,
            String codigoPleito,
            String cicloEleitoral,
            Path diretorioTemp,
            Path diretorioBoletins,
            Path arquivoCoordenadasTse
    ) {
        this(
                uf,
                codigoMunicipio,
                codigoPleito,
                cicloEleitoral,
                diretorioTemp,
                diretorioBoletins,
                arquivoCoordenadasTse,
                DEFAULT_LIMPAR_ARQUIVOS_TEMPORARIOS
        );
    }

    public EleicaoConfig(
            String uf,
            String codigoMunicipio,
            String codigoPleito,
            String cicloEleitoral,
            Path diretorioTemp,
            Path diretorioBoletins
    ) {
        this(
                uf,
                codigoMunicipio,
                codigoPleito,
                cicloEleitoral,
                diretorioTemp,
                diretorioBoletins,
                DEFAULT_ARQUIVO_COORDENADAS,
                DEFAULT_LIMPAR_ARQUIVOS_TEMPORARIOS
        );
    }

    public static EleicaoConfig fromArgs(String[] args) {
        if (args == null || args.length < 3) {
            throw new IllegalArgumentException(
                    "Parâmetros insuficientes. Uso: java App <UF> <CODIGO_MUNICIPIO> <CAMINHO_ARQUIVO_COORDENADAS> [LIMPAR_ARQUIVOS_TEMPORARIOS] " +
                    "(ex: java App PR 75353 ./dados/eleitorado_local_votacao_2026_PR.csv [true|false])"
            );
        }
        boolean limparArquivos = DEFAULT_LIMPAR_ARQUIVOS_TEMPORARIOS;
        if (args.length >= 4 && args[3] != null && !args[3].isBlank()) {
            limparArquivos = Boolean.parseBoolean(args[3].trim());
        }
        return new EleicaoConfig(
                args[0],
                args[1],
                DEFAULT_COD_PLEITO,
                DEFAULT_CICLO,
                DEFAULT_DIR_TEMP,
                DEFAULT_DIR_BOLETINS,
                Path.of(args[2]),
                limparArquivos
        );
    }

    public static EleicaoConfig padrao() {
        return new EleicaoConfig(
                DEFAULT_UF,
                DEFAULT_COD_MUNICIPIO,
                DEFAULT_COD_PLEITO,
                DEFAULT_CICLO,
                DEFAULT_DIR_TEMP,
                DEFAULT_DIR_BOLETINS,
                DEFAULT_ARQUIVO_COORDENADAS,
                DEFAULT_LIMPAR_ARQUIVOS_TEMPORARIOS
        );
    }

    public Path arquivoTempCsv() {
        return diretorioTemp.resolve("temp.csv");
    }

    public Path arquivoEnriquecidoCsv() {
        return diretorioTemp.resolve("arquivo-enriquecido.csv");
    }

    public Path arquivoBoletimUrnaCsv() {
        return diretorioTemp.resolve("arquivo-boletim-urna.csv");
    }

    public Path arquivoResultadoMescladoCsv() {
        return diretorioTemp.resolve("resultado-mesclado.csv");
    }

    public Path arquivoCheckpointJson() {
        return diretorioTemp.resolve("checkpoint-status.json");
    }

    public Path arquivoCartoFinal() {
        return Path.of(String.format("carto-dataset-final-eleic-presi-2026-1-turn-%s.csv", uf.toLowerCase()));
    }
}
