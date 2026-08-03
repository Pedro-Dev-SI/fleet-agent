# Evolução do backend de locadora com Spring Modulith e LangChain4j

Este documento é um guia de evolução do projeto. A proposta não é apenas listar tarefas, mas explicar o motivo de cada decisão, a ordem recomendada e como verificar se cada etapa realmente funciona.

O projeto representa o backend de uma locadora de veículos online. A inteligência artificial funciona como um canal de atendimento: conversa com o cliente, consulta políticas por RAG e chama ferramentas Java para cadastrar clientes, calcular cotações, consultar veículos e criar reservas.

A IA não deve ser responsável pelas regras de negócio. Ela interpreta a conversa e chama casos de uso do backend. Quem decide se um veículo pode ser reservado, quanto custa a locação ou se um cliente existe são os módulos de negócio.

## 1. O que já existe

Antes de planejar o próximo passo, é importante reconhecer o que já foi construído.

O projeto atualmente possui:

- Spring Boot 3.5 e Java 25;
- LangChain4j com Ollama e Gemini opcional;
- memória de conversa identificada por `sessionId`;
- guardrails de entrada e saída;
- tools para cotação, clientes, veículos e reservas;
- RAG com documentos Markdown;
- embeddings persistidos em PostgreSQL com pgvector;
- versionamento dos documentos ingeridos;
- módulos `ai`, `customer`, `knowledge` e `rental`;
- limites de módulos verificados pelo Spring Modulith;
- diagramas PlantUML gerados pelo `DocumentationTest`;
- Flyway para evolução do banco;
- testes unitários das principais regras existentes.

Portanto, a primeira trilha de aprendizado de LangChain4j já foi superada. A próxima fase deve transformar o exemplo de IA em um backend de locadora mais completo e arquiteturalmente convincente.

## 2. Visão da arquitetura

O fluxo principal deve continuar assim:

```text
Cliente
  -> API HTTP do assistente
  -> módulo ai
  -> tool do LangChain4j
  -> API pública do módulo de negócio
  -> serviço de aplicação
  -> domínio e banco de dados
```

Exemplo de criação de reserva:

```text
Mensagem do cliente
  -> AssistantAiService
  -> ReservationTools
  -> ReservationUseCase
  -> ReservationService
  -> valida cliente
  -> valida veículo e período
  -> salva a reserva
  -> publica ReservationCreatedEvent
  -> notification recebe o evento
```

Observe que a IA é apenas uma entrada. No futuro, um controller REST tradicional poderá chamar o mesmo `ReservationUseCase` sem duplicar a regra de negócio.

### 2.1 Responsabilidade dos módulos

```text
ai
  Atendimento, memória, guardrails, RAG e adaptação das tools.

customer
  Cadastro, consulta e regras do cliente.

rental
  Categorias, veículos, cotação, disponibilidade e reservas.

knowledge
  Ingestão, versionamento e recuperação da base de conhecimento.

notification (próximo módulo)
  Reação a eventos e confirmação de operações ao cliente.
```

### 2.2 Regra para escolher comunicação síncrona ou evento

Use uma chamada síncrona quando o resultado for necessário para concluir a operação atual.

Exemplos:

- verificar se o cliente existe;
- verificar se o veículo está disponível;
- calcular o preço;
- salvar a reserva;
- impedir duas reservas incompatíveis.

Use evento quando a operação principal já puder ser considerada concluída e outros módulos precisarem reagir.

Exemplos:

- enviar confirmação;
- registrar auditoria;
- atualizar métricas;
- iniciar uma tarefa secundária de IA;
- gerar um relatório.

A regra prática é: se a falha da ação secundária não deve desfazer a reserva, essa ação é uma boa candidata a listener de evento.

## 3. Por que não usar Kafka agora

Kafka resolve comunicação distribuída, retenção de grandes fluxos de mensagens e integração entre processos independentes. Este projeto é um monólito modular executado como uma única aplicação.

Adicionar Kafka agora traria:

- broker adicional;
- serialização e versionamento de mensagens;
- configuração de consumidores;
- tratamento de duplicidade e retentativas distribuídas;
- mais infraestrutura para testar e demonstrar.

Nada disso é necessário para ensinar desacoplamento entre os módulos atuais. Comece com `ApplicationEventPublisher` e listeners transacionais do Spring. Se um dia um módulo for extraído para outro serviço, o contrato do evento já ajudará na evolução.

## 4. Como validar a arquitetura antes de continuar

### Passo 1: executar os testes

```bash
./mvnw test
```

Se o Maven estiver usando outro JDK:

```bash
JAVA_HOME=/home/youx/.sdkman/candidates/java/25.0.3-tem ./mvnw test
```

O objetivo não é apenas obter `BUILD SUCCESS`. Você deve saber explicar que testes rápidos não podem depender de Gemini, Ollama ou outro serviço externo.

### Passo 2: verificar os limites dos módulos

O teste `ModularityTest` executa:

```java
ApplicationModules.of(Langchain4jApplication.class).verify();
```

Ele detecta dependências proibidas e ciclos entre os módulos. Execute esse teste sempre que criar uma nova integração modular.

### Passo 3: gerar os diagramas

O `DocumentationTest` gera os arquivos PlantUML:

```bash
./mvnw -Dtest=DocumentationTest test
```

Saída esperada:

```text
target/spring-modulith-docs/
├── components.puml
├── module-ai.puml
├── module-customer.puml
├── module-knowledge.puml
└── module-rental.puml
```

Depois de criar `notification`, execute esse teste novamente. O novo módulo e sua dependência de eventos devem aparecer no diagrama.

---

# Fase 1 — Eventos de reserva dentro do monólito

## 5. Desafio: publicar `ReservationCreatedEvent`

### 5.1 O que você aprenderá

Nesta etapa você aprenderá que um evento descreve algo que já aconteceu. Por isso, prefira um nome no passado: `ReservationCreatedEvent`, e não `CreateReservationEvent`.

O evento não ordena que outro módulo faça algo. Ele informa que uma reserva foi criada e permite que zero ou mais listeners reajam.

### 5.2 O que não deve ir no evento

Não publique:

- a entidade JPA `Reservation`;
- a entidade `Customer`;
- CPF completo;
- objetos mutáveis;
- serviços ou repositories.

Entidades JPA podem estar desconectadas da sessão e expõem detalhes internos do módulo. Dados pessoais também aumentam o risco de vazamento em logs.

Publique um contrato imutável contendo identificadores e informações essenciais:

```java
public record ReservationCreatedEvent(
        UUID reservationId,
        UUID customerId,
        UUID carId,
        LocalDateTime startDate,
        LocalDateTime endDate
) {}
```

### 5.3 Arquivos previstos

```text
src/main/java/com/br/langchain4j/rental/api/event/
├── ReservationCreatedEvent.java
└── package-info.java
```

O `package-info.java` deve expor esse pacote como uma interface nomeada:

```java
@org.springframework.modulith.NamedInterface("events")
package com.br.langchain4j.rental.api.event;
```

Isso cria o contrato `rental::events`. Outros módulos poderão consumir os eventos sem acessar detalhes internos de `rental`.

### 5.4 Preparar a entidade

O evento precisa do identificador da reserva salva. Se `Reservation` ainda não possuir `getId()`, adicione esse método.

Não crie um setter público para o ID. O identificador continua sendo controlado pela persistência.

### 5.5 Publicar no serviço de aplicação

Injete `ApplicationEventPublisher` no `ReservationService`:

```java
private final ApplicationEventPublisher eventPublisher;
```

Publique o evento somente depois de `reservationRepository.save(...)`:

```java
Reservation savedReservation = reservationRepository.save(reservation);

eventPublisher.publishEvent(new ReservationCreatedEvent(
        savedReservation.getId(),
        savedReservation.getCustomerId(),
        savedReservation.getCar().getId(),
        savedReservation.getStartDate(),
        savedReservation.getEndDate()
));
```

Não publique nos caminhos de validação que retornam erro. Uma tentativa recusada não é uma reserva criada.

### 5.6 Por que publicar dentro da transação

`createReservation` já é transacional. O evento é publicado durante essa transação, mas o listener será configurado para agir depois do commit.

O efeito desejado é:

```text
reserva salva + commit realizado
  -> listener executa

rollback da reserva
  -> listener não executa
```

### 5.7 Testes desta etapa

Atualize `ReservationServiceTest` para fornecer um mock de `ApplicationEventPublisher`.

Teste o cenário de sucesso:

- a reserva é salva;
- o carro é marcado como reservado;
- exatamente um `ReservationCreatedEvent` é publicado;
- o evento contém os IDs e datas corretos.

Teste pelo menos um cenário de erro:

- veículo indisponível ou cliente inexistente;
- `reservationRepository.save(...)` não é chamado;
- `eventPublisher.publishEvent(...)` não é chamado.

### 5.8 Critérios de aceite

- [ ] Existe um contrato imutável `ReservationCreatedEvent`.
- [ ] O pacote do evento é exposto como `rental::events`.
- [ ] O evento é publicado apenas depois de salvar a reserva.
- [ ] Nenhuma entidade JPA ou CPF é carregado pelo evento.
- [ ] Testes comprovam publicação no sucesso e ausência no erro.
- [ ] `ModularityTest` continua passando.

### 5.9 Erros comuns

- Publicar o evento antes de salvar e não possuir `reservationId`.
- Enviar a entidade inteira por conveniência.
- Usar evento para consultar o cliente ou decidir disponibilidade.
- Publicar o mesmo evento em mais de um ponto do fluxo.
- Testar apenas se o método terminou sem exceção.

---

## 6. Desafio: criar o módulo `notification`

### 6.1 Objetivo

Agora será criado um consumidor para provar o desacoplamento. O módulo `rental` não deve conhecer `notification`. A direção da dependência é do consumidor para o contrato do produtor:

```text
notification -> rental::events
```

### 6.2 Estrutura sugerida

```text
src/main/java/com/br/langchain4j/notification/
├── application/
│   └── ReservationNotificationListener.java
└── package-info.java
```

Declare a dependência permitida:

```java
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = "rental::events"
)
package com.br.langchain4j.notification;
```

### 6.3 Criar o listener

Na primeira versão, o listener pode apenas registrar uma confirmação controlada:

```java
@Component
class ReservationNotificationListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(ReservationCreatedEvent event) {
        // Registrar confirmação sem dados pessoais.
    }
}
```

Não envie e-mail real nesta etapa. Primeiro prove o fluxo e os limites arquiteturais. Depois você poderá criar uma interface `NotificationGateway` e implementações para e-mail, WhatsApp ou uma simulação local.

### 6.4 Por que usar `AFTER_COMMIT`

Se o banco rejeitar a transação, não existe reserva válida para confirmar. `AFTER_COMMIT` impede o listener de agir quando ocorre rollback.

O cliente não deve perder a reserva somente porque uma confirmação secundária falhou. Entretanto, `@TransactionalEventListener` continua síncrono por padrão: ele protege a ordem transacional, mas o listener ainda ocupa a thread da requisição. Nesta primeira versão isso é aceitável porque o listener apenas registra a confirmação.

Quando houver envio de e-mail ou outra integração lenta, estude `@ApplicationModuleListener` ou execução assíncrona com um executor configurado. Nesse caso, trate falhas, retentativas e idempotência explicitamente.

### 6.5 Testes desta etapa

Crie um teste de integração que:

1. inicia o contexto Spring;
2. cria uma reserva válida;
3. confirma que `ReservationCreatedEvent` foi publicado;
4. confirma que o listener recebeu o evento após a transação;
5. verifica que o módulo não acessa classes internas de `rental`.

O Spring Modulith fornece `@ApplicationModuleTest` e suporte a cenários de eventos. Use-os quando o teste precisar verificar a interação entre módulos. Continue usando Mockito nos testes unitários do serviço.

### 6.6 Critérios de aceite

- [ ] O módulo `notification` depende somente de `rental::events`.
- [ ] O listener executa depois do commit.
- [ ] Um rollback não produz confirmação.
- [ ] A confirmação não recebe CPF nem entidade JPA.
- [ ] O fluxo possui teste de integração.
- [ ] O diagrama PlantUML mostra o novo módulo.

---

## 7. Desafio opcional: tornar eventos internos mais resilientes

A primeira versão usa eventos em memória. Isso é suficiente para aprender e demonstrar um monólito modular, mas existe uma limitação: se o processo terminar entre o commit da reserva e o processamento assíncrono, uma ação secundária pode não ser concluída.

Quando esse risco se tornar importante, estude o registro de publicações de eventos do Spring Modulith com persistência JPA. A ideia é registrar publicações pendentes no mesmo banco e permitir reprocessamento.

Faça isso somente depois de o fluxo simples estar testado. Não adicione resiliência sem antes possuir um caso de falha que justifique a complexidade.

Kafka continua fora do escopo. Persistir publicações internas não transforma o projeto em microserviços.

---

# Fase 2 — Ciclo de vida real da reserva

## 8. Desafio: adicionar status à reserva

Hoje a existência da reserva representa implicitamente seu estado. Um backend real precisa distinguir etapas.

Comece com:

```text
CREATED -> CONFIRMED -> COMPLETED
    |          |
    +----------+-> CANCELLED
```

Crie `ReservationStatus` e adicione o status à entidade. Use uma migration Flyway nova; nunca altere uma migration que já pode ter sido aplicada.

Arquivos previstos:

```text
rental/domain/enums/ReservationStatus.java
rental/domain/Reservation.java
db/migration/V10__add_status_to_reservation.sql
```

Não exponha um setter genérico como `setStatus`. Prefira métodos que expressem comportamento:

```java
reservation.confirm();
reservation.cancel();
reservation.complete();
```

Cada método deve validar a transição. Por exemplo, uma reserva cancelada não pode ser confirmada.

Eventos futuros:

- `ReservationConfirmedEvent`;
- `ReservationCancelledEvent`;
- `ReservationCompletedEvent`.

### Critérios de aceite

- [ ] A reserva possui status persistido.
- [ ] Transições inválidas são rejeitadas pelo domínio.
- [ ] Cada transição relevante publica um evento depois da persistência.
- [ ] Testes unitários cobrem a máquina de estados.
- [ ] Flyway atualiza uma base existente sem apagar dados.

---

## 9. Desafio: disponibilidade por intervalo de datas

Marcar um carro como `RESERVADO` de forma permanente é suficiente para o primeiro exercício, mas não representa uma locadora real. Um carro reservado para agosto ainda pode estar disponível em setembro.

A disponibilidade deve considerar sobreposição de períodos.

Duas reservas se sobrepõem quando:

```text
reservaExistente.inicio < periodoSolicitado.fim
e
reservaExistente.fim > periodoSolicitado.inicio
```

Implemente uma consulta no `ReservationRepository` que procure reservas ativas para o carro e período solicitados. Reservas `CANCELLED` não devem bloquear o veículo.

Altere a tool de disponibilidade para receber:

- categoria ou modelo;
- data de retirada;
- data de devolução.

### Concorrência

Existe um risco importante: duas requisições podem consultar o mesmo carro ao mesmo tempo, ambas enxergarem disponibilidade e criarem reservas conflitantes.

Primeira solução recomendada:

1. abrir transação;
2. bloquear ou reler o veículo com lock apropriado;
3. consultar sobreposição dentro da transação;
4. salvar somente se ainda estiver disponível.

Não tente resolver concorrência apenas com um `if` em memória.

### Critérios de aceite

- [ ] Um carro pode ser reservado em períodos diferentes.
- [ ] Períodos sobrepostos são rejeitados.
- [ ] Reserva cancelada libera o período.
- [ ] Datas inválidas são rejeitadas antes da consulta.
- [ ] Existe teste de concorrência ou integração do conflito.

---

## 10. Desafio: guardar o preço contratado

O valor de uma categoria pode mudar no futuro. Uma reserva antiga não deve mudar de preço quando a tabela atual for atualizada.

Ao criar a reserva, calcule e salve um snapshot:

- quantidade de diárias;
- valor da diária;
- valor do seguro;
- valor total;
- moeda.

Esse snapshot pertence à reserva. O `QuotationService` continua calculando, mas o resultado contratado é persistido.

### Critérios de aceite

- [ ] Alterar a categoria não modifica reservas antigas.
- [ ] A resposta da reserva mostra o valor contratado.
- [ ] O cálculo usa um tipo monetário adequado, como `BigDecimal`.
- [ ] Testes cobrem arredondamento e quantidade de dias.

---

# Fase 3 — API tradicional e IA como canais equivalentes

## 11. Desafio: criar endpoints REST de negócio

O backend não deve depender exclusivamente do modelo de linguagem. Crie controllers tradicionais que chamem os mesmos casos de uso usados pelas tools.

Endpoints iniciais sugeridos:

```text
POST   /api/customers
GET    /api/customers/{id}
GET    /api/vehicles/available?start=...&end=...&category=...
POST   /api/reservations
GET    /api/reservations/{id}
POST   /api/reservations/{id}/cancel
```

Regra de reutilização:

```text
Controller REST -> ReservationUseCase
Tool da IA      -> ReservationUseCase
```

Nunca faça a tool chamar o controller e nunca duplique a regra dentro da tool.

### Critérios de aceite

- [ ] REST e IA utilizam os mesmos casos de uso.
- [ ] Controllers não contêm regra de negócio.
- [ ] Entidades JPA não são retornadas pela API.
- [ ] Validação HTTP usa DTOs e Bean Validation.
- [ ] Erros possuem respostas consistentes.

---

## 12. Desafio: autenticação e autorização

Depois que os endpoints existirem, proteja operações sensíveis.

Perfis iniciais:

- `CUSTOMER`: consulta e cancela as próprias reservas;
- `ATTENDANT`: cria e consulta reservas durante atendimento;
- `ADMIN`: administra frota, categorias e políticas.

Não use o `sessionId` da conversa como identidade autenticada. Ele identifica memória de chat, não prova quem é o usuário.

Use Spring Security com autenticação stateless. Mantenha autorização também nos casos de uso quando ela representar regra de negócio, e não apenas no controller.

### Critérios de aceite

- [ ] Endpoints privados exigem autenticação.
- [ ] Um cliente não acessa reserva de outro cliente.
- [ ] Tools com efeito colateral possuem contexto de identidade confiável.
- [ ] Logs não exibem token, senha ou CPF completo.

---

# Fase 4 — Qualidade, observabilidade e segurança da IA

## 13. Desafio: completar a pirâmide de testes

Organize os testes em três níveis.

### Testes unitários

Sem Spring, banco ou LLM:

- cálculo de cotação;
- transições da reserva;
- validação de períodos;
- guardrails;
- adapters das tools com dependências mockadas.

### Testes de módulo e integração

Com Spring e infraestrutura controlada:

- repositories com PostgreSQL/Testcontainers;
- migrations Flyway;
- publicação e consumo de eventos;
- isolamento dos módulos;
- endpoint HTTP com modelo de IA substituído por fake.

### Testes end-to-end opcionais

Podem usar Ollama ou Gemini, mas devem ser opt-in. O comando padrão `./mvnw test` precisa funcionar sem rede, chave ou modelo local.

### Critérios de aceite

- [ ] `./mvnw test` não chama APIs externas.
- [ ] Testes de modelo real são identificados e opcionais.
- [ ] Eventos possuem testes de sucesso, erro e rollback.
- [ ] Banco real é validado com Testcontainers onde necessário.

---

## 14. Desafio: observabilidade do fluxo completo

Uma resposta de IA pode envolver memória, RAG e várias tools. Sem observabilidade fica difícil explicar por que a resposta foi produzida.

Registre de forma estruturada:

- `sessionId` ou correlation ID;
- duração da chamada ao modelo;
- nome da tool chamada;
- sucesso ou erro da tool;
- fontes recuperadas pelo RAG;
- bloqueios de guardrail;
- tipo do evento publicado e processado;
- tempo total da requisição.

Não registre:

- API keys;
- tokens de autenticação;
- prompt completo por padrão;
- CPF completo;
- conteúdo pessoal desnecessário.

Adicione métricas com Actuator/Micrometer somente depois de definir quais perguntas elas responderão, por exemplo:

- quantas reservas foram concluídas pela IA;
- quantas mensagens foram bloqueadas;
- quantas tools falharam;
- quanto tempo uma resposta demora;
- quantos eventos de notificação falharam.

---

## 15. Desafio: tornar tools com efeito colateral seguras

Criar ou cancelar uma reserva é diferente de consultar uma política. A primeira operação altera estado.

Para tools de escrita:

1. valide todos os parâmetros no backend;
2. não confie em texto produzido pelo modelo;
3. use idempotência para evitar repetição;
4. peça confirmação explícita do usuário antes da operação final;
5. registre auditoria sem dados sensíveis;
6. retorne um resultado estruturado.

O `sessionId` pode ajudar na idempotência da conversa, mas não deve ser a única garantia de negócio. Considere uma chave específica da operação.

### Critérios de aceite

- [ ] Repetir a mesma chamada não cria reservas duplicadas.
- [ ] O usuário confirma os dados antes da criação.
- [ ] Datas, veículo e cliente são revalidados no servidor.
- [ ] A tool retorna erro estruturado e compreensível.

---

# Fase 5 — Preparação para demonstração e LinkedIn

## 16. Desafio: criar uma demonstração reproduzível

Uma boa postagem precisa permitir que outra pessoa entenda o projeto rapidamente.

Prepare um fluxo demonstrável:

1. subir PostgreSQL/pgvector com Docker Compose;
2. iniciar Ollama;
3. iniciar a aplicação;
4. cadastrar ou localizar um cliente pela conversa;
5. consultar política da locadora usando RAG e mostrar a fonte;
6. pedir cotação;
7. consultar disponibilidade por período;
8. confirmar a reserva;
9. mostrar o evento de confirmação no log;
10. consultar a reserva criada;
11. exibir o diagrama dos módulos.

Inclua no README:

- visão geral;
- arquitetura;
- pré-requisitos;
- comandos para execução;
- exemplos de requisição;
- variáveis de ambiente;
- decisões arquiteturais;
- limitações conhecidas.

### História técnica sugerida para a postagem

Explique o projeto nesta ordem:

1. problema: atendimento e locação online de veículos;
2. solução: IA integrada a um backend real, não um chatbot isolado;
3. arquitetura: monólito modular com Spring Modulith;
4. IA: LangChain4j, memória, guardrails, tools e RAG;
5. dados: PostgreSQL, pgvector e Flyway;
6. negócio: clientes, frota, cotação e reserva;
7. eventos: confirmação desacoplada sem Kafka;
8. qualidade: testes, limites de módulos e diagramas executáveis;
9. próximos passos: segurança, disponibilidade concorrente e deploy.

---

# Fase 6 — O que deixar para depois

## 17. Kafka

Considere Kafka apenas se existirem processos independentes, grande volume de eventos, múltiplos consumidores externos ou necessidade real de retenção e replay distribuído.

Não use Kafka apenas para dizer que o projeto é orientado a eventos.

## 18. Microserviços

O tamanho atual não justifica separar deploys, bancos e operações. Mantenha os módulos explícitos. Se um módulo ganhar necessidade operacional independente, seus contratos públicos e eventos facilitarão uma extração futura.

## 19. MCP

MCP pode ser estudado quando ferramentas vierem de sistemas externos ou precisarem ser compartilhadas com outros agentes. As tools locais já atendem o caso atual com menos complexidade.

Antes de expor qualquer tool por MCP, defina:

- autenticação;
- autorização;
- allowlist;
- timeout;
- idempotência;
- auditoria;
- proteção de dados pessoais.

## 20. Frontend completo

Um frontend melhora a apresentação, mas não deve interromper a consolidação das regras centrais. Primeiro torne criação, consulta, cancelamento e disponibilidade confiáveis por API.

---

# Ordem recomendada a partir de agora

Siga esta sequência e conclua os critérios de aceite antes de avançar:

1. Criar `ReservationCreatedEvent`.
2. Testar publicação somente no sucesso.
3. Criar o módulo `notification`.
4. Testar consumo depois do commit.
5. Executar `ModularityTest` e regenerar os diagramas.
6. Adicionar ciclo de vida da reserva.
7. Implementar disponibilidade por intervalo e proteção de concorrência.
8. Persistir o preço contratado.
9. Criar endpoints REST usando os mesmos casos de uso das tools.
10. Adicionar autenticação e autorização.
11. Completar testes de integração e observabilidade.
12. Preparar Docker, README e roteiro de demonstração.

## Próximo passo imediato

O próximo passo é somente o primeiro item da lista:

> Criar e testar `ReservationCreatedEvent` sem implementar e-mail, Kafka ou novos serviços externos.

Quando ele estiver funcionando, execute:

```bash
./mvnw test
./mvnw -Dtest=DocumentationTest test
```

Abra `target/spring-modulith-docs/components.puml` e confirme que a arquitetura continua coerente.

## O que você deve saber explicar ao concluir esta trilha

- por que a IA é um adapter e não o domínio;
- como tools chamam casos de uso Java;
- quando usar chamada síncrona e quando publicar evento;
- por que eventos internos não exigem Kafka;
- como o Spring Modulith protege os limites dos módulos;
- como uma reserva mantém consistência transacional;
- como impedir conflito entre períodos;
- por que preços contratados precisam de snapshot;
- como testar sem depender de uma LLM real;
- como RAG, memória, guardrails e tools trabalham juntos;
- como proteger tools que alteram estado;
- quais sinais justificariam uma futura extração para microserviços.

O objetivo final não é apenas ter um chatbot que responde perguntas. É demonstrar um backend modular de locadora, com regras reais de negócio, IA integrada de forma segura e uma arquitetura que você consegue defender tecnicamente.
