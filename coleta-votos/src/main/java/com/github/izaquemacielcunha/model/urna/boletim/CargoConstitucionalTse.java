package com.github.izaquemacielcunha.model.urna.boletim;

public enum CargoConstitucionalTse {
    PRESIDENTE(1);

    private final int codigo;

    CargoConstitucionalTse(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public boolean coincideCom(Integer codigoCargo) {
        return codigoCargo != null && this.codigo == codigoCargo;
    }
}
