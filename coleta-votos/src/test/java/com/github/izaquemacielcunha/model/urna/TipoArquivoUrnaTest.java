package com.github.izaquemacielcunha.model.urna;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TipoArquivoUrnaTest {

    @Test
    @DisplayName("Deve identificar corretamente extensões BU e BUSA")
    void deveIdentificarArquivosUrna() {
        assertTrue(TipoArquivoUrna.isArquivoUrna("bu"));
        assertTrue(TipoArquivoUrna.isArquivoUrna("BU"));
        assertTrue(TipoArquivoUrna.isArquivoUrna("busa"));
        assertTrue(TipoArquivoUrna.isArquivoUrna("BUSA"));

        assertFalse(TipoArquivoUrna.isArquivoUrna("rdv"));
        assertFalse(TipoArquivoUrna.isArquivoUrna("vsc"));
        assertFalse(TipoArquivoUrna.isArquivoUrna(null));
        assertFalse(TipoArquivoUrna.isArquivoUrna(""));
    }

    @Test
    @DisplayName("Método coincideCom deve comparar sem distinção de caixa")
    void deveCoincidirComCaseInsensitive() {
        assertTrue(TipoArquivoUrna.BU.coincideCom("bu"));
        assertTrue(TipoArquivoUrna.BU.coincideCom("BU"));
        assertTrue(TipoArquivoUrna.BUSA.coincideCom("busa"));
        assertTrue(TipoArquivoUrna.BUSA.coincideCom("BUSA"));
        assertFalse(TipoArquivoUrna.BU.coincideCom("busa"));
    }
}
