package com.github.izaquemacielcunha.model.checkpoint;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.dependencyinjection.MapperModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Domínio do Checkpoint")
class EstadoCheckpointTest {

    private final ObjectMapper objectMapper = MapperModule.provideObjectMapper();

    @Test
    @DisplayName("Deve criar estado inicial e validar contexto correto")
    void deveCriarEstadoInicialEValidarContexto() {
        EleicaoConfig config = EleicaoConfig.padrao();
        EstadoCheckpoint estado = EstadoCheckpoint.inicial(config);

        assertEquals("PR", estado.uf());
        assertEquals("75353", estado.codigoMunicipio());
        assertEquals("3220", estado.codigoPleito());
        assertEquals("ele2026", estado.cicloEleitoral());
        assertTrue(estado.etapasConcluidas().isEmpty());
        assertNotNull(estado.ultimaAtualizacao());
        assertTrue(estado.pertenceAoContexto(config));
    }

    @Test
    @DisplayName("Deve rejeitar contexto divergente")
    void deveRejeitarContextoDivergente() {
        EleicaoConfig configOriginal = EleicaoConfig.padrao();
        EstadoCheckpoint estado = EstadoCheckpoint.inicial(configOriginal);

        EleicaoConfig configOutroMunicipio = new EleicaoConfig(
                "PR", "75000", "3220", "ele2026",
                configOriginal.diretorioTemp(), configOriginal.diretorioBoletins(),
                configOriginal.arquivoCoordenadasTse(), true
        );
        assertFalse(estado.pertenceAoContexto(configOutroMunicipio));

        EleicaoConfig configOutraUf = new EleicaoConfig(
                "SC", "75353", "3220", "ele2026",
                configOriginal.diretorioTemp(), configOriginal.diretorioBoletins(),
                configOriginal.arquivoCoordenadasTse(), true
        );
        assertFalse(estado.pertenceAoContexto(configOutraUf));

        assertFalse(estado.pertenceAoContexto(null));
    }

    @Test
    @DisplayName("Deve adicionar etapa mantendo imutabilidade")
    void deveAdicionarEtapaMantendoImutabilidade() {
        EleicaoConfig config = EleicaoConfig.padrao();
        EstadoCheckpoint estadoInicial = EstadoCheckpoint.inicial(config);

        EstadoCheckpoint estadoComEtapa1 = estadoInicial.comEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES);
        assertTrue(estadoInicial.etapasConcluidas().isEmpty(), "Estado inicial deve permanecer imutável");
        assertEquals(Set.of(EtapaPipeline.ESTRUTURA_SECOES), estadoComEtapa1.etapasConcluidas());

        EstadoCheckpoint estadoComEtapa2 = estadoComEtapa1.comEtapaConcluida(EtapaPipeline.ENRIQUECIMENTO_METADADOS);
        assertEquals(2, estadoComEtapa2.etapasConcluidas().size());
        assertTrue(estadoComEtapa2.etapasConcluidas().contains(EtapaPipeline.ESTRUTURA_SECOES));
        assertTrue(estadoComEtapa2.etapasConcluidas().contains(EtapaPipeline.ENRIQUECIMENTO_METADADOS));
    }

    @Test
    @DisplayName("Deve serializar e desserializar com Jackson 3.x corretamente")
    void deveSerializarEDesserializarComJackson() throws Exception {
        EstadoCheckpoint original = new EstadoCheckpoint(
                "PR",
                "75353",
                "3220",
                "ele2026",
                Set.of(EtapaPipeline.ESTRUTURA_SECOES, EtapaPipeline.DOWNLOAD_BOLETINS),
                Instant.parse("2026-10-20T12:00:00Z")
        );

        String json = objectMapper.writeValueAsString(original);
        assertNotNull(json);

        EstadoCheckpoint desserializado = objectMapper.readValue(json, EstadoCheckpoint.class);
        assertEquals(original.uf(), desserializado.uf());
        assertEquals(original.codigoMunicipio(), desserializado.codigoMunicipio());
        assertEquals(original.codigoPleito(), desserializado.codigoPleito());
        assertEquals(original.cicloEleitoral(), desserializado.cicloEleitoral());
        assertEquals(original.etapasConcluidas(), desserializado.etapasConcluidas());
        assertEquals(original.ultimaAtualizacao(), desserializado.ultimaAtualizacao());
    }
}
