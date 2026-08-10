# Renomeação do projeto para Fleet Agent

## Contexto e objetivo

O projeto público demonstra uma plataforma de locação de veículos com Spring Boot, Spring Modulith e LangChain4j. O nome atual, `langchain4j`, identifica a biblioteca utilizada, mas não comunica o domínio do produto.

A migração dará ao projeto a identidade `fleet-agent` sem alterar regras de negócio, contratos HTTP ou comportamento funcional. A renomeação local será validada antes da alteração do repositório remoto.

## Escopo

### Incluído

- Renomear as coordenadas Maven do projeto para `io.github.pedrodevsi:fleet-agent`.
- Definir o nome Maven como `Fleet Agent`.
- Migrar o pacote-base de `com.br.langchain4j` para `io.github.pedrodevsi.fleetagent`.
- Renomear a classe principal para `FleetAgentApplication` e ajustar os testes que a referenciam.
- Mover os diretórios Java de produção e teste para refletir o novo pacote-base.
- Renomear a aplicação Spring para `fleet-agent`.
- Renomear os identificadores locais de PostgreSQL e Docker.
- Atualizar o README e referências internas que representem a identidade do projeto.
- Renomear o repositório GitHub de `Pedro-Dev-SI/langchain4j` para `Pedro-Dev-SI/fleet-agent` somente depois da validação local.
- Atualizar o remoto Git local para `git@github.com:Pedro-Dev-SI/fleet-agent.git`.

### Não incluído

- Alterações em regras de negócio, endpoints, payloads ou modelo de dados.
- Alterações nas dependências da biblioteca LangChain4j.
- Substituição de imports externos no namespace `dev.langchain4j`.
- Exclusão ou migração automática do volume Docker antigo.
- Criação do futuro produto privado e genérico de agentes.
- Renomeação física da pasta raiz do checkout local durante esta migração.

## Mapeamento de nomes

| Elemento | Atual | Novo |
| --- | --- | --- |
| Repositório GitHub | `Pedro-Dev-SI/langchain4j` | `Pedro-Dev-SI/fleet-agent` |
| Maven `groupId` | `com.br` | `io.github.pedrodevsi` |
| Maven `artifactId` | `langchain4j` | `fleet-agent` |
| Maven `name` | vazio | `Fleet Agent` |
| Pacote-base | `com.br.langchain4j` | `io.github.pedrodevsi.fleetagent` |
| Classe principal | `Langchain4jApplication` | `FleetAgentApplication` |
| Spring application name | `langchain4j` | `fleet-agent` |
| Banco PostgreSQL | `langchain4j` | `fleet_agent` |
| Usuário PostgreSQL | `langchain4j` | `fleet_agent` |
| Senha local padrão | `langchain4j` | `fleet_agent` |
| Container PostgreSQL | `langchain4j-postgres` | `fleet-agent-postgres` |
| Volume PostgreSQL | `langchain4j-postgres-data` | `fleet-agent-postgres-data` |

## Estratégia de migração

1. Preservar a alteração local já existente do `artifactId` no `pom.xml` e completar as demais coordenadas Maven.
2. Mover as árvores `src/main/java/com/br/langchain4j` e `src/test/java/com/br/langchain4j` para o novo caminho de pacote.
3. Atualizar declarações de pacote, imports internos, classe principal e referências dos testes.
4. Atualizar a identidade Spring, as configurações locais do PostgreSQL e o Docker Compose.
5. Atualizar a documentação de uso e arquitetura, mantendo referências que descrevam a biblioteca LangChain4j.
6. Executar verificações textuais, compilação e testes automatizados.
7. Renomear o repositório remoto somente se as verificações locais passarem.
8. Atualizar o remoto `origin` e confirmar que o repositório renomeado está acessível.

## Compatibilidade e dados locais

A API continuará expondo os mesmos endpoints e formatos de request e response. A mudança de pacote não afeta consumidores HTTP.

O Docker Compose passará a criar um volume chamado `fleet-agent-postgres-data`. O volume anterior, `langchain4j-postgres-data`, permanecerá intacto e não será removido. Como consequência, a aplicação iniciada com o novo Compose utilizará um banco vazio, inicializado pelas migrations do Flyway.

As variáveis de ambiente existentes continuam sendo aceitas. Apenas seus valores padrão locais serão atualizados. Um ambiente que já forneça `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `RAG_VECTOR_DATABASE`, `RAG_VECTOR_USER` ou `RAG_VECTOR_PASSWORD` continuará controlando explicitamente a conexão.

## Tratamento de falhas

- Se compilação ou testes falharem, o repositório GitHub não será renomeado.
- Se restarem referências internas a `com.br.langchain4j` ou `Langchain4jApplication`, elas serão corrigidas antes da alteração remota.
- Se a renomeação no GitHub falhar, o checkout local continuará funcional e o remoto atual não será alterado.
- Se o novo remoto não puder ser confirmado, o `origin` anterior será preservado até que o destino esteja disponível.

## Validação

- Confirmar que não existem declarações ou imports internos com `com.br.langchain4j`.
- Confirmar que `Langchain4jApplication` não é mais referenciada.
- Confirmar que referências a `dev.langchain4j` e aos artefatos oficiais da biblioteca continuam intactas.
- Executar a suíte Maven de testes.
- Verificar o teste de modularidade do Spring Modulith após a migração de pacote.
- Confirmar que os arquivos de configuração e o Docker Compose utilizam os novos identificadores.
- Confirmar que o README apresenta o projeto como Fleet Agent.
- Após a renomeação remota, consultar `Pedro-Dev-SI/fleet-agent` e verificar o endereço configurado em `origin`.

## Critérios de aceite

- Dado o código migrado, quando a suíte Maven for executada, então todos os testes devem passar.
- Dado o novo pacote-base, quando o projeto for compilado, então o Spring deve descobrir os mesmos módulos e beans existentes.
- Dado o novo Docker Compose, quando seus serviços forem iniciados sem variáveis externas, então devem usar banco e volume com a identidade Fleet Agent.
- Dado que o volume antigo pode conter dados, quando a migração for concluída, então esse volume não deve ser excluído ou sobrescrito.
- Dado que LangChain4j permanece como dependência, quando a busca textual for revisada, então referências oficiais à biblioteca devem permanecer válidas.
- Dada a validação local bem-sucedida, quando o repositório remoto for renomeado, então `origin` deve apontar para `git@github.com:Pedro-Dev-SI/fleet-agent.git`.

