# AI Rental Operations Platform

## 1. Visao Geral

Este documento define a proposta de evolucao do sistema atual de locadora corporativa com Spring Boot e LangChain4j para uma plataforma backend orientada a eventos.

O projeto continua como um modular monolith, nao como microservices. A aplicacao sera uma unica aplicacao Spring Boot, organizada em modulos de negocio bem definidos. Kafka sera usado como infraestrutura de eventos assincronos para notificacoes, auditoria, integracoes futuras e aprendizado de arquitetura orientada a eventos.

A IA atuara como interface conversacional e orquestradora de intencoes. Ela podera entender pedidos do cliente, consultar a base de conhecimento, chamar tools e conduzir fluxos de atendimento. Mesmo assim, a autoridade final sobre regras, validacoes e estado do sistema sera sempre o backend Spring.

## 2. Objetivo do Projeto

Construir uma plataforma didatica e demonstravel que mostre como usar LLMs dentro de um backend real, com regras de negocio, eventos, persistencia, auditoria, notificacoes e testes.

O projeto deve ser bom o suficiente para gerar conteudo tecnico publico, especialmente posts no LinkedIn, mostrando decisoes de arquitetura e implementacao alem de um chatbot simples.

## 3. Objetivos Tecnicos

- Evoluir o projeto atual sem jogar fora a base existente.
- Organizar o sistema como modular monolith.
- Usar Spring Modulith para reforcar limites entre modulos.
- Usar Kafka para eventos assincronos.
- Implementar notificacoes baseadas em eventos.
- Manter RAG com PostgreSQL e pgvector.
- Usar LangChain4j com tools, guardrails e memoria.
- Auditar interacoes da IA, chamadas de tools, fontes RAG e eventos relevantes.
- Aplicar idempotencia em operacoes criticas.
- Tratar concorrencia na criacao de reservas.
- Criar testes unitarios, testes de integracao e testes assincronos.
- Preparar o projeto para ser explicado de forma didatica.

## 4. Stack Principal

- Java 25
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Actuator
- Spring Modulith
- PostgreSQL 17
- pgvector
- Flyway
- Kafka
- Outbox Pattern
- LangChain4j
- Ollama
- Gemini como provider opcional
- Docker Compose
- Testcontainers
- Awaitility
- WireMock
- Micrometer
- OpenAPI/Swagger

## 5. Decisao Arquitetural Principal

O projeto usara modular monolith com eventos.

Isso significa que:

- todos os modulos vivem no mesmo deploy Spring Boot;
- cada modulo tem responsabilidades claras;
- comunicacao direta entre modulos deve ser controlada;
- eventos de dominio representam fatos importantes do negocio;
- Kafka processa fluxos assincronos e integracoes;
- eventos criticos podem ser persistidos via outbox antes de serem enviados ao Kafka;
- o sistema pode evoluir futuramente para microservices, mas esse nao e o objetivo inicial.

Kafka sera usado mesmo dentro de um modulith porque o objetivo e aprender backend orientado a eventos com um cenario controlado. O foco nao e simular microservices artificiais, mas criar um produto backend mais realista.

## 6. Modulos da Aplicacao

### 6.1 assistant

Responsavel pela camada de IA.

Deve conter:

- controller HTTP do assistente;
- AI Service do LangChain4j;
- configuracao de modelo;
- escolha entre Ollama e Gemini;
- tools expostas para a LLM;
- guardrails de entrada e saida;
- memoria de conversa por sessao;
- interpretacao de intencao;
- integracao com RAG;
- publicacao de eventos de auditoria de IA.

O modulo `assistant` nao deve conter regra de negocio da locadora. Ele deve adaptar conversas para chamadas seguras ao dominio.

### 6.2 rental

Responsavel pelo dominio central da locadora.

Deve conter:

- categorias de veiculo;
- veiculos;
- cotacoes;
- reservas;
- disponibilidade;
- regras de negocio;
- maquina de estados da reserva;
- validacao de datas;
- bloqueio de veiculo;
- idempotencia de criacao de reserva;
- publicacao de eventos de dominio.

O modulo `rental` deve ser a autoridade sobre reservas e disponibilidade. A IA nunca deve decidir sozinha se uma reserva e valida.

### 6.3 customer

Responsavel por clientes.

Deve conter:

- cadastro de cliente;
- busca por CPF/documento;
- validacao de nome, email e telefone;
- vinculo entre cliente e reserva;
- publicacao de evento de cliente criado.

### 6.4 knowledge

Responsavel pela base de conhecimento e RAG.

Deve conter:

- documentos markdown;
- definicao dos documentos conhecidos;
- ingestao;
- chunking;
- geracao de embeddings;
- persistencia em pgvector;
- versionamento dos documentos;
- metadados de fonte;
- health check do RAG;
- criterios para resposta com fonte obrigatoria.

### 6.5 messaging

Responsavel pela integracao com Kafka.

Deve conter:

- configuracao do Kafka;
- producers;
- consumers;
- outbox para eventos criticos;
- serializacao e desserializacao de eventos;
- nomes de topicos;
- retry;
- dead letter topic;
- correlation id;
- event id.

Este modulo deve esconder detalhes de infraestrutura Kafka dos modulos de dominio.

### 6.6 notification

Responsavel por notificacoes assincronas.

Deve conter:

- consumo de eventos relevantes;
- envio de email fake;
- envio de WhatsApp fake;
- templates de mensagem;
- registro de tentativas;
- status de envio;
- tratamento de falhas;
- publicacao de eventos de notificacao enviada ou falha.

No primeiro momento, email e WhatsApp podem ser simulados por logs ou por chamada a um endpoint mockado via WireMock.

### 6.7 audit

Responsavel por rastreabilidade.

Deve conter:

- historico de mensagens do assistente;
- tools chamadas pela IA;
- parametros relevantes das tools;
- fontes RAG usadas;
- eventos publicados;
- eventos consumidos;
- bloqueios de guardrail;
- erros tecnicos;
- correlation id por fluxo;
- session id por conversa.

O objetivo do modulo `audit` e tornar explicavel o que a IA fez e o que o backend executou.

## 7. Fluxos Funcionais

### 7.1 Cotacao

O cliente pergunta o preco de uma locacao.

Fluxo esperado:

1. Cliente envia mensagem ao assistente.
2. IA identifica intencao de cotacao.
3. IA chama tool de cotacao.
4. Backend valida categoria e quantidade de dias.
5. Sistema calcula valor.
6. Evento `QuotationRequested` e publicado.
7. Resposta e devolvida ao cliente.
8. Auditoria registra a tool chamada.

### 7.2 Consulta de Politicas

O cliente pergunta sobre documentos, seguro, combustivel, pagamento ou regras da locadora.

Fluxo esperado:

1. Cliente envia pergunta.
2. RAG busca trechos relevantes no pgvector.
3. IA responde somente com base nas fontes recuperadas.
4. Resposta inclui fonte.
5. Evento `KnowledgeAnswerGenerated` e publicado.
6. Auditoria registra pergunta, resposta, score e documentos usados.

### 7.3 Criacao de Cliente

O cliente decide prosseguir com a locacao.

Fluxo esperado:

1. IA solicita CPF se ele ainda nao estiver disponivel.
2. Tool consulta cliente existente.
3. Se o cliente existir, a IA pede confirmacao para usar o cadastro.
4. Se nao existir, a IA solicita nome, email e telefone.
5. Tool cria o cliente.
6. Evento `CustomerCreated` e publicado.
7. Notification consome o evento e simula envio de confirmacao.
8. Auditoria registra a operacao.

### 7.4 Criacao de Reserva

O cliente escolhe veiculo, data de retirada e data de entrega.

Fluxo esperado:

1. IA coleta CPF, modelo do carro, data de retirada e data de entrega.
2. Backend valida cliente.
3. Backend valida datas.
4. Backend valida disponibilidade.
5. Backend bloqueia o veiculo de forma transacional.
6. Reserva e criada.
7. Evento `ReservationCreated` e publicado.
8. Notification envia confirmacao.
9. Audit registra mensagens, tool chamada e evento gerado.

Para evitar perda de evento em falhas entre commit de banco e publicacao no Kafka, a reserva deve gravar o fato de dominio dentro da mesma transacao e publicar de forma confiavel depois.

### 7.5 Falha de Reserva

Quando a reserva nao puder ser criada.

Fluxo esperado:

1. Backend rejeita a criacao por regra de negocio ou indisponibilidade.
2. Evento `ReservationFailed` e publicado.
3. IA informa o motivo ao cliente.
4. Sistema pode sugerir veiculos alternativos quando fizer sentido.
5. Auditoria registra a falha.

### 7.6 Notificacao Assincrona

Quando um evento relevante acontecer, o modulo de notificacao deve reagir sem bloquear a operacao principal.

Fluxo esperado:

1. Evento de negocio e publicado.
2. Consumer de notificacao recebe o evento.
3. Sistema monta mensagem a partir de template.
4. Canal fake de email ou WhatsApp e acionado.
5. Tentativa e registrada.
6. Evento `NotificationSent` ou `NotificationFailed` e publicado.

## 8. Eventos Kafka

Eventos iniciais do sistema:

- `CustomerCreated`
- `QuotationRequested`
- `KnowledgeAnswerGenerated`
- `ReservationRequested`
- `ReservationCreated`
- `ReservationFailed`
- `VehicleReserved`
- `VehicleUnavailable`
- `NotificationRequested`
- `NotificationSent`
- `NotificationFailed`
- `AiToolCalled`
- `AiGuardrailBlocked`
- `HumanReviewRequested`

Todos os eventos devem conter:

- `eventId`;
- `eventType`;
- `occurredAt`;
- `correlationId`;
- `source`;
- payload especifico do evento.

Quando existir conversa com IA, o evento tambem deve conter `sessionId`.

## 9. Topicos Kafka

Topicos sugeridos:

- `rental.customer.events`
- `rental.reservation.events`
- `rental.quotation.events`
- `rental.notification.events`
- `rental.ai.audit.events`
- `rental.knowledge.events`
- `rental.dlt`

O projeto deve usar nomes explicitos e documentados. A primeira versao pode usar poucos topicos, desde que a separacao por contexto seja mantida.

## 10. Notificacoes

O modulo de notificacoes deve ser assincrono e tolerante a falhas.

Canais simulados:

- email;
- WhatsApp fake.

Notificacoes esperadas:

- cliente criado;
- reserva confirmada;
- falha na reserva;
- lembrete de retirada;
- solicitacao de revisao humana;
- falha tecnica que exige intervencao.

Falha de notificacao nao deve desfazer reserva, cadastro ou cotacao.

## 11. Requisitos Nao Funcionais

### 11.1 Seguranca

- Nao expor API keys no codigo.
- Usar variaveis de ambiente para credenciais.
- Nao permitir que a IA execute acoes fora das tools.
- Validar todas as entradas no backend.
- Impedir que prompt injection altere regras de negocio.
- Guardrails devem bloquear mensagens fora do escopo.

### 11.2 Confiabilidade

- Eventos devem ter `eventId`.
- Operacoes criticas devem ser idempotentes.
- Eventos criticos devem usar outbox ou mecanismo equivalente de publicacao confiavel.
- Consumers Kafka devem tolerar reprocessamento.
- Reservas nao podem duplicar veiculo no mesmo periodo.
- Falhas de notificacao nao devem desfazer reserva.
- O sistema deve lidar com eventos duplicados.

### 11.3 Observabilidade

- Logs estruturados com `sessionId`, `customerId`, `reservationId`, `eventId` e `correlationId`.
- Health checks para banco, Kafka e RAG.
- Metricas de chamadas da IA.
- Metricas de eventos publicados e consumidos.
- Metricas de notificacoes enviadas e falhadas.
- Metricas de bloqueios de guardrails.

### 11.4 Testabilidade

- Testes unitarios de dominio.
- Testes de tools sem chamar LLM real.
- Testes de integracao com PostgreSQL usando Testcontainers.
- Testes de Kafka usando Testcontainers.
- Testes assincronos com Awaitility.
- Testes de guardrails.
- Testes de RAG com base pequena e respostas esperadas.

## 12. Criterios de Aceite

O projeto sera considerado bem-sucedido quando:

- usuario conseguir conversar com a IA e criar uma reserva completa;
- reserva for persistida no PostgreSQL;
- eventos relevantes forem publicados no Kafka;
- modulo de notificacoes consumir eventos e registrar envio;
- RAG responder perguntas citando fontes;
- auditoria mostrar mensagens, tools chamadas, fontes e eventos;
- testes automatizados cobrirem os principais fluxos;
- sistema subir localmente com Docker Compose;
- documentacao explicar as decisoes tecnicas;
- projeto gerar material didatico para posts tecnicos.

## 13. Roadmap Didatico

### Fase 1: Organizacao Modular

- Revisar pacotes atuais.
- Separar responsabilidades.
- Definir contratos entre modulos.
- Introduzir Spring Modulith.
- Validar dependencias entre modulos.

### Fase 2: Eventos de Dominio

- Criar eventos internos.
- Publicar eventos em acoes importantes.
- Registrar eventos em auditoria.
- Definir `eventId` e `correlationId`.
- Definir quais eventos exigem outbox.

### Fase 3: Kafka

- Adicionar Kafka ao Docker Compose.
- Configurar producers e consumers.
- Criar topicos.
- Implementar retries.
- Implementar dead letter topic.
- Testar consumidores com Testcontainers.

### Fase 4: Notificacoes

- Criar modulo `notification`.
- Consumir eventos de cliente e reserva.
- Simular email e WhatsApp.
- Persistir status de envio.
- Publicar eventos de sucesso e falha.

### Fase 5: Reserva Robusta

- Melhorar maquina de estados.
- Garantir idempotencia.
- Tratar concorrencia.
- Evitar dupla reserva.
- Criar testes de integracao.

### Fase 6: IA Auditavel

- Registrar chamadas de tools.
- Registrar fontes RAG.
- Registrar bloqueios de guardrails.
- Criar endpoint de auditoria.
- Correlacionar conversa, evento e reserva.

### Fase 7: Observabilidade

- Adicionar metricas.
- Melhorar health checks.
- Padronizar logs.
- Documentar operacao local.

## 14. Narrativa para LinkedIn

O projeto deve permitir uma narrativa publica clara:

> Construir uma plataforma de locadora corporativa com IA, onde a LLM atende o cliente, chama tools, consulta politicas via RAG, mas todas as decisoes criticas passam pelo backend Spring. As operacoes geram eventos Kafka, notificacoes assincronas e auditoria completa.

Possiveis posts:

- Por que escolhi modular monolith em vez de microservices.
- Como usei Kafka em um modulith sem criar microservices artificiais.
- Como impedir que uma LLM execute regras de negocio indevidas.
- RAG com pgvector e citacao de fontes em uma aplicacao Spring.
- Notificacoes assincronas com Kafka em uma plataforma de locadora.
- Testando Kafka, PostgreSQL e IA sem depender de servicos externos.

## 15. Escopo Fora da Primeira Versao

Para manter o projeto executavel como estudo, a primeira versao nao deve incluir:

- microservices separados;
- deploy em Kubernetes;
- pagamento real;
- envio real de WhatsApp;
- envio real de email;
- autenticacao completa com usuarios e perfis;
- frontend complexo;
- billing;
- multi-tenancy.

Esses pontos podem virar fases futuras depois que o fluxo principal estiver solido.
