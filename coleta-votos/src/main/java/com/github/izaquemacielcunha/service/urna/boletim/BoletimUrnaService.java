package com.github.izaquemacielcunha.service.urna.boletim;

import java.nio.file.Path;
import java.util.List;

import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;

public interface BoletimUrnaService {
    List<ResumoVotacao> processarDiretorio(Path diretorioBoletins);
}
