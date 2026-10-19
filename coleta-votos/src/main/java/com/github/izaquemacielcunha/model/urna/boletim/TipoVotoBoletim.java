package com.github.izaquemacielcunha.model.urna.boletim;

public enum TipoVotoBoletim {
    NOMINAL(1),
    BRANCO(2),
    NULO(3),
    DESCONHECIDO(-1);

    private final int codigo;

    TipoVotoBoletim(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public static TipoVotoBoletim fromCodigo(Integer codigo) {
        if (codigo == null) {
            return DESCONHECIDO;
        }
        for (TipoVotoBoletim tipo : values()) {
            if (tipo.codigo == codigo) {
                return tipo;
            }
        }
        return DESCONHECIDO;
    }
}
