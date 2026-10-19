package com.github.izaquemacielcunha.dependencyinjection;

import java.net.http.HttpClient;

import jakarta.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.service.EleicaoPipelineService;
import com.github.izaquemacielcunha.service.ExternalService;
import com.github.izaquemacielcunha.service.csv.CsvBoletimUrnaService;
import com.github.izaquemacielcunha.service.csv.CsvCartoExportadorService;
import com.github.izaquemacielcunha.service.csv.CsvEnriquecedorService;
import com.github.izaquemacielcunha.service.csv.CsvMescladorBoletimService;
import com.github.izaquemacielcunha.service.csv.CsvService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaDownloadService;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.csv.CsvMapper;

import io.github.resilience4j.ratelimiter.RateLimiter;

@Singleton
@Component(modules = {NetworkModule.class, MapperModule.class})
public interface ApplicationComponent {

    HttpClient httpClient();
    RateLimiter rateLimiter();
    ObjectMapper objectMapper();
    CsvMapper csvMapper();
    ExternalService externalService();
    EleicaoConfig eleicaoConfig();
    TseUrlBuilder tseUrlBuilder();

    CsvService csvService();
    CsvEnriquecedorService csvEnriquecedorService();
    BoletimUrnaDownloadService boletimUrnaDownloadService();
    BoletimUrnaService boletimUrnaService();
    CsvBoletimUrnaService csvBoletimUrnaService();
    CsvMescladorBoletimService csvMescladorBoletimService();
    CsvCartoExportadorService csvCartoExportadorService();

    EleicaoPipelineService pipelineService();

    @Component.Factory
    interface Factory {
        ApplicationComponent create(@BindsInstance EleicaoConfig config);
    }

}
