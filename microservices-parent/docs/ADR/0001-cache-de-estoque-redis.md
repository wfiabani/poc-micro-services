# ADR-0001: Cache de estoque do fornecedor com Redis

**Status**: Aceito
**Data**: 2026-07-10
**Serviço**: inventory-service

## Contexto

O `inventory-service` escala automaticamente (1 a 3 réplicas). O cache
antigo de consultas ao fornecedor vivia na memória de cada pod. Isso é um
problema: cada réplica tem seu próprio cache, então boa parte das consultas
não aproveita o que outra réplica já buscou, e tudo se perde quando um pod
é reiniciado ou removido.

## Decisão

Usar Redis como cache compartilhado entre todas as réplicas, rodando
dentro do próprio cluster (mesmo padrão já usado para Jaeger, Loki,
Prometheus e Grafana).

- As informações ficam guardadas por 5 minutos e depois expiram.
- O Redis tem limite de memória e descarta os dados mais antigos quando
  esse limite é atingido.
- Se o Redis cair ou ficar lento, o serviço simplesmente ignora o cache e
  busca a informação direto no fornecedor — nada quebra.

Optamos por não usar um cache local combinado com o Redis por enquanto: o
volume atual não justifica essa complexidade extra. Pode ser revisto se o
tráfego crescer bastante.

## Consequências

**Positivo**
- Todas as réplicas compartilham o mesmo cache, aproveitando melhor as
  buscas já feitas.
- O cache não se perde quando um pod escala ou reinicia.

**Negativo**
- Consultar o cache passa a depender da rede (um pouco mais lento que
  memória local, mas ainda bem mais rápido que buscar no fornecedor).
- O Redis roda como uma única instância, então é um ponto único de falha
  do cache. Aceitável no estágio atual do projeto; em produção valeria a
  pena torná-lo redundante.
