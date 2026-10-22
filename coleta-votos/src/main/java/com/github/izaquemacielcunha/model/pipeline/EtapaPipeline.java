package com.github.izaquemacielcunha.model.pipeline;

import java.util.Objects;

public enum EtapaPipeline {
    ESTRUTURA_SECOES(1, "Estrutura de Seções"),
    ENRIQUECIMENTO_METADADOS(2, "Enriquecimento de Metadados"),
    DOWNLOAD_BOLETINS(3, "Download de Boletins"),
    APURACAO_VOTOS(4, "Apuração de Votos"),
    MESCLAGEM_VOTOS(5, "Mesclagem Territorial"),
    EXPORTACAO_CARTO(6, "Exportação Carto");

    private final int ordem;
    private final String descricao;

    EtapaPipeline(int ordem, String descricao) {
        this.ordem = ordem;
        this.descricao = Objects.requireNonNull(descricao, "Descrição não pode ser nula");
    }

    public int ordem() {
        return ordem;
    }

    public String descricao() {
        return descricao;
    }
}
