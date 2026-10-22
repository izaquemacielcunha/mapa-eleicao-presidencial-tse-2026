package com.github.izaquemacielcunha.model.territorio;

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
