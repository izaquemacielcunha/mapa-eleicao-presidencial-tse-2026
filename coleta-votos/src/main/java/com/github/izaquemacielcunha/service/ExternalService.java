package com.github.izaquemacielcunha.service;

import java.nio.file.Path;
import java.util.Optional;

import com.github.izaquemacielcunha.model.Municipio;
import com.github.izaquemacielcunha.model.urna.Metadados;

public interface ExternalService {

    Optional<Municipio> buscarConfiguracaoMunicipio(String url, String codigoMunicipal);

    Optional<Metadados> buscarMetadadosSecao(String url);

    Optional<Path> baixarArquivoBoletimUrnaDat(String url, Path diretorioDestino);

    @Deprecated
    default Optional<Municipio> callMunicipioConfig(String url, String codigoMunicipal) {
        return buscarConfiguracaoMunicipio(url, codigoMunicipal);
    }

    @Deprecated
    default Optional<Metadados> callZonaSecaoInfos(String url) {
        return buscarMetadadosSecao(url);
    }
}
