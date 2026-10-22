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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.izaquemacielcunha.exception.EleicaoDataProcessingException;
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
public class BoletimUrnaServiceImpl implements BoletimUrnaService {
    private static final Logger logger = LoggerFactory.getLogger(BoletimUrnaServiceImpl.class);
    private static final String VOTO_NULO_PABLO_MARCAL = "28";

    @Inject
    public BoletimUrnaServiceImpl() {}

    @Override
    public List<ResumoVotacao> processarDiretorio(Path diretorioBoletins) {
        return processarBoletins(diretorioBoletins);
    }

    private List<ResumoVotacao> processarDiretorio(String nomeDiretorio) {
        return processarBoletins(nomeDiretorio);
    }

    private List<ResumoVotacao> processarBoletins(Path diretorio) {
        Objects.requireNonNull(diretorio, "Diretório de boletins não pode ser nulo");
        List<Path> listaBoletins = listarArquivosBoletins(diretorio);
        return listaBoletins.parallelStream()
                .flatMap(boletimPath -> {
                    try {
                        String idBoletim = boletimPath.getFileName().toString();
                        return extrairResumoVotacao(boletimPath.toString(), idBoletim).stream();
                    } catch (Exception e) {
                        logger.error("Erro ao extrair resumo do boletim: {}", boletimPath, e);
                        throw new EleicaoDataProcessingException("Erro ao extrair resumo do boletim: " + boletimPath, e);
                    }
                })
                .toList();
    }

    private List<ResumoVotacao> processarBoletins(String nomeDiretorio) {
        Objects.requireNonNull(nomeDiretorio, "Nome do diretório não pode ser nulo");
        return processarBoletins(Path.of(nomeDiretorio));
    }

    private List<ResumoVotacao> extrairResumoVotacao(String pathBoletimUrna, String idBoletimUrna) {
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

    private List<Path> listarArquivosBoletins(Path diretorio) {
        if (!Files.isDirectory(diretorio)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(diretorio)) {
            return stream
                    .filter(Files::isRegularFile)
                    .toList();
        } catch (IOException e) {
            logger.error("Erro ao listar arquivos do diretório: {}", diretorio, e);
            throw new EleicaoDataProcessingException("Erro ao listar arquivos do diretório: " + diretorio, e);
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

    private Optional<EntidadeBoletimUrna> desenveloparBoletimUrna(String filePath) {
        if (filePath == null || filePath.trim().isEmpty() || !filePath.endsWith(".dat")) {
            return Optional.empty();
        }

        File arquivoDat = new File(filePath);

        if (!arquivoDat.exists()) {
            logger.warn("Arquivo .dat ausente ou inacessível: {}", arquivoDat.getAbsolutePath());
            return Optional.empty();
        }

        try (InputStream fis = new FileInputStream(arquivoDat)) {
            EntidadeEnvelopeGenerico envelope = new EntidadeEnvelopeGenerico();
            envelope.decode(fis);

            EntidadeBoletimUrna boletimUrna = new EntidadeBoletimUrna();
            try (ByteArrayInputStream buStream = new ByteArrayInputStream(envelope.getConteudo().value)) {
                boletimUrna.decode(buStream);
                return Optional.of(boletimUrna);
            } catch (Exception ex) {
                logger.error("Corrupção ou falha na estrutura ASN.1 do Boletim de Urna: {}", filePath, ex);
                throw new EleicaoDataProcessingException("Erro ao decodificar conteúdo ASN.1 do Boletim de Urna: " + filePath, ex);
            }
        } catch (EleicaoDataProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Erro ao ler ou decodificar envelope ASN.1: {}", filePath, ex);
            throw new EleicaoDataProcessingException("Erro ao ler ou decodificar envelope ASN.1: " + filePath, ex);
        }
    }
}
