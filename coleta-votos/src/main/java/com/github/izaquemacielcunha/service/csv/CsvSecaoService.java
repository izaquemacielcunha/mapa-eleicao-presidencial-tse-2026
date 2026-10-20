package com.github.izaquemacielcunha.service.csv;

import java.util.List;
import com.github.izaquemacielcunha.model.Abrangencia;

public interface CsvSecaoService {
    void exportarSecoesMunicipaisParaCsv(List<Abrangencia> abrangencias);
}
