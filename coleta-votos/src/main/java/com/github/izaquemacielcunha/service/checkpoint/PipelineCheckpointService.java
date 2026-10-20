package com.github.izaquemacielcunha.service.checkpoint;

import com.github.izaquemacielcunha.model.checkpoint.EtapaPipeline;

/**
 * Contrato de serviço responsável pela gestão de checkpoints e resiliência do pipeline eleitoral.
 */
public interface PipelineCheckpointService {

    /**
     * Prepara o contexto de execução, garantindo a criação dos diretórios necessários
     * e validando se o arquivo de checkpoint existente pertence ao mesmo município/UF/pleito.
     */
    void prepararContextoExecucao();

    /**
     * Verifica se a etapa informada já foi concluída e se seus artefatos físicos de saída
     * estão presentes e íntegros no disco.
     *
     * @param etapa a etapa do pipeline a verificar
     * @return true se a etapa foi concluída e seus artefatos são válidos; false caso contrário
     */
    boolean isEtapaConcluida(EtapaPipeline etapa);

    /**
     * Registra uma etapa como concluída e persiste o estado de forma atômica no disco.
     *
     * @param etapa a etapa do pipeline recém-concluída
     */
    void registrarEtapaConcluida(EtapaPipeline etapa);

    /**
     * Remove o arquivo de checkpoint do disco e reinicializa o estado em memória.
     */
    void limparCheckpoint();
}
