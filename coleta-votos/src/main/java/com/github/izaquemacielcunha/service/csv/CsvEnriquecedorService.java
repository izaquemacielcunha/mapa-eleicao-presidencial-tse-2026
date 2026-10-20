package com.github.izaquemacielcunha.service.csv;

public interface CsvEnriquecedorService {
    void enriquecerMetadadosSecoes();

    @Deprecated
    default CsvEnriquecedorServiceImpl.ResultadoEnriquecimento enriquecer() {
        if (this instanceof CsvEnriquecedorServiceImpl impl) {
            return impl.enriquecer();
        }
        enriquecerMetadadosSecoes();
        return new CsvEnriquecedorServiceImpl.ResultadoEnriquecimento(0, 0, 0);
    }
}
