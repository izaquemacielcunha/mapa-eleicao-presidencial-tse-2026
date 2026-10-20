package com.github.izaquemacielcunha.model.csv;

/**
 * Interface que padroniza a geração da chave composta de identificação de seção eleitoral
 * no formato {@code UF-MUNICIPIO-ZONA-SECAO}, garantindo a eliminação de zeros à esquerda
 * para junção consistente entre diferentes fontes de dados (Carto e apuração do TSE).
 */
public interface ChaveSecaoEleitoral {

    String unidadeFederativa();

    String codigoMunicipio();

    String numeroZona();

    String numeroSecao();

    default String gerarChaveComposta() {
        return normalizarChave(unidadeFederativa(), codigoMunicipio(), numeroZona(), numeroSecao());
    }

    static String normalizarChave(String uf, String codigoMunicipio, String numeroZona, String numeroSecao) {
        int zona = (numeroZona != null && !numeroZona.isBlank()) ? Integer.parseInt(numeroZona.trim()) : 0;
        int secao = (numeroSecao != null && !numeroSecao.isBlank()) ? Integer.parseInt(numeroSecao.trim()) : 0;
        String ufStr = (uf != null) ? uf.trim() : "";
        String munStr = (codigoMunicipio != null) ? codigoMunicipio.trim() : "";
        return ufStr + "-" + munStr + "-" + zona + "-" + secao;
    }
}
