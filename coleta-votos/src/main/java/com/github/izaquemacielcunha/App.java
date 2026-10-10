package com.github.izaquemacielcunha;

import com.github.izaquemacielcunha.dependencyinjection.ApplicationComponent;
import com.github.izaquemacielcunha.dependencyinjection.DaggerApplicationComponent;
import com.github.izaquemacielcunha.service.ExternalServiceImpl;
import com.github.izaquemacielcunha.service.downloader.BoletimUrnaService;
import tools.jackson.dataformat.csv.CsvMapper;

public class App {

    static void main(String[] args) throws Exception {
        ApplicationComponent component = DaggerApplicationComponent.create();

        ExternalServiceImpl externalService = new ExternalServiceImpl(component.httpClient(), component.rateLimiter(), component.objectMapper());

        //Fase 1 - Pegar infos municipios e salvar em csv ✅
//        String url = "https://resultados.tse.jus.br/oficial/ele2026/arquivo-urna/3220/config/pr/pr-p003220-cs.json";
//        String codMun = "75353";
//        Optional<Municipio> resultado = externalService.callMunicipioConfig(url, codMun);
//        Abrangencia abrangencia = new Abrangencia("PR", resultado.get());
//        IO.print(abrangencia);
//        new CsvService().gerarCsv(List.of(abrangencia));

        //Fase 2 - Enriquecer csv salvo ✅
//        CsvEnriquecedorService csvEnriquecedorService = new CsvEnriquecedorService(externalService, new CsvMapper());
//        csvEnriquecedorService.enriquecer();

        //Fase 3 - Baixar boletins das urnas ✅
//        BoletimUrnaService boletimUrnaDownloader = new BoletimUrnaService(externalService, new CsvMapper());
//        boletimUrnaDownloader.download();

        //Fase 4 - Script python contabilizar votos

        //Fase 5  - Merge votos com csv enriquecido

    }

}// end of class