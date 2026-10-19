/* Capital individual */
SELECT
    nm_municipio,
    nm_local_votacao,
    nm_bairro,
    ANY_VALUE(geom) AS geom,
    SUM(qtd_votos) AS total_votos,
    SUM(CASE WHEN candidato = '22' THEN qtd_votos ELSE 0 END) AS votos_22,
    SUM(CASE WHEN candidato = '13' THEN qtd_votos ELSE 0 END) AS votos_13,
    SUM(CASE WHEN candidato = '14' THEN qtd_votos ELSE 0 END) AS votos_14,
    SUM(CASE WHEN candidato = '70' THEN qtd_votos ELSE 0 END) AS votos_70,
    SUM(CASE WHEN candidato = '55' THEN qtd_votos ELSE 0 END) AS votos_55,
    SUM(CASE WHEN candidato = 'BRANCO' THEN qtd_votos ELSE 0 END) AS votos_branco,
    SUM(CASE WHEN candidato = 'NULO' THEN qtd_votos ELSE 0 END) AS votos_nulo,
    SUM(CASE WHEN candidato = '30' THEN qtd_votos ELSE 0 END) AS votos_30,
    SUM(CASE WHEN candidato = '80' THEN qtd_votos ELSE 0 END) AS votos_80,
    SUM(CASE WHEN candidato = '16' THEN qtd_votos ELSE 0 END) AS votos_16,
    SUM(CASE WHEN candidato = '27' THEN qtd_votos ELSE 0 END) AS votos_27,
    SUM(CASE WHEN candidato = '29' THEN qtd_votos ELSE 0 END) AS votos_29,
    SUM(CASE WHEN candidato = '21' THEN qtd_votos ELSE 0 END) AS votos_21,
    SUM(CASE WHEN candidato = '35' THEN qtd_votos ELSE 0 END) AS votos_35,
    SUM(CASE WHEN candidato NOT IN ('22', '13', '14', '70', '55', 'BRANCO', 'NULO', '30', '80', '16', '27', '29', '21', '35') THEN qtd_votos ELSE 0 END) AS outros_votos
FROM `carto-dw-ac-gy7fq9vc.private_izaquemaciel_c23c5cd3.carto-dataset-final-eleic-presi-2026-1-turn`
GROUP BY
    nm_municipio,
    nm_local_votacao,
    nm_bairro

/* Juntar capitais */
WITH uniao_capitais AS (
  SELECT
    nm_municipio,
    nm_local_votacao,
    nm_bairro,
    candidato,
    qtd_votos,
    geom
  FROM `carto-dw-ac-gy7fq9vc.private_izaquemaciel_c23c5cd3.carto-dataset-final-eleic-presi-2026-1-turn-sp`

  UNION ALL

  SELECT
    nm_municipio,
    nm_local_votacao,
    nm_bairro,
    candidato,
    qtd_votos,
    geom
  FROM `carto-dw-ac-gy7fq9vc.private_izaquemaciel_c23c5cd3.carto-dataset-final-eleic-presi-2026-1-turn-cwb`
)
SELECT
    nm_municipio,
    nm_local_votacao,
    nm_bairro,
    ANY_VALUE(geom) AS geom,
    SUM(qtd_votos) AS total_votos,
    SUM(CASE WHEN candidato = '22' THEN qtd_votos ELSE 0 END) AS votos_22,
    SUM(CASE WHEN candidato = '13' THEN qtd_votos ELSE 0 END) AS votos_13,
    SUM(CASE WHEN candidato = '14' THEN qtd_votos ELSE 0 END) AS votos_14,
    SUM(CASE WHEN candidato = '70' THEN qtd_votos ELSE 0 END) AS votos_70,
    SUM(CASE WHEN candidato = '55' THEN qtd_votos ELSE 0 END) AS votos_55,
    SUM(CASE WHEN candidato = 'BRANCO' THEN qtd_votos ELSE 0 END) AS votos_branco,
    SUM(CASE WHEN candidato = 'NULO' THEN qtd_votos ELSE 0 END) AS votos_nulo,
    SUM(CASE WHEN candidato = '30' THEN qtd_votos ELSE 0 END) AS votos_30,
    SUM(CASE WHEN candidato = '80' THEN qtd_votos ELSE 0 END) AS votos_80,
    SUM(CASE WHEN candidato = '16' THEN qtd_votos ELSE 0 END) AS votos_16,
    SUM(CASE WHEN candidato = '27' THEN qtd_votos ELSE 0 END) AS votos_27,
    SUM(CASE WHEN candidato = '29' THEN qtd_votos ELSE 0 END) AS votos_29,
    SUM(CASE WHEN candidato = '21' THEN qtd_votos ELSE 0 END) AS votos_21,
    SUM(CASE WHEN candidato = '35' THEN qtd_votos ELSE 0 END) AS votos_35,
    SUM(CASE WHEN candidato NOT IN ('22', '13', '14', '70', '55', 'BRANCO', 'NULO', '30', '80', '16', '27', '29', '21', '35') THEN qtd_votos ELSE 0 END) AS outros_votos
FROM uniao_capitais
GROUP BY
    nm_municipio,
    nm_local_votacao,
    nm_bairro

/* Juntar capitais com VENCEDOR */
WITH uniao_capitais AS (
  SELECT
    nm_municipio,
    nm_local_votacao,
    nm_bairro,
    candidato,
    qtd_votos,
    geom
  FROM `carto-dw-ac-gy7fq9vc.private_izaquemaciel_c23c5cd3.carto-dataset-final-eleic-presi-2026-1-turn-sp`

  UNION ALL

  SELECT
    nm_municipio,
    nm_local_votacao,
    nm_bairro,
    candidato,
    qtd_votos,
    geom
  FROM `carto-dw-ac-gy7fq9vc.private_izaquemaciel_c23c5cd3.carto-dataset-final-eleic-presi-2026-1-turn-cwb`
)
SELECT
    nm_municipio,
    nm_local_votacao,
    nm_bairro,
    ANY_VALUE(geom) AS geom,
    SUM(qtd_votos) AS total_votos,
    CASE
        WHEN SUM(CASE WHEN candidato = '22' THEN qtd_votos ELSE 0 END) > SUM(CASE WHEN candidato = '13' THEN qtd_votos ELSE 0 END) THEN 'Flávio (22)'
        WHEN SUM(CASE WHEN candidato = '13' THEN qtd_votos ELSE 0 END) > SUM(CASE WHEN candidato = '22' THEN qtd_votos ELSE 0 END) THEN 'Lula (13)'
        ELSE 'Outros/Empate'
        END AS vencedor,
    SUM(CASE WHEN candidato = '22' THEN qtd_votos ELSE 0 END) AS votos_22,
    SUM(CASE WHEN candidato = '13' THEN qtd_votos ELSE 0 END) AS votos_13,
    SUM(CASE WHEN candidato = '14' THEN qtd_votos ELSE 0 END) AS votos_14,
    SUM(CASE WHEN candidato = '70' THEN qtd_votos ELSE 0 END) AS votos_70,
    SUM(CASE WHEN candidato = '55' THEN qtd_votos ELSE 0 END) AS votos_55,
    SUM(CASE WHEN candidato = 'BRANCO' THEN qtd_votos ELSE 0 END) AS votos_branco,
    SUM(CASE WHEN candidato = 'NULO' THEN qtd_votos ELSE 0 END) AS votos_nulo,
    SUM(CASE WHEN candidato = '30' THEN qtd_votos ELSE 0 END) AS votos_30,
    SUM(CASE WHEN candidato = '80' THEN qtd_votos ELSE 0 END) AS votos_80,
    SUM(CASE WHEN candidato = '16' THEN qtd_votos ELSE 0 END) AS votos_16,
    SUM(CASE WHEN candidato = '27' THEN qtd_votos ELSE 0 END) AS votos_27,
    SUM(CASE WHEN candidato = '29' THEN qtd_votos ELSE 0 END) AS votos_29,
    SUM(CASE WHEN candidato = '21' THEN qtd_votos ELSE 0 END) AS votos_21,
    SUM(CASE WHEN candidato = '35' THEN qtd_votos ELSE 0 END) AS votos_35,
    SUM(CASE WHEN candidato NOT IN ('22', '13', '14', '70', '55', 'BRANCO', 'NULO', '30', '80', '16', '27', '29', '21', '35') THEN qtd_votos ELSE 0 END) AS outros_votos
FROM uniao_capitais
GROUP BY
    nm_municipio,
    nm_local_votacao,
    nm_bairro

/* INCONSISTÊNCIA DATASET DE SP
FUNDAÇÃO CASA - COMPLEXO BRÁS
FUNDAÇÃO CASA - COMPLEXO BELÉM
UI-UNIDADE DE INTERNAÇÃO ITAQUERA - FUNDAÇÃO CASA
CDP III CENTRO DE DETENÇÃO PROVISÓRIA DE PINHEIROS III
FUNDAÇÃO CASA - OURO PRETO E JOÃO DO PULO
FUNDAÇÃO CASA - NOVA VIDA
PENITENCIÁRIA FEMININA SANT'ANA
PRESÍDIO DA POLÍCIA MILITAR ROMÃO GOMES
CDP DE VILA INDEPENDÊNCIA
EMEI PROFESSORA ISABEL COLOMBO
INTERNATO PIRITUBA
EMEI CARLOS DE LAET
EMEF. CHÁCARA TURÍSTICA
*/

/* INCONSISTÊNCIA DATASET DE RS
INST. DE ED. INFANTIL ANJO DAS FLORES (SEÇÕES ESC. OSCAR SCHMITT)
UFCSPA (SEÇÕES MAL. FLORIANO PEIXOTO)
JOÃO PAULO, COLÉGIO
*/

/* INCONSISTÊNCIA DATASET DE BA
ESCOLA MUNICIPAL OSCAR DA PENHA
*/

/*INCONSISTÊNCIA DATASET DE PE
AEROPORTO INTERNACIONAL DOS GUARARAPES - VOTO EM TRÂNSITO
*/

/*INCONSISTÊNCIA DATASET DE PB
CRECHE FABIANA OLIVEIRA LUCENA
*/

/*INCONSISTÊNCIA DATASET DE CE
EMTI JOSE LEOPOLDINO DA SILVA FILHO
CENTRO SOCIOEDUCATIVO CARDEAL ALOÍSIO LORSCHEIDER - CSCAL
CAPS INFANTIL DA SECRETARIA EXECUTIVA REGIONAL V
*/

/*INCONSISTÊNCIA DATASET DE PA
EEEFM RUI BARBOSA
*/

/*INCONSISTÊNCIA DATASET DE AP
IGREJA SANTA CRUZ - ANEXO I
ESCOLA MARIA LOPES DA CONCEIÇÃO
ESCOLA ESTADUAL MARIA MERIAN DOS SANTOS CORDEIRO FERNANDES
LOCAL 2 PRÉDIO ANEXO DA ESCOLA JARDIM FELICIDADE
CARTÓRIO ELEITORAL DA 10ª ZONA
ASSEMBLEIA DE DEUS DE ANÁPOLIS - NOVA CANAÃ
ESCOLA ESTADUAL OLENDINA FERREIRA DO ROSARIO
ESCOLA APAE
ESCOLA ESTADUAL BARÃO DO RIO BRANCO
E.E. SÃO BENEDITO
ESCOLA ESTADUAL ANTONIO JOÃO
ESCOLA ESTADUAL ROSA DOS SANTOS SARGES
*/

/*INCONSISTÊNCIA DATASET DE RR
UERR - UNIVERSIDADE ESTADUAL DE RORAIMA - UNIDADE II
UNIP - UNIVERSIDADE PAULISTA
ESCOLA MUNICIPAL MARIA FRANCISCA DA SILVA LEMOS
ESCOLA MUNICIPAL JAMES MACELLARO THOMÉ
ESCOLA MUNICIPAL ANTONIO AIRTON DE OLIVEIRA DIAS
ESCOLA INDÍGENA VOVÓ ANTÔNIA CELESTINA DA SILVA
*/

/*INCONSISTÊNCIA DATASET DE MT
CDL - CÂMARA DE DIRIGENTES LOJISTAS DE CUIABÁ - MT
CMEI REGINA PIA PADILLA DE BOURBON NEVES
*/