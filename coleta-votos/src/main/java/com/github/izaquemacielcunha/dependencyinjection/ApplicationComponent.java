package com.github.izaquemacielcunha.dependencyinjection;

import jakarta.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.service.pipeline.EleicaoPipelineService;

@Singleton
@Component(modules = {NetworkModule.class, MapperModule.class, ServiceModule.class})
public interface ApplicationComponent {

    EleicaoPipelineService pipelineService();

    @Component.Factory
    interface Factory {
        ApplicationComponent create(@BindsInstance EleicaoConfig config);
    }

}
