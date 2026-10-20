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

    private final BoletimUrnaService service = new BoletimUrnaService();

    @Test
    @DisplayName("processarBoletins deve retornar lista vazia para diretório vazio")
    void deveRetornarListaVaziaParaDiretorioVazio(@TempDir Path tempDir) {
        List<ResumoVotacao> resultado = service.processarBoletins(tempDir);
        assertEquals(0, resultado.size());
    }

    @Test
    @DisplayName("processarBoletins deve ignorar arquivos que não terminam com .dat")
    void deveIgnorarArquivosSemExtensaoDat(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("ignore.txt"), "ignorar");
        Files.writeString(tempDir.resolve("dados.csv"), "col1,col2");

        List<ResumoVotacao> resultado = service.processarBoletins(tempDir);
        assertEquals(0, resultado.size());
    }

    @Test
    @DisplayName("processarBoletins deve retornar lista vazia para diretório inexistente")
    void deveRetornarListaVaziaParaDiretorioInexistente() {
        List<ResumoVotacao> resultado = service.processarBoletins(Path.of("diretorio-inexistente-12345"));
        assertEquals(0, resultado.size());
    }

    @Test
    @DisplayName("processarBoletins deve lançar NullPointerException para argumento nulo")
    void deveLancarExcecaoParaDiretorioNulo() {
        assertThrows(NullPointerException.class, () -> service.processarBoletins((Path) null));
        assertThrows(NullPointerException.class, () -> service.processarBoletins((String) null));
    }

    @Test
    @DisplayName("extrairResumoVotacao deve retornar lista vazia quando arquivo não for .dat ou for inexistente")
    void extrairResumoVotacaoArquivoInvalidoOuInexistente() {
        List<ResumoVotacao> resultadoNaoDat = service.extrairResumoVotacao("arquivo.txt", "id1");
        assertTrue(resultadoNaoDat.isEmpty());

        List<ResumoVotacao> resultadoInexistente = service.extrairResumoVotacao("nao_existe.dat", "id2");
        assertTrue(resultadoInexistente.isEmpty());
    }

    @Test
    @DisplayName("processarBoletins deve lançar EleicaoDataProcessingException ao encontrar arquivo .dat corrompido")
    void deveLancarExcecaoParaArquivoDatCorrompido(@TempDir Path tempDir) throws IOException {
        Path arquivoCorrompido = tempDir.resolve("urna-corrompida.dat");
        Files.writeString(arquivoCorrompido, "dados binarios corrompidos que nao sao asn1");

        assertThrows(EleicaoDataProcessingException.class, () -> service.processarBoletins(tempDir));
    }
}
