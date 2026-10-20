package com.github.izaquemacielcunha.model.territorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusSecaoEleitoralTest {

    @Test
    @DisplayName("Deve identificar corretamente status inoperante/agregada sem distinção de caixa")
    void deveIdentificarInoperanteAgregada() {
        assertTrue(StatusSecaoEleitoral.isInoperanteOuAgregada("inoperante/agregada"));
        assertTrue(StatusSecaoEleitoral.isInoperanteOuAgregada("INOPERANTE/AGREGADA"));
        assertTrue(StatusSecaoEleitoral.isInoperanteOuAgregada("  inoperante/agregada  "));

        assertFalse(StatusSecaoEleitoral.isInoperanteOuAgregada("ativa"));
        assertFalse(StatusSecaoEleitoral.isInoperanteOuAgregada("bu-123.dat"));
        assertFalse(StatusSecaoEleitoral.isInoperanteOuAgregada(null));
        assertFalse(StatusSecaoEleitoral.isInoperanteOuAgregada(""));
    }
}
