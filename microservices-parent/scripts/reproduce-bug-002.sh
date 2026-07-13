#!/usr/bin/env bash
# Reproduz o vazamento de threads do BUG-002 (notification-service).
#
# Dispara requisições concorrentes contra POST /notifications e monitora a
# métrica jvm.threads.live (via /actuator/metrics) ao longo do teste,
# evidenciando o crescimento monotônico do número de threads da JVM — o
# sintoma que caracteriza o vazamento descrito em docs/bug-002.md.
#
# Pré-requisitos:
#   - Pods de pé (kubectl apply -f k8s/), incluindo payment-service e
#     shipping-service (notification-service depende de ambos)
#   - `minikube tunnel` rodando em outro terminal (para os LoadBalancer IPs)
#
# Uso:
#   ./scripts/reproduce-bug-002.sh [TOTAL_REQUISICOES] [CONCORRENCIA]
#
# Exemplos:
#   ./scripts/reproduce-bug-002.sh              # 2000 requisições, concorrência 20
#   ./scripts/reproduce-bug-002.sh 6000 40      # carga maior, atinge o OOM mais rápido

set -uo pipefail

TOTAL_REQUESTS=${1:-2000}
CONCURRENCY=${2:-20}

get_ip() {
  kubectl get svc "$1" -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null
}

echo "Descobrindo IP externo do notification-service..."
NOTIFICATION_IP=$(get_ip notification-service)

if [ -z "$NOTIFICATION_IP" ]; then
  echo "ERRO: sem EXTERNAL-IP para notification-service (minikube tunnel está rodando?)"
  exit 1
fi

NOTIFICATION_URL="http://${NOTIFICATION_IP}:8090"
echo "  notification-service -> $NOTIFICATION_URL"
echo

read_threads() {
  curl -s --max-time 3 "$NOTIFICATION_URL/actuator/metrics/jvm.threads.live" \
    | grep -o '"value":[0-9.]*' | head -1 | cut -d: -f2
}

BASELINE=$(read_threads)
echo "Threads vivas na JVM antes da carga (jvm.threads.live): ${BASELINE:-desconhecido}"
echo

SKUS=("WIDGET-A" "WIDGET-B" "WIDGET-C")
PAYMENT_IDS=(1 2 3)

worker() {
  local n_per_worker=$1
  for ((i = 0; i < n_per_worker; i++)); do
    pid=${PAYMENT_IDS[$((RANDOM % 3))]}
    sku=${SKUS[$((RANDOM % 3))]}
    curl -s -o /dev/null -X POST "$NOTIFICATION_URL/notifications" \
      -H "Content-Type: application/json" \
      -d "{\"paymentId\": \"$pid\", \"sku\": \"$sku\"}"
  done
}

monitor() {
  while true; do
    sleep 5
    echo "  [monitor] jvm.threads.live = $(read_threads)"
  done
}

echo "Disparando $TOTAL_REQUESTS requisições (concorrência $CONCURRENCY) contra POST /notifications..."
echo "Acompanhe também o painel de threads no Grafana (jvm_threads_live_threads)."
echo "Pressione Ctrl+C para interromper antes do fim."
echo

monitor &
MONITOR_PID=$!

PER_WORKER=$((TOTAL_REQUESTS / CONCURRENCY))
for i in $(seq 1 "$CONCURRENCY"); do
  worker "$PER_WORKER" &
done
wait

kill "$MONITOR_PID" 2>/dev/null

echo
FINAL=$(read_threads)
echo "Threads vivas na JVM após a carga: ${FINAL:-desconhecido} (antes: ${BASELINE:-desconhecido})"
echo
echo "Aguarde ~30s sem gerar tráfego e rode de novo:"
echo "  curl -s $NOTIFICATION_URL/actuator/metrics/jvm.threads.live"
echo "Se o número não recuar para perto da linha de base, é sinal de que as"
echo "threads criadas pelos ExecutorServices por requisição nunca foram"
echo "encerradas (thread leak) — confirma o diagnóstico do BUG-002."
echo
echo "Para inspecionar as threads em detalhe (thread dump):"
echo "  kubectl exec -it deployment/notification-service -- jcmd 1 Thread.print | grep -c 'pool-'"
echo
echo "Repita com uma carga maior (ex.: ./scripts/reproduce-bug-002.sh 8000 40)"
echo "para reproduzir o OutOfMemoryError: unable to create native thread."
