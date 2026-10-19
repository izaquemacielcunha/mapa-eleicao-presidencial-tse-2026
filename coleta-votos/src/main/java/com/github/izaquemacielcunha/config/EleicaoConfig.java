package com.github.izaquemacielcunha.config;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public record EleicaoConfig(
        String uf,
        String codigoMunicipio,
        String codigoPleito,
        String cicloEleitoral,
        Path diretorioTemp,
        Path diretorioBoletins
) {
    public static final String DEFAULT_UF = "PR";
    public static final String DEFAULT_COD_MUNICIPIO = "75353";
    public static final String DEFAULT_COD_PLEITO = "3220";
    public static final String DEFAULT_CICLO = "ele2026";
    public static final Path DEFAULT_DIR_TEMP = Path.of("temp-files");
    public static final Path DEFAULT_DIR_BOLETINS = Path.of("temp-files/boletins-urnas");

    public static EleicaoConfig fromArgs(String[] args) {
        if (args == null || args.length == 0) {
            return padrao();
        }
        if (args.length < 2) {
            throw new IllegalArgumentException("Parâmetros insuficientes. Uso: java App <UF> <CODIGO_MUNICIPIO> (ex: PR 75353)");
        }
        return criar(args[0], args[1]);
    }

    public static EleicaoConfig criar(String uf, String codigoMunicipio) {
        Objects.requireNonNull(uf, "UF não pode ser nula");
        Objects.requireNonNull(codigoMunicipio, "Código do município não pode ser nulo");

        String ufLimpa = uf.trim().toUpperCase();
        String codMunLimpo = codigoMunicipio.trim();

        if (ufLimpa.isBlank()) {
            throw new IllegalArgumentException("UF não pode ser vazia");
        }
        if (codMunLimpo.isBlank()) {
            throw new IllegalArgumentException("Código do município não pode ser vazio");
        }

        return new EleicaoConfig(
                ufLimpa,
                codMunLimpo,
                DEFAULT_COD_PLEITO,
                DEFAULT_CICLO,
                DEFAULT_DIR_TEMP,
                DEFAULT_DIR_BOLETINS
        );
    }

    public static EleicaoConfig padrao() {
        return criar(DEFAULT_UF, DEFAULT_COD_MUNICIPIO);
    }

    public File arquivoTempCsv() {
        return diretorioTemp.resolve("temp.csv").toFile();
    }

    public File arquivoEnriquecidoCsv() {
        return diretorioTemp.resolve("arquivo-enriquecido.csv").toFile();
    }

    public File arquivoBoletimUrnaCsv() {
        return diretorioTemp.resolve("arquivo-boletim-urna.csv").toFile();
    }

    public File arquivoResultadoMescladoCsv() {
        return diretorioTemp.resolve("resultado-mesclado.csv").toFile();
    }

    public Path arquivoCoordenadasTse() {
        return diretorioTemp.resolve(String.format("eleitorado_local_votacao_2026_%s.csv", uf));
    }

    public File arquivoCartoFinal() {
        return Path.of(String.format("carto-dataset-final-eleic-presi-2026-1-turn-%s.csv", uf.toLowerCase())).toFile();
    }
}
