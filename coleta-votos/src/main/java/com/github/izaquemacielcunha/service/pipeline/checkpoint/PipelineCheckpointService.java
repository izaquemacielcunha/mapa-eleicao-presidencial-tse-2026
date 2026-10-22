package com.github.izaquemacielcunha.service.pipeline.checkpoint;

import com.github.izaquemacielcunha.model.pipeline.EtapaPipeline;

public interface PipelineCheckpointService {

    void prepararContextoExecucao();

    boolean isEtapaConcluida(EtapaPipeline etapa);

    void registrarEtapaConcluida(EtapaPipeline etapa);

    void limparCheckpoint();
}
