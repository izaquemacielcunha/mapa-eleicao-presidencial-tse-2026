# Guia de Desenvolvimento para Agentes de IA

Este documento orienta agentes autônomos e desenvolvedores nas manutenções, melhorias e extensões do projeto **coleta-votos**.

---

## 1. Visão Geral do Projeto

O **coleta-votos** é uma ferramenta de ETL em linha de comando projetada para extrair, decodificar, enriquecer e agregar dados eleitorais do Tribunal Superior Eleitoral (TSE). 

O pipeline divide-se em 6 fases sequenciais:
1. **Configuração municipal:** Consulta metadados de seções e zonas do município via API oficial do TSE e gera o CSV inicial.
2. **Enriquecimento:** Consulta metadados específicos para cada seção, identificando os hashes dos arquivos de urna.
3. **Download dos Boletins de Urna (BU):** Realiza o download assíncrono e com controle de taxa dos arquivos binários de urna.
4. **Decodificação ASN.1 e totalização:** Processa a estrutura ASN.1 dos arquivos `.bu` (usando `asn1bean`) e totaliza votos válidos por candidato.
5. **Mesclagem:** Combina dados territoriais enriquecidos com a contagem de votos.
6. **Geolocalização para Carto:** Cruza os boletins agregados com os dados geoespaciais (latitude, longitude, bairro e local de votação) para gerar o dataset pronto para Carto.com.

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

## 4. Regras Arquiteturais e Padrões de Código

### Injeção de Dependência (Dagger)

* Respeite o grafo de dependências gerenciado pelo Dagger.
* Ao criar novos serviços de rede ou mappers, prefira injetá-los via `@Inject` no construtor ou prover instâncias singleton através de módulos (`NetworkModule`, `MapperModule`) ligados a `ApplicationComponent`.
* Sempre execute `mvn compile` após alterar módulos ou componentes do Dagger para garantir a geração correta das classes `_Factory` e `DaggerApplicationComponent`.

### Modelagem de Dados

* **Uso estrito de Records:** Todos os modelos de transferência de dados, modelos de linhas CSV (`LinhaBoletimUrna`, `LinhaMescladaMunicipioBoletim`, etc.) e entidades imutáveis **devem** ser declarados como `record`.
* **Modelos ASN.1:** As classes dentro do pacote `model.tse.asn1` representam especificações estritas do protocolo eleitoral do TSE. Não altere tipos primitivos ou identificadores sem consultar a especificação ASN.1 correspondente.

### Manipulação de CSV e I/O

* Utilize as APIs de streaming do Jackson 3.x (`CsvMapper`, `MappingIterator`, `SequenceWriter`) para lidar com grandes volumes de votos sem esgotar a memória heap (`OutOfMemoryError`).
* Atenção aos separadores de coluna: os arquivos de coordenadas do TSE usam delimitador `;` e codificação `ISO-8859-1`, enquanto os arquivos finais gerados para Carto usam delimitador `,` e `UTF-8`.
* Sempre feche fluxos de arquivos utilizando declarações `try-with-resources`.

### Resiliência e Tráfego Externo

* Nunca faça chamadas HTTP diretas sem passar pela camada `ExternalService` ou sem a proteção do `RateLimiter`.
* Mantenha timeouts adequados nas requisições HTTP para evitar que o pipeline fique travado por conexões presas.

---

## 5. Regras para o Agente de IA

1. **Sem quebra de compatibilidade com Jackson 3.x:** Observe que este projeto usa o pacote `tools.jackson` (versão 3.x), e **não** o tradicional `com.fasterxml.jackson`. Mantenha as importações consistentes.
2. **Preservação de integridade de dados eleitorais:** As chaves compostas para junção de coordenadas (`UF-MUNICIPIO-ZONA-SECAO`) exigem normalização numérica (eliminação de zeros à esquerda em zonas/seções ao comparar). Nunca altere essa lógica sem criar testes de regressão.
3. **Não poluir a raiz:** Arquivos CSV intermediários ou de saída gerados durante a execução não devem ser comitados no controle de versão Git.
4. **Alterações incrementais em `App.java`:** Ao modularizar ou refatorar o fluxo de `App.java`, faça-o extraindo responsabilidades para classes especializadas no pacote `service`, mantendo a ordem estrita das 6 fases do pipeline.
5. **Verificação final:** Toda modificação de código deve obrigatoriamente compilar com `mvn clean compile` e passar nos testes com `mvn test` antes da conclusão.
