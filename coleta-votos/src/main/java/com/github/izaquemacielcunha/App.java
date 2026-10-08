package com.github.izaquemacielcunha;

import java.util.List;
import java.util.Optional;

import com.github.izaquemacielcunha.dependencyinjection.ApplicationComponent;
import com.github.izaquemacielcunha.dependencyinjection.DaggerApplicationComponent;
import com.github.izaquemacielcunha.model.Abrangencia;
import com.github.izaquemacielcunha.model.Municipio;
import com.github.izaquemacielcunha.model.urnas.Metadados;
import com.github.izaquemacielcunha.service.ExternalServiceImpl;
import com.github.izaquemacielcunha.service.csv.CsvService;

public class App {

    static void main(String[] args) throws Exception {
        ApplicationComponent component = DaggerApplicationComponent.create();

        ExternalServiceImpl externalService = new ExternalServiceImpl(component.httpClient(), component.rateLimiter(), component.objectMapper());

        //Fase 1 - pega infos municipios e salvar em csv
//        String url = "https://resultados.tse.jus.br/oficial/ele2026/arquivo-urna/3220/config/pr/pr-p003220-cs.json";
//        String codMun = "75353";
//        Optional<Municipio> resultado = externalService.callMunicipioConfig(url, codMun);
//        Abrangencia abrangencia = new Abrangencia("PR", resultado.get());
//        IO.print(abrangencia);
//        new CsvService().gerarCsv(List.of(abrangencia));

        //FASE 2 - Pegar dados para add na planilha
        String urlZonaSecao ="https://resultados.tse.jus.br/oficial/ele2026/arquivo-urna/3220/dados/pr/74039/0048/0001/p003220-pr-m74039-z0048-s0001-aux.json";
        Metadados dados = externalService.callZonaSecaoInfos(urlZonaSecao).get();
        IO.println(dados.toString());




    }

}// end of class