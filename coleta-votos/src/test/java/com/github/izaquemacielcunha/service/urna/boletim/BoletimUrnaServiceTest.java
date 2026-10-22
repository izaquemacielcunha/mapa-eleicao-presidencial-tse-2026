package com.github.izaquemacielcunha.service.urna.boletim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.izaquemacielcunha.exception.EleicaoDataProcessingException;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;

import static org.junit.jupiter.api.Assertions.*;

class BoletimUrnaServiceTest {

    private final BoletimUrnaService service = new BoletimUrnaServiceImpl();

    @Test
    @DisplayName("processarDiretorio deve retornar lista vazia para diretório vazio")
    void deveRetornarListaVaziaParaDiretorioVazio(@TempDir Path tempDir) {
        List<ResumoVotacao> resultado = service.processarDiretorio(tempDir);
        assertEquals(0, resultado.size());
    }

    @Test
    @DisplayName("processarDiretorio deve ignorar arquivos que não terminam com .dat")
    void deveIgnorarArquivosSemExtensaoDat(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("ignore.txt"), "ignorar");
        Files.writeString(tempDir.resolve("dados.csv"), "col1,col2");

        List<ResumoVotacao> resultado = service.processarDiretorio(tempDir);
        assertEquals(0, resultado.size());
    }

    @Test
    @DisplayName("processarDiretorio deve retornar lista vazia para diretório inexistente")
    void deveRetornarListaVaziaParaDiretorioInexistente() {
        List<ResumoVotacao> resultado = service.processarDiretorio(Path.of("diretorio-inexistente-12345"));
        assertEquals(0, resultado.size());
    }

    @Test
    @DisplayName("processarDiretorio deve lançar NullPointerException para argumento nulo")
    void deveLancarExcecaoParaDiretorioNulo() {
        assertThrows(NullPointerException.class, () -> service.processarDiretorio(null));
    }

    @Test
    @DisplayName("processarDiretorio deve retornar lista vazia quando diretório contiver apenas arquivo não .dat")
    void deveRetornarListaVaziaParaArquivoNaoDat(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("arquivo.txt"), "conteudo texto");
        List<ResumoVotacao> resultado = service.processarDiretorio(tempDir);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("processarDiretorio deve lançar EleicaoDataProcessingException ao encontrar arquivo .dat corrompido")
    void deveLancarExcecaoParaArquivoDatCorrompido(@TempDir Path tempDir) throws IOException {
        Path arquivoCorrompido = tempDir.resolve("urna-corrompida.dat");
        Files.writeString(arquivoCorrompido, "dados binarios corrompidos que nao sao asn1");

        assertThrows(EleicaoDataProcessingException.class, () -> service.processarDiretorio(tempDir));
    }
}
