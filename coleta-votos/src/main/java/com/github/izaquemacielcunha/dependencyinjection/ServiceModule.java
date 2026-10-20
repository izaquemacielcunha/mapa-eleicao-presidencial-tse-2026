package com.github.izaquemacielcunha.dependencyinjection;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;

import com.github.izaquemacielcunha.service.EleicaoPipelineService;
import com.github.izaquemacielcunha.service.EleicaoPipelineServiceImpl;
import com.github.izaquemacielcunha.service.ExternalService;
import com.github.izaquemacielcunha.service.ExternalServiceImpl;
import com.github.izaquemacielcunha.service.LimpezaArquivosTemporariosService;
import com.github.izaquemacielcunha.service.LimpezaArquivosTemporariosServiceImpl;
import com.github.izaquemacielcunha.service.csv.CsvBoletimUrnaService;
import com.github.izaquemacielcunha.service.csv.CsvBoletimUrnaServiceImpl;
import com.github.izaquemacielcunha.service.csv.CsvCartoExportadorService;
import com.github.izaquemacielcunha.service.csv.CsvCartoExportadorServiceImpl;
import com.github.izaquemacielcunha.service.csv.CsvEnriquecedorService;
import com.github.izaquemacielcunha.service.csv.CsvEnriquecedorServiceImpl;
import com.github.izaquemacielcunha.service.csv.CsvMescladorBoletimService;
import com.github.izaquemacielcunha.service.csv.CsvMescladorBoletimServiceImpl;
import com.github.izaquemacielcunha.service.csv.CsvSecaoService;
import com.github.izaquemacielcunha.service.csv.CsvSecaoServiceImpl;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaDownloadService;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaDownloadServiceImpl;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaService;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaServiceImpl;

@Module
public abstract class ServiceModule {

    @Binds
    @Singleton
    public abstract EleicaoPipelineService bindPipelineService(EleicaoPipelineServiceImpl impl);

    @Binds
    @Singleton
    public abstract ExternalService bindExternalService(ExternalServiceImpl impl);

    @Binds
    @Singleton
    public abstract CsvSecaoService bindCsvSecaoService(CsvSecaoServiceImpl impl);

    @Binds
    @Singleton
    public abstract CsvEnriquecedorService bindCsvEnriquecedorService(CsvEnriquecedorServiceImpl impl);

    @Binds
    @Singleton
    public abstract BoletimUrnaDownloadService bindBoletimUrnaDownloadService(BoletimUrnaDownloadServiceImpl impl);

    @Binds
    @Singleton
    public abstract BoletimUrnaService bindBoletimUrnaService(BoletimUrnaServiceImpl impl);

    @Binds
    @Singleton
    public abstract CsvBoletimUrnaService bindCsvBoletimUrnaService(CsvBoletimUrnaServiceImpl impl);

    @Binds
    @Singleton
    public abstract CsvMescladorBoletimService bindCsvMescladorBoletimService(CsvMescladorBoletimServiceImpl impl);

    @Binds
    @Singleton
    public abstract CsvCartoExportadorService bindCsvCartoExportadorService(CsvCartoExportadorServiceImpl impl);

    @Binds
    @Singleton
    public abstract LimpezaArquivosTemporariosService bindLimpezaArquivosTemporariosService(
            LimpezaArquivosTemporariosServiceImpl impl);
}
