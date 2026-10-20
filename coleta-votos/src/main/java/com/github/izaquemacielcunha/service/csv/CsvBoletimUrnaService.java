package com.github.izaquemacielcunha.service.csv;

import java.util.List;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;

public interface CsvBoletimUrnaService {
    void exportarVotosParaCsv(List<ResumoVotacao> resultados);
}
