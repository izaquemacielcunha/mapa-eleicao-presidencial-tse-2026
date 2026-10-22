package com.github.izaquemacielcunha.model.pipeline.checkpoint;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.model.pipeline.EtapaPipeline;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public record EstadoCheckpoint(
        String uf,
        String codigoMunicipio,
        String codigoPleito,
        String cicloEleitoral,
        Set<EtapaPipeline> etapasConcluidas,
        Instant ultimaAtualizacao
) {

    public EstadoCheckpoint {
        Objects.requireNonNull(uf, "UF não pode ser nula");
        Objects.requireNonNull(codigoMunicipio, "Código do município não pode ser nulo");
        Objects.requireNonNull(codigoPleito, "Código do pleito não pode ser nulo");
        Objects.requireNonNull(cicloEleitoral, "Ciclo eleitoral não pode ser nulo");
        etapasConcluidas = etapasConcluidas == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(etapasConcluidas));
        if (ultimaAtualizacao == null) {
            ultimaAtualizacao = Instant.now();
        }
    }

    public static EstadoCheckpoint inicial(EleicaoConfig config) {
        Objects.requireNonNull(config, "Configuração de eleição não pode ser nula");
        return new EstadoCheckpoint(
                config.uf(),
                config.codigoMunicipio(),
                config.codigoPleito(),
                config.cicloEleitoral(),
                Collections.emptySet(),
                Instant.now()
        );
    }

    public boolean pertenceAoContexto(EleicaoConfig config) {
        if (config == null) {
            return false;
        }
        return this.uf.equalsIgnoreCase(config.uf())
                && this.codigoMunicipio.equals(config.codigoMunicipio())
                && this.codigoPleito.equals(config.codigoPleito())
                && this.cicloEleitoral.equalsIgnoreCase(config.cicloEleitoral());
    }

    public EstadoCheckpoint comEtapaConcluida(EtapaPipeline etapa) {
        Objects.requireNonNull(etapa, "Etapa não pode ser nula");
        Set<EtapaPipeline> novasEtapas = new HashSet<>(this.etapasConcluidas);
        novasEtapas.add(etapa);
        return new EstadoCheckpoint(
                this.uf,
                this.codigoMunicipio,
                this.codigoPleito,
                this.cicloEleitoral,
                novasEtapas,
                Instant.now()
        );
    }
}
