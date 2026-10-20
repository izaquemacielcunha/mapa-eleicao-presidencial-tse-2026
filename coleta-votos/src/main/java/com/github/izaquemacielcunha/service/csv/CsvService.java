package com.github.izaquemacielcunha.service.csv;

import tools.jackson.dataformat.csv.CsvMapper;
import com.github.izaquemacielcunha.config.EleicaoConfig;

@Deprecated
public class CsvService extends CsvSecaoServiceImpl {

    public CsvService(CsvMapper mapper) {
        super(mapper);
    }

    public CsvService(CsvMapper mapper, EleicaoConfig config) {
        super(mapper, config);
    }
}
