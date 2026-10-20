package com.github.izaquemacielcunha;

import org.slf4j.LoggerFactory;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.dependencyinjection.ApplicationComponent;
import com.github.izaquemacielcunha.dependencyinjection.DaggerApplicationComponent;

public class App {

    void main(String[] args) {
        LoggerFactory.getILoggerFactory();
        EleicaoConfig config = EleicaoConfig.fromArgs(args);
        ApplicationComponent component = DaggerApplicationComponent.factory().create(config);
        component.pipelineService().executar();
    }

}// end of class
