package com.github.izaquemacielcunha.service.csv;

import java.io.File;
import java.util.List;

import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.model.csv.LinhaBoletimUrna;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;


public class CsvBoletimUrnaService {
    private final CsvMapper mapper;

    public CsvBoletimUrnaService(CsvMapper mapper) {
        this.mapper = mapper;
    }

    public void process(List<ResumoVotacao> resultados) {

        List<LinhaBoletimUrna> linhasBoletins = resultados.parallelStream().map(resumoVotacao ->
                new LinhaBoletimUrna(
                        resumoVotacao.candidato(),
                        resumoVotacao.votos(),
                        resumoVotacao.boletimUrna()))
                .toList();

        CsvSchema schemaEscrita = mapper.schemaFor(LinhaBoletimUrna.class).withHeader();
        mapper.writer(schemaEscrita).writeValue(new File("arquivo-boletim-urna.csv"), linhasBoletins);
    }

}// end of class
