# Guia de Desenvolvimento para Agentes de IA

Este documento orienta agentes autônomos e desenvolvedores nas manutenções, melhorias e extensões do projeto **coleta-votos**.

---

## 1. Visão Geral do Projeto

O **coleta-votos** é uma ferramenta de ETL em linha de comando projetada para extrair, decodificar, enriquecer e agregar dados eleitorais do Tribunal Superior Eleitoral (TSE). 

O pipeline divide-se em 6 fases sequenciais orquestradas pelo `EleicaoPipelineService`:
1. **Configuração municipal:** Consulta metadados de seções e zonas do município via API oficial do TSE (`TseUrlBuilder`) e gera o CSV inicial.
2. **Enriquecimento:** Consulta metadados específicos para cada seção, identificando os hashes dos arquivos de urna e tratando seções agregadas/inoperantes.
3. **Download dos Boletins de Urna (BU):** Realiza o download concorrente com taxa controlada (`RateLimiter`) dos arquivos binários de urna.
4. **Decodificação ASN.1 e totalização:** Processa a estrutura ASN.1 dos arquivos `.bu` (usando `asn1bean`) e totaliza votos válidos por candidato.
5. **Mesclagem:** Combina dados territoriais enriquecidos com a apuração de votos.
6. **Geolocalização para Carto:** Cruza os boletins agregados com dados geoespaciais (latitude, longitude, bairro e local de votação) para gerar o dataset pronto para Carto.com.

---

## 2. Stack Técnica e Dependências

- **Linguagem:** Java 25 (utiliza recursos modernos de preview/release mais recente, como `void main(String[] args)` e Java Records).
- **Gerenciador de Dependências e Build:** Apache Maven.
- **Injeção de Dependências:** Google Dagger 2 (`com.google.dagger:dagger:2.59.1`) configurado com *annotation processor* no `maven-compiler-plugin`.
- **Manipulação de Dados / Formatos:**
  - `asn1bean` (1.16.0) para decodificação binária de estruturas eleitorais do TSE.
  - `jackson-dataformat-csv` e `jackson-databind` (versão **3.2.3 / tools.jackson**).
- **Resiliência de Rede:** Resilience4j RateLimiter para respeitar os limites de requisições à infraestrutura do TSE.
- **Testes:** JUnit Jupiter 5.11.4 e Mockito 5.14.2.

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

---

## 4. Regras Arquiteturais e Padrões de Código

### Injeção de Dependência (Dagger) e Configuração CLI

* **Configuração via CLI (`EleicaoConfig`):** O aplicativo recebe parâmetros de linha de comando (`UF` e `CODIGO_MUNICIPIO`) parseados e validados no record `EleicaoConfig.fromArgs(args)`.
* **Injeção no Componente:** O `EleicaoConfig` é injetado no grafo Dagger através de `@Component.Factory` e `@BindsInstance` no `ApplicationComponent`. Qualquer novo parâmetro deve passar por essa fábrica.
* **Caminhos Dinâmicos:** Centralize a resolução de diretórios de trabalho (`temp-files/`, diretório de boletins e nomes de CSVs gerados parametrizados pela UF) no `EleicaoConfig`. Nunca espalhe caminhos fixos (hardcoded) nas classes de serviço.
* **Orquestrador Central:** O `App.java` deve ser mínimo (apenas instanciar a configuração, obter o componente Dagger e chamar `pipelineService.executar()`). A coordenação das fases pertence ao `EleicaoPipelineService`.

### Modelagem de Dados e Tipagem

* **Uso estrito de Records:** Todos os modelos de transferência de dados, modelos de linhas CSV (`LinhaBoletimUrna`, `LinhaMescladaMunicipioBoletim`, etc.) e entidades imutáveis **devem** ser declarados como `record`.
* **Enums de Domínio:** Valores de domínio com significado fixo do TSE (como tipos de voto `TipoVotoBoletim` e cargos `CargoConstitucionalTse`) devem usar `enum` tipado com suporte a `switch` expressions modernas, evitando *magic numbers* ou strings literais espalhadas.
* **Modelos ASN.1:** As classes dentro do pacote `model.tse.asn1` representam especificações estritas do protocolo eleitoral do TSE. Não altere tipos primitivos ou identificadores sem consultar a especificação ASN.1 correspondente.

### Manipulação de CSV, I/O e Coordenadas Geográficas

* **Streaming com Jackson 3.x:** Utilize as APIs de streaming do Jackson 3.x (`CsvMapper`, `MappingIterator`, `SequenceWriter`) para lidar com grandes volumes de votos sem esgotar a memória heap (`OutOfMemoryError`).
* **Delimitadores e Charsets:** Os arquivos de coordenadas do TSE usam delimitador `;` e codificação `ISO-8859-1`, enquanto os arquivos intermediários e o dataset final gerado para o Carto usam delimitador `,` e `UTF-8`.
* **Formatação de Latitude e Longitude (KISS):**
  - Para ferramentas GIS e Carto.com, as coordenadas devem estar no formato decimal padrão com ponto (ex: `-25.4759288` e `-49.1950276`).
  - O dado bruto do TSE vem com vírgula (`-25,4759288`), portanto um simples `replace(",", ".")` é suficiente.
  - **Atenção ao efeito de visualização local (Excel/Locale pt-BR):** Softwares de planilha locais podem interpretar o ponto `.` como separador de milhar, dando a impressão equivocada de que o CSV possui números mal formatados (como `-2.543.908.512`). A saída do CSV não deve ser modificada por causa de particularidades de formatação regional de softwares de terceiros.
* **Fechamento de Recursos:** Sempre feche fluxos de arquivos utilizando declarações `try-with-resources`.

### Resiliência, Tráfego Externo e Concorrência

* **Controle de Concorrência vs. RateLimiter:**
  - O Resilience4j RateLimiter do projeto está configurado com limites rígidos de taxa e timeout de fila de espera (ex: 5 segundos).
  - **Cuidado com Virtual Threads:** Nunca dispare milhares de Virtual Threads desreguladas (`Thread.ofVirtual()` irrestrito) para tarefas protegidas por RateLimiter, pois a fila de espera será sobrecarregada, gerando `RequestNotPermitted`.
  - Mantenha um paralelismo controlado e previsível (como `parallelStream()` ou controle explícito via semáforos) compatível com o throughput permitido.
* **Tolerância a HTTP 404 (Urnas Agregadas / Inoperantes):** O `ExternalService` deve tratar HTTP 404 como `Optional.empty()` (e nunca estourar exceção que aborte o processo). O serviço enriquecedor marca o boletim como `"inoperante/agregada"` e o pipeline segue com as demais seções.

---

## 5. Regras para o Agente de IA

1. **Sem quebra de compatibilidade com Jackson 3.x:** Observe que este projeto usa o pacote `tools.jackson` (versão 3.x), e **não** o tradicional `com.fasterxml.jackson`. Mantenha as importações consistentes.
2. **Zero Novas Dependências:** Não adicione novas dependências ao `pom.xml` a menos que haja solicitação explícita do usuário. Utilize a biblioteca padrão do Java 25 e as ferramentas já instaladas.
3. **Fluxo Passo a Passo com Validação:**
   - Execute uma etapa por vez.
   - Valide sempre com `mvn clean compile && mvn test` ao término de cada modificação.
   - Peça permissão ao usuário antes de avançar para a próxima fase.
4. **Foco no Código e Princípio KISS:**
   - Não complique soluções com algoritmos desnecessários quando uma manipulação direta resolver o problema.
   - Evite inspecionar arquivos de dados pesados (como CSVs gigantes do TSE) via comandos de shell quando o foco da tarefa for a regra de negócio ou lógica de código.
5. **Preservação de integridade de dados eleitorais:** As chaves compostas para junção de coordenadas (`UF-MUNICIPIO-ZONA-SECAO`) exigem normalização numérica (eliminação de zeros à esquerda em zonas/seções ao comparar). Nunca altere essa lógica sem criar testes de regressão.
6. **Não poluir a raiz:** Arquivos CSV intermediários ou de saída gerados durante a execução não devem ser comitados no controle de versão Git.
