package com.github.izaquemacielcunha.model.urna.boletim;

public record ResumoVotacao(
        String candidato,
        int votos,
        String boletimUrna
) { }