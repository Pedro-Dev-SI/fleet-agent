# Fleet Agent

Projeto de estudo com Spring Boot e LangChain4j para construir um assistente de locadora corporativa de veículos.

O objetivo do projeto e aprender, de forma incremental, como integrar uma LLM em uma aplicacao Java usando:

- AI Services do LangChain4j.
- Tool calling.
- Memoria de conversa por sessao.
- Guardrails de entrada e saida.
- RAG com documentos `.md`.
- Embeddings com Ollama.
- PostgreSQL com pgvector.
- Versionamento de documentos indexados.
- Health check do RAG.

## Stack

- Java 25
- Spring Boot 3.5
- Maven
- LangChain4j
- Ollama
- Gemini opcional
- PostgreSQL 17 com pgvector
- Flyway
- Spring Data JPA
- Spring Actuator

## Visao Geral

O assistente responde perguntas sobre locacao corporativa de veiculos.

Ele consegue:

- Calcular cotacao usando uma tool Java.
- Responder perguntas sobre politicas da locadora usando RAG.
- Manter memoria por `sessionId`.
- Bloquear perguntas fora do escopo com guardrails.
- Citar fontes usadas na base de conhecimento.
- Persistir embeddings em PostgreSQL com pgvector.

Fluxo simplificado:

```text
Cliente
  -> POST /api/assistant
  -> AssistantAiController
  -> AssistantAiService
  -> LangChain4j
     -> Chat model
     -> Tools
     -> Memory
     -> RAG
  -> resposta
```

## Arquitetura

```text
io.github.pedrodevsi.fleetagent
  ai
    api             Controller e tratamento de erros HTTP
    application     Interface do AI Service
    config          Configuracao do chat model e AiServices
    dto             Request/response da API
    guardrail       Guardrails de entrada e saida
    tools           Tools expostas para a LLM

  rental
    domain          Entidade RentalCategory
    application     Regra de cotacao
    repository      Repository JPA

  knowledge
    application     Orquestracao da ingestion dos documentos
    config          Configuracao de RAG, embedding model e pgvector
    dto             Definicao dos documentos conhecidos
    infra           Repositories tecnicos e health check
    ingestion       Split, embedding e carga inicial
    utils           Hash dos documentos
```

## Requisitos

Instale:

- JDK 25
- Docker e Docker Compose
- Ollama

Verifique o Java:

```bash
java -version
```

O projeto esta configurado com:

```xml
<java.version>25</java.version>
```

## Configurando o Ollama

O projeto usa Ollama por padrao para evitar custo durante os testes.

Baixe o modelo de chat:

```bash
ollama pull llama3.2
```

Baixe o modelo de embedding:

```bash
ollama pull nomic-embed-text
```

Mantenha o Ollama rodando:

```bash
ollama serve
```

Se o Ollama ja estiver rodando como servico, esse comando pode nao ser necessario.

## Subindo o PostgreSQL com pgvector

O compose fica em:

```text
src/main/docker/docker-compose.yml
```

Suba o banco:

```bash
docker compose -f src/main/docker/docker-compose.yml up -d
```

O banco sobe em:

```text
localhost:5433
```

Credenciais padrao:

```text
database: fleet_agent
user: fleet_agent
password: fleet_agent
```

## Configuracao da Aplicacao

As principais propriedades estao em:

```text
src/main/resources/application.yaml
```

Por padrao, o provider e Ollama:

```yaml
app:
  ai:
    provider: ${APP_AI_PROVIDER:ollama}
```

Configuracao do chat model local:

```yaml
ollama:
  base-url: http://localhost:11434
  model: llama3.2
```

Configuracao do embedding model:

```yaml
rag:
  embedding:
    base-url: ${RAG_EMBEDDING_BASE_URL:http://localhost:11434}
    model: ${RAG_EMBEDDING_MODEL:nomic-embed-text}
```

Configuracao do pgvector:

```yaml
rag:
  vector-store:
    host: ${RAG_VECTOR_HOST:localhost}
    port: ${RAG_VECTOR_PORT:5433}
    database: ${RAG_VECTOR_DATABASE:fleet_agent}
    user: ${RAG_VECTOR_USER:fleet_agent}
    password: ${RAG_VECTOR_PASSWORD:fleet_agent}
    table: ${RAG_VECTOR_TABLE:knowledge_embeddings}
    dimension: ${RAG_VECTOR_DIMENSION:768}
```

### Usando Gemini

Gemini e opcional.

Para usar:

```bash
export APP_AI_PROVIDER=gemini
export GEMINI_API_KEY=sua-chave
```

Depois rode a aplicacao normalmente.

Nenhuma chave real deve ser colocada no `application.yaml`.

## Rodando a Aplicacao

Com o PostgreSQL e Ollama rodando:

```bash
./mvnw spring-boot:run
```

Se precisar apontar explicitamente para o Java 25:

```bash
JAVA_HOME=/home/youx/.sdkman/candidates/java/25.0.3-tem ./mvnw spring-boot:run
```

## Endpoint Principal

```text
POST /api/assistant
```

Exemplo:

```bash
curl -X POST http://localhost:8080/api/assistant \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Quais documentos preciso apresentar para alugar um carro?"
  }'
```

Resposta esperada:

```json
{
  "sessionId": "uuid-gerado",
  "answer": "..."
}
```

Para continuar a mesma conversa, envie o `sessionId` retornado:

```bash
curl -X POST http://localhost:8080/api/assistant \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "cole-o-uuid-aqui",
    "message": "E quais formas de pagamento sao aceitas?"
  }'
```

## Exemplos de Perguntas

Cotacao usando tool:

```text
Quanto custa um SUV por 5 dias?
```

RAG com fonte:

```text
Quais documentos preciso apresentar?
```

```text
Quais formas de pagamento sao aceitas?
```

```text
Como funciona o processo de locacao?
```

## Health Check

O projeto usa Spring Actuator.

Endpoint:

```text
GET /actuator/health
```

Exemplo:

```bash
curl http://localhost:8080/actuator/health
```

Existe um health check customizado chamado `rag`.

Ele executa uma busca real no RAG com a consulta:

```text
documentos obrigatorios para locacao
```

Se recuperar chunks da base vetorial, retorna `UP`.

Se o Ollama, pgvector, tabela ou ingestion estiverem com problema, retorna `DOWN`.

## O Que Foi Implementado

### 1. AI Service

O arquivo:

```text
AssistantAiService.java
```

define o comportamento do assistente com `@SystemMessage`, `@UserMessage` e `@MemoryId`.

`@MemoryId` separa a memoria por sessao. Duas conversas com `sessionId` diferente nao compartilham historico.

### 2. Configuracao Manual do AiServices

O arquivo:

```text
AssistantConfig.java
```

monta o AI Service manualmente:

- chat model;
- tools;
- memoria;
- retrieval augmentor;
- guardrails.

Isso deixa claro o que esta sendo conectado no LangChain4j.

### 3. Tool Calling

O arquivo:

```text
AssistantTools.java
```

expoe uma tool:

```text
calculateQuotation
```

Ela chama o `QuotationService`, que contem a regra de negocio da cotacao.

A tool e apenas um adaptador entre a LLM e o dominio Java.

### 4. Regra de Negocio Separada

O calculo da cotacao fica em:

```text
QuotationService.java
```

Ele consulta categorias cadastradas no banco e calcula o valor da locacao.

Isso evita colocar regra de negocio dentro da tool.

### 5. Contrato HTTP com DTOs

Request:

```text
AssistantRequest.java
```

Response:

```text
AssistantResponse.java
```

O request possui:

- `message`
- `sessionId`

Se `sessionId` vier vazio, o controller gera um UUID novo.

### 6. Guardrails

Guardrails ficam em:

```text
RentalScopeInputGuardrail.java
RentalScopeOutputGuardrail.java
```

Eles validam entrada e saida do modelo.

Input guardrail:

- bloqueia mensagens fora do dominio;
- reduz chamadas desnecessarias ao modelo.

Output guardrail:

- evita resposta vazia ou nula.

### 7. RAG

Os documentos ficam em:

```text
src/main/resources/knowledge
```

Documentos atuais:

- `politica-combustivel.md`
- `politica-documentos.md`
- `politica-pagamentos.md`
- `politica-seguros.md`
- `processo-locacao.md`

O fluxo e:

```text
Document
  -> split em chunks
  -> embeddings
  -> pgvector
  -> busca semantica
  -> contexto para IA
```

Chunk e um pedaco menor do documento.

Embedding e a representacao numerica de um texto.

Vector store e o lugar onde esses embeddings ficam salvos.

### 8. Metadata e Citacao de Fonte

Cada documento recebe metadados:

```text
source
title
category
content_hash
```

O `DefaultContentInjector` injeta esses metadados no contexto enviado ao modelo.

O system message instrui o modelo a citar fontes no formato:

```text
Fonte: nome-do-arquivo.md
```

### 9. pgvector

O projeto usa:

```text
PgVectorEmbeddingStore
```

Tabela:

```text
knowledge_embeddings
```

Migration:

```text
V3__create_knowledge_embeddings.sql
```

O campo:

```text
embedding vector(768)
```

guarda o vetor gerado pelo modelo `nomic-embed-text`.

### 10. Versionamento dos Documentos

Para evitar duplicar embeddings ao reiniciar a aplicacao, existe controle por hash.

Tabela:

```text
knowledge_documents
```

Migration:

```text
V4__create_knowledge_documents.sql
```

Fluxo:

```text
le documento
calcula SHA-256 do conteudo
verifica source + content_hash
se ja existe, pula
se mudou, remove embeddings antigos e reindexa
```

Isso evita que a mesma politica seja indexada varias vezes.

### 11. Service de Ingestion

O arquivo:

```text
KnowledgeIngestionService.java
```

coordena:

- carregar documento;
- calcular hash;
- verificar se mudou;
- remover embeddings antigos;
- adicionar metadata;
- ingerir documento;
- atualizar controle em `knowledge_documents`.

Ele usa `@Transactional` para coordenar as operacoes de banco feitas pelos repositories.

### 12. Flyway

Migrations:

```text
V1__create_rental_category.sql
V2__enable_pgvector.sql
V3__create_knowledge_embeddings.sql
V4__create_knowledge_documents.sql
```

Flyway garante que a estrutura do banco seja criada de forma versionada.

## Rodando Testes

```bash
./mvnw test
```

Com Java especifico:

```bash
JAVA_HOME=/home/youx/.sdkman/candidates/java/25.0.3-tem ./mvnw test
```

## Problemas Comuns

### Ollama nao esta rodando

Erro comum:

```text
Connection refused localhost:11434
```

Solucao:

```bash
ollama serve
```

### Modelo de embedding nao encontrado

Solucao:

```bash
ollama pull nomic-embed-text
```

### Erro de dimensao no pgvector

Verifique:

```yaml
rag.vector-store.dimension: 768
```

Essa dimensao precisa bater com o modelo de embedding.

### Health check do RAG retorna DOWN

Possiveis causas:

- Postgres nao subiu.
- Ollama nao esta rodando.
- `nomic-embed-text` nao foi baixado.
- Migrations nao rodaram.
- Base de conhecimento ainda nao foi indexada.
- `minScore` do retriever esta alto demais.

## Comandos Rapidos

```bash
ollama pull llama3.2
ollama pull nomic-embed-text
docker compose -f src/main/docker/docker-compose.yml up -d
./mvnw spring-boot:run
curl http://localhost:8080/actuator/health
```

## Observacao de Seguranca

Nao versionar chaves reais.

Use variaveis de ambiente:

```bash
export GEMINI_API_KEY=sua-chave
```

O arquivo `application.yaml` deve conter apenas referencias como:

```yaml
gemini:
  api-key: ${GEMINI_API_KEY:}
```
