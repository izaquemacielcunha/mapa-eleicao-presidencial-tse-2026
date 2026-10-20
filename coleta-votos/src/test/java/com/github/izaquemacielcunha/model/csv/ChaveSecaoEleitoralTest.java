package com.github.izaquemacielcunha.model.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChaveSecaoEleitoralTest {

    @Test
    @DisplayName("Deve normalizar chave composta eliminando zeros à esquerda de zona e seção")
    void deveNormalizarChaveComposta() {
        String chave = ChaveSecaoEleitoral.normalizarChave("PR", "75353", "0001", "0042");
        assertEquals("PR-75353-1-42", chave);
    }

    @Test
    @DisplayName("Deve tratar valores nulos ou vazios de zona e seção como 0")
    void deveTratarValoresVaziosComoZero() {
        String chave = ChaveSecaoEleitoral.normalizarChave("SP", "71072", null, "  ");
        assertEquals("SP-71072-0-0", chave);
    }

    @Test
    @DisplayName("Instâncias de ChaveSecaoEleitoral devem gerar chaves equivalentes")
    void deveGerarChavesEquivalentes() {
        ChaveSecaoEleitoral c1 = new LinhaMescladaMunicipioBoletim("PR", "75353", "CURITIBA", "01", "05", "13", 10, "h", "bu.dat");
        ChaveSecaoEleitoral c2 = new LinhaMunicipioCoordenadas("PR", "75353", "CURITIBA", "1", "5", "CENTRO", "-25.0", "-49.0", "LOCAL");

        assertEquals("PR-75353-1-5", c1.gerarChaveComposta());
        assertEquals("PR-75353-1-5", c2.gerarChaveComposta());
        assertEquals(c1.gerarChaveComposta(), c2.gerarChaveComposta());
    }
}
