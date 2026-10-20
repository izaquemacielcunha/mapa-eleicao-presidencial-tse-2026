package com.github.izaquemacielcunha.model.territorio;

/**
 * Constantes e utilitários que representam os status possíveis de uma seção eleitoral.
 */
public final class StatusSecaoEleitoral {

    public static final String STATUS_INOPERANTE_AGREGADA = "inoperante/agregada";

    private StatusSecaoEleitoral() {}

    public static boolean isInoperanteOuAgregada(String status) {
        return status != null && STATUS_INOPERANTE_AGREGADA.equalsIgnoreCase(status.trim());
    }
}
