package com.github.izaquemacielcunha.service.urna.boletim;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.github.izaquemacielcunha.model.tse.asn1.EntidadeBoletimUrna;
import com.github.izaquemacielcunha.model.tse.asn1.ResultadoVotacaoPorEleicao;
import com.github.izaquemacielcunha.model.tse.asn1.ResultadoVotacao;
import com.github.izaquemacielcunha.model.tse.asn1.TotalVotosCargo;
import com.github.izaquemacielcunha.model.tse.asn1.TotalVotosVotavel;
import com.github.izaquemacielcunha.model.tse.asn1.IdentificacaoVotavel;
import com.github.izaquemacielcunha.model.tse.asn1.EntidadeEnvelopeGenerico;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;

public class BoletimUrnaService {
    private static final int CARGO_PRESIDENTE = 1;
    private static final int VOTO_NOMINAL = 1;
    private static final int VOTO_BRANCO = 2;
    private static final int VOTO_NULO = 3;
    private static final String VOTO_NULO_PABLO_MARCAL = "28";

    public BoletimUrnaService() {}

    public List<ResumoVotacao> extrairResumoVotacao(String pathBoletimUrna, String idBoletinUrna) throws Exception {
        Optional<EntidadeBoletimUrna> boletimUrna = desenveloparBoletimUrna(pathBoletimUrna);
        if (boletimUrna.isPresent()) {
            return obterEleicoes(boletimUrna.get())
                    .flatMap(this::obterResultados)
                    .flatMap(this::obterTotaisCargo)
                    .filter(this::isPresidente)
                    .flatMap(this::obterVotosVotaveis)
                    .map(voto -> mapearParaResumo(voto, idBoletinUrna))
                    .toList();

        }
        return List.of();
    }

    public List<String> listaArquivosBoletinsUrnas(String nomeDiretorio) {
        File diretorio = new File(nomeDiretorio);

        List<String> listaArquivos = List.of();
        if (diretorio.exists()) {
            listaArquivos =  Stream.of(diretorio.listFiles()).parallel()
                    .filter(file -> !file.isDirectory())
                    .map(File::getPath)
                    .toList();
        }
        return listaArquivos;
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
                && total.getCodigoCargo().getCargoConstitucional().intValue() == CARGO_PRESIDENTE;
    }

    private ResumoVotacao mapearParaResumo(TotalVotosVotavel voto, String idBoletim) {
        int tipoVoto = voto.getTipoVoto() != null ? voto.getTipoVoto().intValue() : -1;
        int qtdVotos = voto.getQuantidadeVotos() != null ? voto.getQuantidadeVotos().intValue() : 0;

        String candidato = switch (tipoVoto) {
            case VOTO_NOMINAL -> formatarCandidatoNominal(voto.getIdentificacaoVotavel());
            case VOTO_BRANCO -> "BRANCO";
            case VOTO_NULO -> "NULO";
            default -> String.valueOf(tipoVoto);
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
            IO.println("Arquivo não encontrado: " + arquivoDat.getAbsolutePath());
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

}// end of class
