package com.github.izaquemacielcunha;

import com.github.izaquemacielcunha.dependencyinjection.ApplicationComponent;
import com.github.izaquemacielcunha.dependencyinjection.DaggerApplicationComponent;
import com.github.izaquemacielcunha.model.Municipio;
import com.github.izaquemacielcunha.service.ExternalServiceImpl;

import java.util.Optional;

public class App {

    static void main(String[] args) throws Exception {
        ApplicationComponent component = DaggerApplicationComponent.create();
        String url = "https://resultados.tse.jus.br/oficial/ele2026/arquivo-urna/3220/config/pr/pr-p003220-cs.json";
        String codMun = "75353";

        ExternalServiceImpl externalService = new ExternalServiceImpl(component.httpClient(), component.rateLimiter(), component.objectMapper());

        Optional<Municipio> resultado = externalService.callService(url, codMun);
        IO.println("Resultado: " + resultado.get().zonas().getFirst().secoes().size());
    }

}// end of class