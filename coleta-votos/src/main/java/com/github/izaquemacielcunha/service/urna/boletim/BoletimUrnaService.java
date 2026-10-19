package com.github.izaquemacielcunha.service.urna.boletim;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.github.izaquemacielcunha.model.tse.asn1.EntidadeBoletimUrna;
import com.github.izaquemacielcunha.model.tse.asn1.ResultadoVotacaoPorEleicao;
import com.github.izaquemacielcunha.model.tse.asn1.ResultadoVotacao;
import com.github.izaquemacielcunha.model.tse.asn1.TotalVotosCargo;
import com.github.izaquemacielcunha.model.tse.asn1.TotalVotosVotavel;
import com.github.izaquemacielcunha.model.tse.asn1.IdentificacaoVotavel;
import com.github.izaquemacielcunha.model.tse.asn1.EntidadeEnvelopeGenerico;
import com.github.izaquemacielcunha.model.urna.boletim.CargoConstitucionalTse;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;
import com.github.izaquemacielcunha.model.urna.boletim.TipoVotoBoletim;

@Singleton
public class BoletimUrnaService {
    private static final String VOTO_NULO_PABLO_MARCAL = "28";

    @Inject
    public BoletimUrnaService() {}

    public List<ResumoVotacao> processarDiretorio(String nomeDiretorio) {
        List<String> listaBoletins = listaArquivosBoletinsUrnas(nomeDiretorio);
        return listaBoletins.parallelStream()
                .flatMap(boletim -> {
                    try {
                        String idBoletim = boletim.substring(boletim.lastIndexOf(File.separatorChar) + 1);
                        return extrairResumoVotacao(boletim, idBoletim).stream();
                    } catch (Exception e) {
                        throw new RuntimeException("Erro ao extrair resumo do boletim: " + boletim, e);
                    }
                })
                .toList();
    }

    public List<ResumoVotacao> extrairResumoVotacao(String pathBoletimUrna, String idBoletimUrna) throws Exception {
        Optional<EntidadeBoletimUrna> boletimUrna = desenveloparBoletimUrna(pathBoletimUrna);
        if (boletimUrna.isPresent()) {
            return obterEleicoes(boletimUrna.get())
                    .flatMap(this::obterResultados)
                    .flatMap(this::obterTotaisCargo)
                    .filter(this::isPresidente)
                    .flatMap(this::obterVotosVotaveis)
                    .map(voto -> mapearParaResumo(voto, idBoletimUrna))
                    .toList();

        }
        return List.of();
    }

    public List<String> listaArquivosBoletinsUrnas(String nomeDiretorio) {
        Path dir = Path.of(nomeDiretorio);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao listar arquivos do diretório: " + nomeDiretorio, e);
        }
    }

    private Stream<ResultadoVotacaoPorEleicao> obterEleicoes(EntidadeBoletimUrna bu) {
        if (bu == null || bu.getResultadosVotacaoPorEleicao() == null) return Stream.empty();
        return nullSafeStream(bu.getResultadosVotacaoPorEleicao().getResultadoVotacaoPorEleicao());
    }

    private Stream<ResultadoVotacao> obterResultados(ResultadoVotacaoPorEleicao eleicao) {
        if (eleicao == null || eleicao.getResultadosVotacao() == null) return Stream.empty();
        return nullSafeStream(eleicao.getResultadosVotacao().getResultadoVotacao());
    }

    private Stream<TotalVotosCargo> obterTotaisCargo(ResultadoVotacao resultado) {
        if (resultado == null || resultado.getTotaisVotosCargo() == null) return Stream.empty();
        return nullSafeStream(resultado.getTotaisVotosCargo().getTotalVotosCargo());
    }

    private Stream<TotalVotosVotavel> obterVotosVotaveis(TotalVotosCargo total) {
        if (total == null || total.getVotosVotaveis() == null) return Stream.empty();
        return nullSafeStream(total.getVotosVotaveis().getTotalVotosVotavel());
    }

    private boolean isPresidente(TotalVotosCargo total) {
        return total.getCodigoCargo() != null
                && total.getCodigoCargo().getCargoConstitucional() != null
                && CargoConstitucionalTse.PRESIDENTE.coincideCom(total.getCodigoCargo().getCargoConstitucional().intValue());
    }

    private ResumoVotacao mapearParaResumo(TotalVotosVotavel voto, String idBoletim) {
        Integer codigoTipo = voto.getTipoVoto() != null ? voto.getTipoVoto().intValue() : null;
        TipoVotoBoletim tipoVoto = TipoVotoBoletim.fromCodigo(codigoTipo);
        int qtdVotos = voto.getQuantidadeVotos() != null ? voto.getQuantidadeVotos().intValue() : 0;

        String candidato = switch (tipoVoto) {
            case NOMINAL -> formatarCandidatoNominal(voto.getIdentificacaoVotavel());
            case BRANCO -> "BRANCO";
            case NULO -> "NULO";
            case DESCONHECIDO -> String.valueOf(codigoTipo != null ? codigoTipo : -1);
        };

        return new ResumoVotacao(candidato, qtdVotos, idBoletim);
    }

    private String formatarCandidatoNominal(IdentificacaoVotavel ident) {
        if (ident == null || ident.getCodigo() == null) {
            return "N/A";
        }

        String codigo = ident.getCodigo().toString();
        return VOTO_NULO_PABLO_MARCAL.equals(codigo) ? "NULO" : codigo;
    }

    private <T> Stream<T> nullSafeStream(Collection<T> collection) {
        return collection == null ? Stream.empty() : collection.stream();
    }

    private Optional<EntidadeBoletimUrna> desenveloparBoletimUrna(String filePath) throws Exception {
        if (filePath == null || filePath.trim().isEmpty() || !filePath.endsWith(".dat")) {
            return Optional.empty();
        }

        File arquivoDat = new File(filePath);

        if (!arquivoDat.exists()) {
            System.err.println("Arquivo não encontrado: " + arquivoDat.getAbsolutePath());
            return Optional.empty();
        }

        try (InputStream fis = new FileInputStream(arquivoDat)) {
            EntidadeEnvelopeGenerico envelope = new EntidadeEnvelopeGenerico();
            envelope.decode(fis);

            EntidadeBoletimUrna boletimUrna = new EntidadeBoletimUrna();
            try (ByteArrayInputStream buStream = new ByteArrayInputStream(envelope.getConteudo().value)) {
                boletimUrna.decode(buStream);
                return Optional.of(boletimUrna);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
