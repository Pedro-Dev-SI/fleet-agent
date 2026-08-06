# Bloco 3 — Produção, segurança e observabilidade de soluções com LLM

## Objetivo do bloco

Aprofundar conhecimentos de gerenciamento, testes, avaliação, segurança e observabilidade de soluções com IA, aplicando práticas que permitam medir sua qualidade e preparar o assistente para um ambiente de produção.

Este bloco continua seguindo o modelo de aprendizagem 70–20–10:

- **70% — prática:** evoluir o projeto, instrumentar a aplicação, criar testes, avaliações, proteções e critérios de produção;
- **20% — troca de conhecimento:** apresentar os resultados para uma pessoa com experiência no tema e receber uma revisão técnica;
- **10% — estudo:** utilizar a documentação oficial do LangChain4j e materiais técnicos relacionados.

> Os percentuais representam a forma de aprendizagem e o esforço empregado. Eles não precisam corresponder à quantidade de tarefas cadastradas na Feedz.

## Resultado esperado

Ao concluir o bloco, devo ser capaz de responder com evidências às seguintes perguntas:

1. A aplicação e seus componentes de IA estão disponíveis e saudáveis?
2. Quanto tempo, quantos tokens e quantas chamadas cada atendimento consome?
3. O modelo escolhe as tools corretas e envia argumentos válidos?
4. As respostas do RAG estão fundamentadas nos documentos recuperados?
5. Uma mudança de prompt, modelo ou configuração piorou algum comportamento importante?
6. Tentativas de prompt injection e acessos indevidos são bloqueados?
7. Falhas do modelo, banco de dados ou tools são tratadas sem expor informações internas?
8. Existe um critério objetivo para aprovar ou impedir uma publicação em produção?

---

# Diagnóstico do projeto antes do bloco 3

## O que já existe

- integração com Ollama e Gemini por configuração externa;
- AI Service com prompt de sistema, memória, tools, RAG e guardrails;
- memória isolada por sessão com limite de dez mensagens;
- tools para cotação, veículos, clientes e reservas;
- RAG com documentos Markdown, embeddings, PostgreSQL e pgvector;
- metadados e identificação da fonte recuperada;
- controle de alteração dos documentos por hash SHA-256;
- testes unitários para regras de negócio e guardrails;
- testes de arquitetura com Spring Modulith;
- Spring Boot Actuator com endpoint de saúde;
- indicador de saúde para o RAG;
- logs estruturados em partes do fluxo de ingestão.

## O que ainda precisa evoluir

- não há métricas e traces completos das interações com a LLM;
- somente o endpoint `health` está exposto pelo Actuator;
- não há dashboard, alertas ou objetivos de nível de serviço;
- o consumo de tokens e o uso das tools ainda não são acompanhados;
- os testes atuais não cobrem o AI Service, a memória, as tools e o RAG de ponta a ponta;
- não existe um conjunto de avaliação para detectar regressões de comportamento;
- a proteção contra prompt injection é baseada em uma lista pequena de textos conhecidos;
- erros de argumentos, nomes e execução de tools ainda precisam de tratamento específico e sanitizado;
- a memória é mantida apenas na aplicação e desaparece após uma reinicialização;
- não existem políticas explícitas de retenção e remoção de dados de conversas;
- faltam limites de uso, estratégia de retry, fallback e circuit breaker;
- as dependências do LangChain4j estão em versões diferentes e incluem APIs beta;
- não há um checklist automatizado para liberar uma versão em produção;
- Skills ainda não foram estudadas nem avaliadas no projeto.

---

# Tarefas para cadastrar na Feedz

Todas as tarefas começam como **Não concluído**. A data e o prazo podem ser ajustados conforme a disponibilidade.

## 01 — [10%] Estudar práticas de produção do LangChain4j

### Descrição para a Feedz

Estudar a documentação oficial sobre observabilidade, testes e avaliações, guardrails, tools, memória, RAG, Skills e integração com Spring Boot, identificando os recursos necessários para operar uma solução com IA em produção.

### Passo a passo

1. Estudar a diferença entre observabilidade do `ChatModel`, do AI Service e de um fluxo agentic.
2. Entender quais métricas são fornecidas por Micrometer e quais precisam ser criadas pela aplicação.
3. Estudar estratégias para testes determinísticos e avaliações de respostas não determinísticas.
4. Revisar guardrails de entrada e saída, incluindo proteção contra prompt injection.
5. Estudar tratamento de erros de tools e os riscos de enviar exceções internas para a LLM.
6. Comparar memória por quantidade de mensagens e por quantidade de tokens.
7. Estudar Skills em Tool Mode e os riscos do Shell Mode.
8. Registrar decisões, dúvidas e recursos que serão aplicados no projeto.

### Evidências

- resumo dos conceitos estudados;
- links da documentação consultada;
- lista priorizada de melhorias para o projeto.

### Critério de conclusão

A tarefa estará concluída quando os conceitos forem registrados e relacionados a mudanças concretas que possam ser verificadas no projeto.

---

## 02 — [70%] Instrumentar métricas e traces das chamadas à LLM

### Descrição para a Feedz

Integrar a observabilidade do LangChain4j ao Spring Boot para acompanhar duração, consumo de tokens, resultado e falhas das chamadas aos modelos, correlacionando as interações de um mesmo atendimento.

### Passo a passo

1. Alinhar as versões das dependências do LangChain4j antes de adicionar novos módulos.
2. Avaliar `langchain4j-observation` e `langchain4j-micrometer-metrics`, considerando que essas APIs ainda são experimentais.
3. Registrar o listener de observabilidade no `ChatModel` usando `ObservationRegistry` e `MeterRegistry`.
4. Disponibilizar métricas para coleta, preferencialmente por Prometheus.
5. Gerar traces para visualizar o tempo gasto nas chamadas ao modelo.
6. Adicionar um identificador de correlação que permita relacionar requisição, sessão e operações internas.
7. Diferenciar provedor, modelo, operação e resultado usando tags de baixa cardinalidade.
8. Confirmar que prompts, respostas, CPF, telefone, e-mail e outros dados pessoais não são utilizados como tags de métricas.

### Métricas mínimas

- duração das chamadas ao modelo;
- quantidade de chamadas com sucesso e erro;
- tokens de entrada e saída, quando fornecidos pelo modelo;
- quantidade de chamadas por provedor e modelo;
- timeouts e falhas de comunicação;
- chamadas realizadas por atendimento.

O módulo de observabilidade do LangChain4j segue as convenções de IA generativa do OpenTelemetry e produz, entre outras, as métricas `gen_ai_client_operation_duration` e `gen_ai_client_token_usage`. Como as convenções também são experimentais, dashboards e alertas devem tolerar possíveis mudanças de nome em atualizações futuras.

### Evidências

- endpoint de métricas funcionando;
- trace de um atendimento completo;
- captura ou consulta mostrando duração e tokens;
- teste ou verificação garantindo que informações pessoais não aparecem nas tags.

### Critério de conclusão

A tarefa estará concluída quando for possível acompanhar uma chamada à LLM do início ao fim e identificar modelo, duração, consumo e resultado sem expor dados sensíveis.

---

## 03 — [70%] Criar indicadores, SLOs e dashboard da solução

### Descrição para a Feedz

Definir indicadores técnicos e de qualidade para acompanhar o comportamento da solução com IA, criar um dashboard e estabelecer limites que ajudem a identificar degradações.

### Passo a passo

1. Coletar uma linha de base antes de definir metas definitivas.
2. Separar indicadores técnicos de indicadores de qualidade da resposta.
3. Construir um dashboard com visão geral e possibilidade de comparação por modelo.
4. Definir objetivos de nível de serviço, chamados de SLOs.
5. Criar alertas somente para situações que exijam alguma ação.
6. Documentar como cada indicador deve ser interpretado.

### Indicadores técnicos sugeridos

| Indicador | O que ajuda a responder |
|---|---|
| Disponibilidade | O serviço está respondendo? |
| Latência média, p95 e p99 | Existem atendimentos excessivamente lentos? |
| Taxa de erro | O modelo, o banco ou uma integração está falhando? |
| Tokens por atendimento | O contexto ou a memória cresceram demais? |
| Chamadas à LLM por atendimento | O agente está criando ciclos ou chamadas desnecessárias? |
| Taxa de sucesso das tools | As operações do backend estão sendo executadas corretamente? |
| Taxa de timeout | O tempo limite está adequado? |
| Bloqueios por guardrail | Existem entradas indevidas ou falsos positivos? |

### Indicadores de qualidade sugeridos

| Indicador | O que ajuda a responder |
|---|---|
| Acerto de intenção | O assistente entendeu o que o usuário queria fazer? |
| Seleção correta de tool | A tool esperada foi chamada? |
| Argumentos corretos | A tool recebeu os dados e formatos esperados? |
| Resposta fundamentada | A afirmação está apoiada no contexto do RAG? |
| Citação correta | A fonte citada realmente foi recuperada? |
| Recusa adequada | O assistente recusou solicitações fora do escopo ou inseguras? |
| Taxa de resolução | O atendimento foi concluído sem intervenção manual? |

### Evidências

- dashboard com indicadores técnicos e de qualidade;
- documento com definição, fórmula e origem de cada métrica;
- SLOs iniciais e alertas configurados.

### Critério de conclusão

A tarefa estará concluída quando o dashboard permitir identificar disponibilidade, lentidão, falhas, consumo e perda de qualidade sem depender apenas da leitura de logs.

---

## 04 — [70%] Implementar uma estratégia de testes para a camada de IA

### Descrição para a Feedz

Criar uma estratégia de testes com mocks, testes de integração e cenários de ponta a ponta para validar AI Services, memória, guardrails, tools e RAG sem depender somente de chamadas reais aos modelos.

### Pirâmide sugerida

- **70% de testes unitários:** rápidos, isolados e determinísticos;
- **20% de testes de integração:** PostgreSQL, pgvector, Spring e componentes reais;
- **10% de testes de ponta a ponta:** jornadas completas com um modelo real controlado.

Essa pirâmide pertence à estratégia de testes e não substitui a divisão 70–20–10 do PDI.

### Passo a passo

1. Criar testes unitários com mocks para evitar custo, lentidão e respostas variáveis.
2. Testar cada guardrail isoladamente, incluindo sucesso, falha e mensagem retornada.
3. Testar as tools sem a LLM, validando parâmetros, retorno, erros e efeitos no banco.
4. Testar a memória com sessões diferentes para garantir isolamento.
5. Verificar a remoção das mensagens antigas conforme a política escolhida.
6. Criar testes de integração para PostgreSQL e pgvector com Testcontainers.
7. Criar testes do pipeline de ingestão e busca do RAG com dados conhecidos.
8. Separar testes que exigem Ollama ou outro modelo real por tag ou perfil.
9. Configurar timeout e health check para evitar testes travados.
10. Nunca chamar APIs pagas em testes unitários ou no pipeline padrão de CI.

### Evidências

- suíte dividida entre testes unitários, integração e ponta a ponta;
- relatório de execução dos testes;
- documentação de como executar testes com e sem um modelo real.

### Critério de conclusão

A tarefa estará concluída quando as principais regras puderem ser validadas de forma determinística e os testes externos estiverem isolados do pipeline rápido.

---

## 05 — [70%] Criar um conjunto de avaliações e testes de regressão

### Descrição para a Feedz

Construir um conjunto versionado de perguntas e resultados esperados para avaliar o comportamento do assistente e detectar regressões após mudanças de modelo, prompt, tool, Skill ou configuração.

### Passo a passo

1. Criar um conjunto inicial de casos representativos, chamado de golden dataset.
2. Cobrir dúvidas informativas, cotações, clientes, veículos, reservas, erros e solicitações fora do escopo.
3. Para cada caso, registrar intenção esperada, tool esperada, argumentos importantes e características da resposta.
4. Evitar comparar o texto completo, pois respostas de LLM não são totalmente determinísticas.
5. Validar propriedades como presença de informação, ausência de invenções, fonte correta e operação executada.
6. Executar o mesmo conjunto antes e depois de mudar prompt, modelo, temperatura ou parâmetros do RAG.
7. Salvar modelo, versão do prompt, versão dos documentos e resultado da avaliação.
8. Usar LLM-as-a-Judge apenas como sinal complementar, nunca como única validação.

### Evidências

- golden dataset versionado;
- executor automatizado das avaliações;
- relatório comparando pelo menos duas configurações ou versões;
- lista de regressões encontradas e corrigidas.

### Critério de conclusão

A tarefa estará concluída quando uma alteração puder ser comparada com a versão anterior por critérios objetivos e repetíveis.

---

## 06 — [70%] Avaliar separadamente recuperação e geração do RAG

### Descrição para a Feedz

Medir a qualidade da recuperação dos documentos e da resposta gerada, avaliando se o RAG encontra os trechos corretos, utiliza as informações recuperadas e cita a fonte adequada.

### Passo a passo

1. Criar perguntas com documento e trecho esperado.
2. Avaliar a recuperação sem chamar o modelo de chat.
3. Medir se o trecho relevante aparece entre os primeiros resultados.
4. Comparar valores de `maxResults`, `minScore`, tamanho do trecho e sobreposição.
5. Avaliar a geração usando um conjunto fixo de trechos recuperados.
6. Verificar fidelidade ao contexto, completude e correção da fonte.
7. Incluir perguntas cuja resposta não exista na base.
8. Confirmar que o assistente reconhece ausência de informação em vez de inventar uma resposta.

### Métricas sugeridas

- `Recall@K`: frequência com que o trecho esperado aparece entre os K primeiros;
- `Precision@K`: proporção de trechos recuperados que realmente são relevantes;
- taxa de respostas fundamentadas;
- taxa de citações corretas;
- taxa de respostas indevidas quando não existe conteúdo suficiente;
- latência da geração do embedding e da busca vetorial.

### Evidências

- dataset de perguntas e fontes esperadas;
- comparação entre configurações do retriever;
- relatório separado de recuperação e geração.

### Critério de conclusão

A tarefa estará concluída quando for possível identificar se uma resposta ruim foi causada pela busca, pelos documentos ou pelo modelo de geração.

---

## 07 — [70%] Fortalecer guardrails e proteção contra prompt injection

### Descrição para a Feedz

Evoluir as validações de entrada e saída para detectar tentativas de prompt injection, vazamento de instruções, solicitações fora do escopo e respostas inadequadas, com testes de segurança e acompanhamento de falsos positivos.

### Passo a passo

1. Separar cada guardrail por responsabilidade.
2. Avaliar o `PatternBasedPromptInjectionGuardrail`, baseado em padrões relacionados ao OWASP LLM01.
3. Manter padrões específicos do domínio somente quando forem realmente necessários.
4. Testar sobrescrita de instruções, troca de papel, jailbreak, pedido do system prompt, delimitadores e conteúdo codificado.
5. Testar prompt injection indireto presente em documentos recuperados pelo RAG.
6. Tratar conteúdo recuperado como dado, e não como nova instrução para o agente.
7. Criar validações de saída para informações obrigatórias, fontes e conteúdos proibidos.
8. Avaliar retry ou reprompt somente para falhas de saída que possam ser corrigidas pelo modelo.
9. Medir bloqueios corretos e falsos positivos.
10. Considerar um modelo de moderação se o risco e o público da aplicação justificarem o custo.

### Cuidados

- Guardrails aumentam a proteção, mas não substituem autenticação, autorização e validações do backend.
- Os guardrails do LangChain4j são experimentais e podem mudar entre versões.
- Validações baratas devem ser executadas antes das validações mais lentas ou pagas.

### Evidências

- matriz de ataques testados;
- testes automatizados de entradas permitidas e bloqueadas;
- métricas de bloqueio e falsos positivos;
- registro das limitações conhecidas.

### Critério de conclusão

A tarefa estará concluída quando ataques representativos forem bloqueados sem impedir os principais atendimentos válidos.

---

## 08 — [70%] Proteger execução, autorização e erros das tools

### Descrição para a Feedz

Fortalecer as tools que consultam e alteram dados, garantindo validação de argumentos, autorização, idempotência, tratamento seguro de falhas e rastreabilidade das operações.

### Passo a passo

1. Manter todas as regras de negócio e permissões no backend, fora da LLM.
2. Não considerar CPF ou informação fornecida na conversa como autenticação suficiente.
3. Exigir confirmação explícita antes de operações com efeito, quando aplicável.
4. Validar novamente todos os argumentos recebidos da LLM.
5. Garantir idempotência em operações que possam ser repetidas, como criação ou cancelamento de reserva.
6. Configurar tratamento para nome de tool inexistente.
7. Configurar tratamento para JSON ou argumentos inválidos, permitindo correção controlada quando for seguro.
8. Configurar `ToolExecutionErrorHandler` para não enviar mensagens internas de exceção para o modelo.
9. Não expor stack trace, caminhos, credenciais, SQL, respostas internas ou dados pessoais.
10. Registrar auditoria com tool, resultado, duração e identificador de correlação.
11. Avaliar compensação somente para fluxos com múltiplas operações que realmente precisem desfazer etapas anteriores.
12. Evitar retry automático de uma operação com efeito sem garantia de idempotência.

### Evidências

- testes de argumentos inválidos e acesso indevido;
- teste de repetição da mesma operação;
- resposta sanitizada para falhas internas;
- log de auditoria sem dados sensíveis.

### Critério de conclusão

A tarefa estará concluída quando nenhuma decisão da LLM conseguir ignorar validações, permissões ou consistência do backend.

---

## 09 — [70%] Evoluir memória, autenticação e privacidade

### Descrição para a Feedz

Evoluir o gerenciamento das conversas, separando memória de histórico, controlando tokens, persistência, expiração e acesso aos dados, com atenção à privacidade e à LGPD.

### Passo a passo

1. Diferenciar memória enviada à LLM do histórico completo exibido ao usuário.
2. Comparar `MessageWindowChatMemory` com `TokenWindowChatMemory`.
3. Preferir limite por tokens quando o modelo e o estimador utilizado permitirem.
4. Implementar um `ChatMemoryStore` persistente somente se houver necessidade de continuidade após reinicializações.
5. Definir prazo de retenção e limpeza automática por sessão.
6. Permitir exclusão dos dados quando necessário.
7. Associar a sessão ao usuário autenticado e impedir acesso ao histórico de outra pessoa.
8. Evitar armazenar dados pessoais que não sejam necessários.
9. Aplicar mascaramento em logs, traces e dados usados em avaliações.
10. Documentar finalidade, retenção e acesso aos dados de conversa.

### Evidências

- decisão registrada sobre memória e histórico;
- testes de isolamento entre sessões e usuários;
- política de expiração e exclusão;
- verificação de que dados sensíveis não aparecem na telemetria.

### Critério de conclusão

A tarefa estará concluída quando a memória possuir limite previsível, isolamento, ciclo de vida definido e acesso protegido.

---

## 10 — [70%] Implementar resiliência, controle de custos e governança de versões

### Descrição para a Feedz

Preparar a integração com os modelos para lidar com lentidão, indisponibilidade, limites de uso e mudanças de versão, evitando falhas em cascata e consumo descontrolado.

### Passo a passo

1. Alinhar todas as dependências do LangChain4j por uma única versão compatível.
2. Fixar versões e revisar notas de atualização antes de upgrades, principalmente para APIs beta.
3. Definir timeout de conexão e resposta para cada provedor.
4. Aplicar retry com backoff e jitter somente em falhas transitórias.
5. Não repetir automaticamente tools com efeito sem idempotência.
6. Adicionar circuit breaker para evitar chamadas contínuas a um provedor indisponível.
7. Definir fallback entre provedores ou uma resposta degradada, quando fizer sentido.
8. Implementar rate limit e limite de concorrência por usuário ou aplicação.
9. Definir limite de tokens, tamanho de mensagem, quantidade de chamadas e duração do atendimento.
10. Acompanhar custo estimado do provedor externo e uso de recursos do modelo local.
11. Separar configurações de desenvolvimento, teste e produção.
12. Manter chaves e senhas em variáveis ou gerenciador de segredos.
13. Versionar prompt, modelo, documentos, Skills e parâmetros do RAG junto dos resultados de avaliação.

### Evidências

- dependências alinhadas;
- testes de timeout, retry e indisponibilidade;
- limites de requisições e concorrência;
- relatório de consumo por modelo;
- procedimento documentado de atualização e rollback.

### Critério de conclusão

A tarefa estará concluída quando uma falha do provedor não causar repetição insegura, espera indefinida ou consumo sem limite.

---

## 11 — [70%] Estudar e aplicar Skills de forma segura

### Descrição para a Feedz

Estudar a API de Skills do LangChain4j e criar uma prova de conceito para carregar instruções especializadas sob demanda, reduzindo o tamanho do prompt principal e limitando as tools disponíveis em cada fluxo.

### Conceito

Uma Skill reúne nome, descrição, instruções e recursos opcionais. O modelo inicialmente conhece somente o nome e a descrição. Quando identifica a necessidade, ativa a Skill e recebe suas instruções completas.

Esse mecanismo é diferente de prompt injection. A Skill é uma capacidade confiável e criada pela aplicação; prompt injection é uma tentativa de inserir instruções não autorizadas.

### Passo a passo

1. Confirmar se a versão adotada pelo projeto é compatível com `langchain4j-skills`.
2. Criar uma Skill pequena para um fluxo conhecido, como consulta ou cancelamento de reserva.
3. Definir um `SKILL.md` com nome, descrição, sequência de passos e tratamento de erros.
4. Carregar a Skill pelo classpath ou por uma origem controlada.
5. Utilizar **Tool Mode**, recomendado pela documentação.
6. Disponibilizar somente as tools necessárias para a Skill ativada.
7. Testar quando a Skill deve e não deve ser ativada.
8. Testar se a ordem das operações e as condições de parada são respeitadas.
9. Versionar e revisar o conteúdo da Skill como código.
10. Validar origem e integridade antes de aceitar Skills externas.
11. Não utilizar Shell Mode no ambiente de produção deste projeto.

### Cuidados

- A API de Skills é experimental.
- Skills reduzem o contexto inicial, mas acrescentam novas decisões do modelo que precisam ser observadas e testadas.
- Tool Mode não oferece acesso arbitrário ao sistema de arquivos; o agente continua limitado às tools registradas.
- Shell Mode pode executar comandos no processo da aplicação e é inseguro sem isolamento forte.

### Evidências

- Skill de prova de conceito;
- testes de ativação, não ativação e execução;
- comparação do tamanho do prompt e consumo de tokens;
- decisão documentada sobre adoção ou adiamento do recurso.

### Critério de conclusão

A tarefa estará concluída quando a prova de conceito demonstrar, com testes e métricas, se Skills agregam valor ao projeto sem ampliar o acesso do agente de forma insegura.

---

## 12 — [70%] Criar critérios de liberação e checklist de produção

### Descrição para a Feedz

Definir uma etapa de validação antes de publicar mudanças de modelo, prompt, RAG, tools ou Skills, reunindo critérios técnicos, de qualidade, segurança e operação.

### Passo a passo

1. Automatizar testes unitários, de integração, arquitetura e avaliações essenciais.
2. Definir limites mínimos de qualidade que bloqueiem uma publicação.
3. Validar migrations e compatibilidade do banco.
4. Configurar endpoints de liveness e readiness sem executar chamadas caras à LLM a cada verificação.
5. Confirmar disponibilidade de banco, pgvector, documentos e configurações obrigatórias.
6. Verificar segredos, logs, traces e políticas de retenção.
7. Criar estratégia de rollback para código, prompt, modelo e documentos.
8. Registrar versão do modelo e configuração implantada.
9. Documentar um runbook para indisponibilidade, aumento de erros, lentidão e respostas inadequadas.
10. Realizar uma execução controlada do golden dataset antes da publicação.

### Checklist de liberação

- [ ] dependências alinhadas e sem vulnerabilidades críticas conhecidas;
- [ ] migrations validadas e sem alteração de versões já aplicadas;
- [ ] testes unitários e de integração aprovados;
- [ ] testes de arquitetura aprovados;
- [ ] golden dataset sem regressão acima do limite definido;
- [ ] testes de RAG aprovados;
- [ ] testes de prompt injection e autorização aprovados;
- [ ] logs e traces sem dados sensíveis;
- [ ] timeouts, rate limits e circuit breaker ativos;
- [ ] métricas, dashboard e alertas funcionando;
- [ ] rollback e runbook revisados;
- [ ] responsável técnico identificável para a publicação.

### Evidências

- checklist preenchido;
- pipeline executando as verificações automatizadas;
- runbook de incidentes e procedimento de rollback;
- relatório final de readiness.

### Critério de conclusão

A tarefa estará concluída quando uma publicação puder ser aprovada ou bloqueada por evidências, sem depender apenas de uma demonstração manual do assistente.

---

## 13 — [20%] Apresentar os resultados e coletar uma revisão técnica

### Descrição para a Feedz

Apresentar a evolução de observabilidade, testes, segurança e prontidão para produção a uma pessoa com experiência no tema, coletando feedback e transformando as sugestões em decisões ou próximos passos.

### Passo a passo

1. Demonstrar uma jornada completa do atendimento.
2. Apresentar dashboard, traces, tokens e execução das tools.
3. Mostrar o golden dataset e uma regressão detectada pelos testes.
4. Demonstrar uma tentativa de prompt injection sendo bloqueada.
5. Apresentar os resultados da avaliação do RAG.
6. Explicar a decisão sobre Skills e os limites de segurança adotados.
7. Solicitar feedback sobre métricas, riscos e critérios de produção.
8. Registrar as sugestões recebidas.
9. Classificar cada sugestão como aceita, adiada ou descartada, com justificativa.

### Evidências

- registro da apresentação;
- principais feedbacks recebidos;
- decisões e ações resultantes da conversa.

### Critério de conclusão

A tarefa estará concluída quando o projeto tiver passado por uma revisão externa e os feedbacks tiverem sido transformados em ações ou decisões registradas.

---

# Ordem recomendada de execução

## Fase 1 — Fundamentos e visibilidade

- **Tarefa 01:** estudar práticas de produção.
- **Preparação da tarefa 02:** alinhar as dependências do LangChain4j.
- **Tarefa 02:** instrumentar métricas e traces.
- **Tarefa 03:** criar indicadores e dashboard.

Sem visibilidade, as decisões seguintes serão baseadas apenas em percepção.

## Fase 2 — Qualidade e regressão

- **Tarefa 04:** implementar a estratégia de testes.
- **Tarefa 05:** criar o golden dataset.
- **Tarefa 06:** avaliar separadamente recuperação e geração do RAG.

Essa fase cria uma linha de base antes de alterar segurança, memória ou orquestração.

## Fase 3 — Segurança e controle

- **Tarefa 07:** fortalecer guardrails e proteção contra prompt injection.
- **Tarefa 08:** proteger tools e operações com efeito.
- **Tarefa 09:** evoluir memória, autenticação e privacidade.

## Fase 4 — Resiliência e evolução do agente

- **Tarefa 10:** implementar resiliência, limites e governança de versões.
- **Tarefa 11:** criar a prova de conceito com Skills.
- **Tarefa 12:** criar o checklist de produção.

## Fase 5 — Troca de conhecimento

- **Tarefa 13:** apresentar os resultados e coletar revisão técnica.

---

# Definição de pronto do bloco 3

O bloco poderá ser considerado concluído quando:

- as interações com os modelos possuírem métricas e traces;
- o projeto tiver indicadores técnicos e de qualidade documentados;
- existir uma estratégia de testes determinísticos e de integração;
- mudanças puderem ser comparadas por um golden dataset versionado;
- recuperação e geração do RAG forem avaliadas separadamente;
- prompt injections representativas forem testadas e bloqueadas;
- erros das tools forem sanitizados e operações importantes forem protegidas;
- memória, histórico, retenção e privacidade tiverem decisões explícitas;
- falhas de provedores tiverem timeout, retry controlado e comportamento degradado;
- a adoção de Skills tiver sido avaliada com uma prova de conceito segura;
- existir um checklist objetivo para publicação;
- os resultados tiverem passado por uma revisão técnica externa.

---

# Referências oficiais

- [Observability — LangChain4j](https://docs.langchain4j.dev/tutorials/observability/)
- [Spring Boot Integration — LangChain4j](https://docs.langchain4j.dev/tutorials/spring-boot-integration/)
- [Testing and Evaluation — LangChain4j](https://docs.langchain4j.dev/tutorials/testing-and-evaluation/)
- [Guardrails — LangChain4j](https://docs.langchain4j.dev/tutorials/guardrails/)
- [Tools / Function Calling — LangChain4j](https://docs.langchain4j.dev/tutorials/tools/)
- [Chat Memory — LangChain4j](https://docs.langchain4j.dev/tutorials/chat-memory/)
- [RAG — LangChain4j](https://docs.langchain4j.dev/tutorials/rag/)
- [AI Services — LangChain4j](https://docs.langchain4j.dev/tutorials/ai-services/)
- [Structured Outputs — LangChain4j](https://docs.langchain4j.dev/tutorials/structured-outputs/)
- [Skills — LangChain4j](https://docs.langchain4j.dev/tutorials/skills/)
- [Agents and Agentic AI — LangChain4j](https://docs.langchain4j.dev/tutorials/agents/)

## Observações sobre as referências

- Observabilidade de AI Services, guardrails, Skills e o módulo agentic possuem recursos experimentais.
- Antes de implementar exemplos da documentação, é necessário confirmar se eles existem na versão adotada pelo projeto.
- O projeto deve manter as dependências do LangChain4j alinhadas para reduzir incompatibilidades entre módulos estáveis e beta.
