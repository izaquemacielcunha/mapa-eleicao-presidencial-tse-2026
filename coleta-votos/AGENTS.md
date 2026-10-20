# Guia de Desenvolvimento para Agentes de IA

Este documento orienta agentes autônomos e desenvolvedores nas manutenções, melhorias e extensões do projeto **coleta-votos**.

---

## 1. Visão Geral do Projeto

O **coleta-votos** é uma ferramenta de ETL em linha de comando projetada para extrair, decodificar, enriquecer e agregar dados eleitorais do Tribunal Superior Eleitoral (TSE). 

O pipeline divide-se em 6 fases sequenciais orquestradas pelo `EleicaoPipelineService`:
1. **`[Fase 1/6 - Estrutura Seções]` (`gerarEstruturaSecoesMunicipais()`):** Consulta metadados de seções e zonas do município via API oficial do TSE (`TseUrlBuilder`) e gera o CSV inicial.
2. **`[Fase 2/6 - Enriquecimento Metadados]` (`enriquecerMetadadosSecoes()`):** Consulta metadados específicos para cada seção, identificando os hashes dos arquivos de urna e tratando seções agregadas/inoperantes.
3. **`[Fase 3/6 - Download Boletins]` (`baixarBoletinsUrna()`):** Realiza o download concorrente com taxa controlada (`RateLimiter`) dos arquivos binários de urna.
4. **`[Fase 4/6 - Apuração Votos]` (`apurarVotosBoletins()`):** Decodifica a estrutura ASN.1 dos arquivos `.dat` (usando `asn1bean`) e totaliza votos válidos para Presidente.
5. **`[Fase 5/6 - Mesclagem Votos]` (`mesclarVotosComSecoes()`):** Combina dados territoriais enriquecidos com a apuração dos votos.
6. **`[Fase 6/6 - Exportação Carto]` (`exportarDatasetGeorreferenciadoParaCarto()`):** Cruza os boletins agregados com dados geoespaciais (latitude, longitude, bairro e local de votação) para gerar o dataset pronto para Carto.com.

---

## 2. Stack Técnica e Dependências

- **Linguagem:** Java 25 (utiliza recursos modernos de preview/release mais recente, como `void main(String[] args)` e Java Records).
- **Gerenciador de Dependências e Build:** Apache Maven.
- **Injeção de Dependências:** Google Dagger 2 (`com.google.dagger:dagger:2.59.1`) configurado com *annotation processor* no `maven-compiler-plugin`.
- **Manipulação de Dados / Formatos:**
  - `asn1bean` (1.16.0) para decodificação binária de estruturas eleitorais do TSE.
  - `jackson-dataformat-csv` e `jackson-databind` (versão **3.2.3 / tools.jackson**).
- **Logging:** SLF4J 2.x (`org.slf4j:slf4j-api:2.0.16`) com Logback (`ch.qos.logback:logback-classic:1.5.16`).
- **Resiliência de Rede:** Resilience4j RateLimiter para respeitar os limites de requisições à infraestrutura do TSE (com exclusão transitiva do `slf4j-api` legado).
- **Testes:** JUnit Jupiter 5.11.4 e Mockito 5.14.2 (com suporte a Java 25 via ByteBuddy agent).

---

## 3. Comandos Essenciais

Execute essas checagens via terminal antes de dar qualquer tarefa como concluída:

* **Compilar e gerar classes do Dagger:**
```bash
mvn clean compile
```

*(Nota: O Dagger gera arquivos em `target/generated-sources/annotations`. Se classes de fábrica ou componentes estiverem faltando, este comando os regenera).*

* **Executar a suíte de testes:**
```bash
mvn test
```

* **Gerar o Fat JAR executável (`target/coleta-votos.jar`):**
```bash
mvn clean package
```

* **Executar a aplicação via JAR:**
```bash
java -jar target/coleta-votos.jar <UF> <CODIGO_MUNICIPIO> <CAMINHO_ARQUIVO_COORDENADAS>
# Exemplo:
java -jar target/coleta-votos.jar PR 75353 ./dados/eleitorado_local_votacao_2026_PR.csv
```

---

## 4. Regras Arquiteturais e Padrões de Código

### Logging Padronizado (SLF4J + Logback)

* **Declaração do Logger:** Toda classe de serviço que requer log deve utilizar estritamente o padrão da indústria SLF4J:
  ```java
  private static final Logger logger = LoggerFactory.getLogger(MinhaClasse.class);
  ```
* **Formatação e Saída no Console (`logback.xml`):**
  - O appender de console deve direcionar sempre para `<target>System.out</target>`. **Nunca** permita saída para `System.err`, pois IDEs e terminais colorem todo o texto em vermelho.
  - O padrão de formatação deve incluir timestamp completo, nível com cores ANSI (`%highlight([%-5level])`) e nome canônico completo da classe:
    ```xml
    <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight([%-5level]) %logger - %msg%n</pattern>
    ```
* **Inicialização no `App.java` (Prevenção de Replay Warning):**
  - Devido ao carregamento antecipado de classes de serviço estáticas pelo grafo de injeção do Dagger, chame explicitamente `LoggerFactory.getILoggerFactory();` logo na primeira linha do `App.main()` antes de instanciar o componente. Isso impede que o SLF4J emita o aviso de aviso/replay `SLF4J(W): A number of logging calls during initialization phase...`.

### Injeção de Dependência (Dagger) e Contratos de Serviço

* **Padrão Interface + Implementação (`*Service` / `*ServiceImpl`):** Todos os serviços de domínio devem expor uma interface pública desacoplada e uma classe concreta implementadora com sufixo `Impl` anotada com `@Inject`. Consumidores e orquestradores devem depender exclusivamente das interfaces.
* **Vínculo com `@Binds` no `ServiceModule`:** Novos serviços devem ser registrados no módulo abstrato `ServiceModule` via anotação `@Binds` e escopo `@Singleton`. Evite `@Provides` com instanciações manuais para classes com construtor `@Inject`.
* **Configuração via CLI (`EleicaoConfig`):** O aplicativo recebe parâmetros de linha de comando obrigatórios (`UF`, `CODIGO_MUNICIPIO` e `CAMINHO_ARQUIVO_COORDENADAS`) parseados e validados no record `EleicaoConfig.fromArgs(args)`, injetado no grafo via `@BindsInstance` na fábrica de `ApplicationComponent`. Exemplo de uso: `java App PR 75353 ./dados/eleitorado_local_votacao_2026_PR.csv`.
* **Caminhos Dinâmicos e `java.nio.file.Path`:** Centralize a resolução de diretórios de trabalho (`temp-files/`, diretório de boletins e nomes de CSVs gerados parametrizados pela UF) no `EleicaoConfig`. O arquivo de coordenadas do TSE é fornecido explicitamente como parâmetro de entrada, enquanto os arquivos temporários são gerados pelo próprio pipeline. Use sempre `Path` e nunca espalhe caminhos fixos (*hardcoded*) nas classes de serviço.
* **Orquestrador Central:** O `App.java` deve ser mínimo (apenas instanciar a configuração, obter o componente Dagger e chamar `pipelineService.executar()`). A coordenação das fases pertence ao `EleicaoPipelineService`.

### Encapsulamento Rígido e Nomenclatura Expressiva

* **Operador de acesso explícito (`private`):** Métodos auxiliares, subtarefas de decomposição ou rotinas de parsing internas devem ser estritamente declarados como `private`. **Nunca** utilize visibilidade padrão de pacote (`package-private`) ou comentários do tipo `/* package-private */`.
* **Testes unitários via API Pública:** Testes unitários devem validar o comportamento dos serviços através dos métodos públicos definidos nos contratos de interface, e não tentar invocar métodos privados diretamente.
* **Nomenclatura orientada ao domínio:** Nomes de métodos e etapas do pipeline devem descrever claramente a intenção e a ação executada (ex: `gerarEstruturaSecoesMunicipais()`, `exportarVotosParaCsv()`), eliminando numerações ordinais (`executarFase1`, `fase2`) ou nomes genéricos (`process()`).

### Modelagem de Dados e Tipagem

* **Uso estrito de Records:** Todos os modelos de transferência de dados, modelos de linhas CSV (`LinhaBoletimUrna`, `LinhaMescladaMunicipioBoletim`, etc.) e entidades imutáveis **devem** ser declarados como `record`.
* **Chave Composta e Enums de Domínio:**
  - A interface `ChaveSecaoEleitoral` centraliza o cálculo da chave `UF-MUNICIPIO-ZONA-SECAO` com normalização numérica (sem zeros à esquerda).
  - Valores com significado fixo do TSE devem usar enums ou constantes dedicadas (`TipoArquivoUrna`, `StatusSecaoEleitoral`), eliminando strings mágicas.
* **Modelos ASN.1:** As classes dentro do pacote `model.tse.asn1` representam especificações estritas do protocolo eleitoral do TSE. Não altere tipos primitivos ou identificadores sem consultar a especificação ASN.1 correspondente.

### Manipulação de CSV, I/O e Coordenadas Geográficas

* **Streaming com Jackson 3.x:** Utilize as APIs de streaming do Jackson 3.x (`CsvMapper`, `MappingIterator`, `SequenceWriter`) para lidar com grandes volumes de votos sem esgotar a memória heap (`OutOfMemoryError`).
* **Exceções do Jackson 3.x:** No Jackson 3.x, operações de streaming lançam `JacksonException` (unchecked). Empregue o tratamento adequado convertendo para `EleicaoDataProcessingException`.
* **Delimitadores e Charsets:** Os arquivos de coordenadas do TSE usam delimitador `;` e codificação `ISO-8859-1`, enquanto os arquivos intermediários e o dataset final gerado para o Carto usam delimitador `,` e `UTF-8`.
* **Formatação de Latitude e Longitude (KISS):**
  - Para ferramentas GIS e Carto.com, as coordenadas devem estar no formato decimal padrão com ponto (ex: `-25.4759288` e `-49.1950276`).
  - O dado bruto do TSE vem com vírgula (`-25,4759288`), portanto um simples `replace(",", ".")` é suficiente.
* **Fechamento de Recursos:** Sempre feche fluxos de arquivos utilizando declarações `try-with-resources`.

### Resiliência, Tráfego Externo e Concorrência

* **Controle de Concorrência vs. RateLimiter:**
  - O Resilience4j RateLimiter do projeto está configurado com limites rígidos de taxa e timeout de fila de espera.
  - Nunca dispare milhares de Virtual Threads desreguladas (`Thread.ofVirtual()` irrestrito) para tarefas protegidas por RateLimiter, pois a fila de espera será sobrecarregada, gerando `RequestNotPermitted`. Mantenha paralelismo controlado (ex: `parallelStream()`).
* **Tolerância a HTTP 404 (Urnas Agregadas / Inoperantes):** O `ExternalService` deve tratar HTTP 404 como `Optional.empty()` (e nunca estourar exceção que aborte o processo). O serviço enriquecedor marca o boletim como `StatusSecaoEleitoral.STATUS_INOPERANTE_AGREGADA` e o pipeline segue com as demais seções.

---

## 5. Regras para o Agente de IA

1. **Sem quebra de compatibilidade com Jackson 3.x:** Observe que este projeto usa o pacote `tools.jackson` (versão 3.x), e **não** o tradicional `com.fasterxml.jackson`. Mantenha as importações consistentes.
2. **Java 25 e Mockito no Surefire:** O `maven-surefire-plugin` deve sempre conter a configuração de JVM `<argLine>-Dnet.bytebuddy.experimental=true -XX:+EnableDynamicAgentLoading</argLine>`. Não remova este argumento, pois ele é obrigatório para que o Mockito/ByteBuddy funcione no Java 25.
3. **Resolução de Conflitos no POM:** A dependência `resilience4j-ratelimiter` traz transitivamente uma versão legada do `slf4j-api` (1.7.x). Ela deve ser sempre excluída no `pom.xml` para não causar conflitos com o `slf4j-api` 2.x do Logback.
4. **Formatação de URLs no `TseUrlBuilder` em Testes:**
   - O `TseUrlBuilder.buildUrlSecaoMetadados()` formata zonas e seções com 4 dígitos usando `%04d` (ex: a seção `10` vira `s0010` na URL).
   - Ao escrever mocks com Mockito em testes unitários, utilize a própria instância de `tseUrlBuilder.buildUrlSecaoMetadados(linha)` ou atente-se ao formato `s%04d` para não gerar divergências de matcher no Mockito.
5. **Fluxo Passo a Passo com Validação:**
   - Execute uma etapa por vez.
   - Valide sempre com `mvn clean compile && mvn test` ao término de cada modificação.
   - Peça permissão ao usuário antes de avançar para a próxima fase.
6. **Preservação de integridade de dados eleitorais:** As chaves compostas para junção de coordenadas (`UF-MUNICIPIO-ZONA-SECAO`) exigem normalização numérica (eliminação de zeros à esquerda em zonas/seções ao comparar). Nunca altere essa lógica sem criar testes de regressão.
7. **Não poluir a raiz:** Arquivos CSV intermediários ou de saída gerados durante a execução não devem ser comitados no controle de versão Git.
