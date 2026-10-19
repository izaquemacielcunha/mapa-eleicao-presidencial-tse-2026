package com.github.izaquemacielcunha;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.dependencyinjection.ApplicationComponent;
import com.github.izaquemacielcunha.dependencyinjection.DaggerApplicationComponent;

public class App {

    void main(String[] args) throws Exception {
        EleicaoConfig config = EleicaoConfig.fromArgs(args);
        ApplicationComponent component = DaggerApplicationComponent.factory().create(config);
        component.pipelineService().executar();
    }

}// end of class