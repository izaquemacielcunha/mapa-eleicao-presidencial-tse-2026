package com.github.izaquemacielcunha.service;

import java.nio.file.Path;
import java.util.Optional;

import com.github.izaquemacielcunha.model.Municipio;
import com.github.izaquemacielcunha.model.urna.Metadados;

public interface ExternalService {

    Optional<Municipio> callMunicipioConfig(String url, String codigoMunicipal) throws Exception;

    Optional<Metadados> callZonaSecaoInfos(String url) throws Exception;

    Optional<Path> baixarArquivoBoletimUrnaDat(String url, Path diretorioDestino) throws Exception;

}
