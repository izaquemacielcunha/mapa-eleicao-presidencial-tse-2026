package com.github.izaquemacielcunha.model.urna;

/**
 * Tipos de arquivos de boletim de urna disponibilizados pelo TSE.
 */
public enum TipoArquivoUrna {
    BU("bu"),
    BUSA("busa");

    private final String extensao;

    TipoArquivoUrna(String extensao) {
        this.extensao = extensao;
    }

    public String extensao() {
        return extensao;
    }

    public boolean coincideCom(String tipo) {
        return tipo != null && extensao.equalsIgnoreCase(tipo.trim());
    }

    public static boolean isArquivoUrna(String tipo) {
        if (tipo == null) {
            return false;
        }
        String limpo = tipo.trim();
        return BU.extensao.equalsIgnoreCase(limpo) || BUSA.extensao.equalsIgnoreCase(limpo);
    }
}
