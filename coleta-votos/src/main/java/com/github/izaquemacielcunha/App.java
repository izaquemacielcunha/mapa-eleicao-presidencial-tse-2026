package com.github.izaquemacielcunha;

import java.util.List;
import java.util.Optional;

import com.github.izaquemacielcunha.dependencyinjection.ApplicationComponent;
import com.github.izaquemacielcunha.dependencyinjection.DaggerApplicationComponent;
import com.github.izaquemacielcunha.model.Abrangencia;
import com.github.izaquemacielcunha.model.Municipio;
import com.github.izaquemacielcunha.model.urnas.Metadados;
import com.github.izaquemacielcunha.service.ExternalServiceImpl;
import com.github.izaquemacielcunha.service.csv.CsvEnriquecedorService;
import com.github.izaquemacielcunha.service.csv.CsvService;
import tools.jackson.dataformat.csv.CsvMapper;

public class App {

    static void main(String[] args) throws Exception {
        ApplicationComponent component = DaggerApplicationComponent.create();

        ExternalServiceImpl externalService = new ExternalServiceImpl(component.httpClient(), component.rateLimiter(), component.objectMapper());

        //Fase 1 - pega infos municipios e salvar em csv ✅
//        String url = "https://resultados.tse.jus.br/oficial/ele2026/arquivo-urna/3220/config/pr/pr-p003220-cs.json";
//        String codMun = "75353";
//        Optional<Municipio> resultado = externalService.callMunicipioConfig(url, codMun);
//        Abrangencia abrangencia = new Abrangencia("PR", resultado.get());
//        IO.print(abrangencia);
//        new CsvService().gerarCsv(List.of(abrangencia));

        //Fase 2 - Enriquecer csv salvo ✅
//        CsvEnriquecedorService csvEnriquecedorService = new CsvEnriquecedorService(externalService, new CsvMapper());
//        csvEnriquecedorService.enriquecer();

        //Fase 3 - Baixar boletins das urnas


        //Fase 3
    }

}// end of class