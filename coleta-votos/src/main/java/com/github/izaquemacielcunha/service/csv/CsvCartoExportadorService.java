package com.github.izaquemacielcunha.service.csv;

import java.nio.file.Path;

public interface CsvCartoExportadorService {
    void exportarDatasetCarto(Path arquivoCoordenadasTse, Path arquivoMesclado, Path arquivoDestino, String codigoMunicipioAlvo);
}
