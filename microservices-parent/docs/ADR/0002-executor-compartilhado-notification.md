# ADR-0002: Executor compartilhado no notification-service

**Status**: Aceito
**Data**: 2026-07-13
**Serviço**: notification-service

## Contexto

`POST /notifications` busca o status do pagamento e do envio em paralelo
para reduzir a latência da resposta. Isso era feito criando um
`ExecutorService` (`Executors.newFixedThreadPool(2)`) dentro do próprio
método do controller, a cada requisição, sem nunca encerrá-lo.

Como as *core threads* de um `ExecutorService` não têm vida útil limitada
por padrão, cada requisição deixava 2 threads permanentemente vivas, mesmo
depois que o pool era descartado. Sob carga sustentada, o número de threads
da JVM (`jvm_threads_live_threads`) crescia de forma monotônica até o
processo falhar com `OutOfMemoryError: unable to create native thread` (ver
`docs/bug-002.md`).

## Decisão

Usar um único `ExecutorService` compartilhado, criado uma vez como bean do
Spring (`NotificationExecutorConfig`), em vez de um pool novo por
requisição.

- O bean é um `newFixedThreadPool(8)`, com `destroyMethod = "shutdown"` para
  que o Spring o encerre de forma ordenada quando o contexto da aplicação
  for finalizado (ex.: `SIGTERM` do Kubernetes).
- O controller passa a receber esse executor via injeção de dependência e
  o reaproveita em todas as requisições, usando `CompletableFuture.supplyAsync`
  no lugar de `executor.submit()` direto.
- O paralelismo entre as chamadas a payment e shipping é mantido; a
  diferença é que agora threads são reaproveitadas, e não criadas e
  abandonadas a cada chamada.

Optamos por manter um `ExecutorService` manual (em vez de migrar para
`WebClient` reativo ou `@Async`/`TaskExecutor` do Spring) para minimizar o
escopo da correção. Migrar para uma solução totalmente gerenciada pelo
ciclo de vida do Spring é uma melhoria possível, mas não necessária para
resolver o vazamento em si.

## Consequências

**Positivo**
- O número de threads do processo passa a ter um teto previsível (8),
  independente do volume de requisições.
- O pool é encerrado corretamente no shutdown do contexto, evitando também
  vazamento nessa hora.

**Negativo**
- Um pool de tamanho fixo (8) limita o paralelismo entre requisições
  concorrentes: sob carga muito alta, requisições podem esperar por uma
  thread livre em vez de ganhar threads dedicadas instantaneamente. Isso é
  aceitável no estágio atual do projeto; o tamanho do pool pode ser
  revisto (ou tornado configurável) se o volume de tráfego justificar.
- O tamanho do pool e a fila de tarefas pendentes ainda não são expostos
  como métricas (`ExecutorServiceMetrics.monitor(...)`), então o
  comportamento do executor sob carga não é diretamente visível em
  Grafana — fica como melhoria futura.
