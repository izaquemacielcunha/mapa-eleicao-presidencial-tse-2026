package com.github.izaquemacielcunha.model.urna.boletim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TipoVotoBoletimTest {

    @Test
    @DisplayName("Deve converter código numérico para enum de TipoVotoBoletim corretamente")
    void deveConverterCodigoParaEnum() {
        assertEquals(TipoVotoBoletim.NOMINAL, TipoVotoBoletim.fromCodigo(1));
        assertEquals(TipoVotoBoletim.BRANCO, TipoVotoBoletim.fromCodigo(2));
        assertEquals(TipoVotoBoletim.NULO, TipoVotoBoletim.fromCodigo(3));
        assertEquals(TipoVotoBoletim.DESCONHECIDO, TipoVotoBoletim.fromCodigo(99));
        assertEquals(TipoVotoBoletim.DESCONHECIDO, TipoVotoBoletim.fromCodigo(null));
    }

    @Test
    @DisplayName("Deve verificar correspondência do cargo de Presidente")
    void deveVerificarCargoPresidente() {
        assertTrue(CargoConstitucionalTse.PRESIDENTE.coincideCom(1));
        assertFalse(CargoConstitucionalTse.PRESIDENTE.coincideCom(2));
        assertFalse(CargoConstitucionalTse.PRESIDENTE.coincideCom(null));
    }
}
