package com.github.izaquemacielcunha.dependencyinjection;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.service.EleicaoPipelineService;
import com.github.izaquemacielcunha.service.EleicaoPipelineServiceImpl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationComponentTest {

    @Test
    @DisplayName("DaggerApplicationComponent deve resolver com sucesso todo o grafo de dependências via ServiceModule")
    void deveConstruirGrafoDeDependenciasComSucesso(@TempDir Path tempDir) {
        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                tempDir,
                tempDir.resolve("boletins")
        );

        ApplicationComponent component = DaggerApplicationComponent.factory().create(config);
        assertNotNull(component, "ApplicationComponent não deve ser nulo");

        EleicaoPipelineService pipelineService = component.pipelineService();
        assertNotNull(pipelineService, "EleicaoPipelineService deve ser resolvido pelo Dagger");
        assertTrue(pipelineService instanceof EleicaoPipelineServiceImpl, "Instância concreta deve ser EleicaoPipelineServiceImpl");
    }
}
