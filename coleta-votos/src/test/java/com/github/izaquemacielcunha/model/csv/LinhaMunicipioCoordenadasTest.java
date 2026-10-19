package com.github.izaquemacielcunha.model.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinhaMunicipioCoordenadasTest {

    @Test
    @DisplayName("Deve gerar chave composta normalizando zeros à esquerda de zona e seção")
    void deveGerarChaveCompostaNormalizada() {
        LinhaMunicipioCoordenadas linha = new LinhaMunicipioCoordenadas(
                "PR",
                "75353",
                "CURITIBA",
                "0008",
                "0413",
                "CENTRO",
                "-25,4284",
                "-49,2733",
                "COLEGIO ESTADUAL"
        );

        assertEquals("PR-75353-8-413", linha.gerarChaveComposta());
    }

    @Test
    @DisplayName("Deve compatibilizar chave composta com LinhaMescladaMunicipioBoletim")
    void deveSerCompativelComLinhaMesclada() {
        LinhaMunicipioCoordenadas coordenadas = new LinhaMunicipioCoordenadas(
                "PR", "75353", "CURITIBA", "8", "413",
                "CENTRO", "-25.4", "-49.2", "LOCAL"
        );

        LinhaMescladaMunicipioBoletim mesclada = new LinhaMescladaMunicipioBoletim(
                "PR", "75353", "CURITIBA", "008", "00413",
                "CANDIDATO A", 100, "hash123", "bu.dat"
        );

        assertEquals(coordenadas.gerarChaveComposta(), mesclada.gerarChaveComposta());
    }
}
