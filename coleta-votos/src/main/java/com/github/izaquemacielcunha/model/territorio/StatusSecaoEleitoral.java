package com.github.izaquemacielcunha.model.territorio;

public final class StatusSecaoEleitoral {

    public static final String STATUS_INOPERANTE_AGREGADA = "inoperante/agregada";

    private StatusSecaoEleitoral() {}

    public static boolean isInoperanteOuAgregada(String status) {
        return status != null && STATUS_INOPERANTE_AGREGADA.equalsIgnoreCase(status.trim());
    }
}
