#!/usr/bin/env bash
# Gera carga sustentada e concorrente contra todas as rotas do sistema,
# cobrindo toda a cadeia de chamadas entre os microserviços.
#
# Pré-requisitos:
#   - Pods de pé (kubectl apply -f k8s/)
#   - `minikube tunnel` rodando em outro terminal (para os LoadBalancer IPs)
#
# Uso:
#   ./scripts/load-test.sh [DURACAO_SEGUNDOS] [CONCORRENCIA_POR_ROTA]
#
# Exemplos:
#   ./scripts/load-test.sh              # 10 min, concorrência 8
#   ./scripts/load-test.sh 1800 16       # 30 min, concorrência 16

set -uo pipefail

DURATION_SECONDS=${1:-600}
CONCURRENCY=${2:-8}

get_ip() {
  kubectl get svc "$1" -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null
}

echo "Descobrindo IPs externos dos serviços..."

ORDER_IP=$(get_ip order-service)
INVENTORY_IP=$(get_ip inventory-service)
SUPPLIER_IP=$(get_ip supplier-service)
PAYMENT_IP=$(get_ip payment-service)
SHIPPING_IP=$(get_ip shipping-service)
NOTIFICATION_IP=$(get_ip notification-service)

missing=0
for pair in "order-service:$ORDER_IP" "inventory-service:$INVENTORY_IP" "supplier-service:$SUPPLIER_IP" \
            "payment-service:$PAYMENT_IP" "shipping-service:$SHIPPING_IP" "notification-service:$NOTIFICATION_IP"; do
  name="${pair%%:*}"; ip="${pair##*:}"
  if [ -z "$ip" ]; then
    echo "  AVISO: sem EXTERNAL-IP para $name (minikube tunnel está rodando?)"
    missing=1
  else
    echo "  $name -> $ip"
  fi
done

if [ "$missing" = "1" ]; then
  echo "Alguns serviços não têm IP externo. As rotas correspondentes serão puladas."
fi

ORDER_URL="http://${ORDER_IP}:8086"
INVENTORY_URL="http://${INVENTORY_IP}:8087"
SUPPLIER_URL="http://${SUPPLIER_IP}:8088"
PAYMENT_URL="http://${PAYMENT_IP}:8089"
SHIPPING_URL="http://${SHIPPING_IP}:8091"
NOTIFICATION_URL="http://${NOTIFICATION_IP}:8090"

SKUS=("WIDGET-A" "WIDGET-B" "WIDGET-C")
PRODUCT_IDS=(1 2 3)

worker() {
  local name="$1"
  local end=$((SECONDS + DURATION_SECONDS))
  local count=0
  while [ "$SECONDS" -lt "$end" ]; do
    sku=${SKUS[$((RANDOM % 3))]}
    pid=${PRODUCT_IDS[$((RANDOM % 3))]}
    case "$name" in
      orders)
        curl -s -o /dev/null -X POST "$ORDER_URL/orders" \
          -H "Content-Type: application/json" -d "{\"productId\": $pid, \"quantity\": 2}"
        curl -s -o /dev/null "$ORDER_URL/orders"
        curl -s -o /dev/null "$ORDER_URL/orders/$pid"
        ;;
      hello)
        curl -s -o /dev/null -X POST "$ORDER_URL/hello-world"
        ;;
      inventory)
        curl -s -o /dev/null "$INVENTORY_URL/inventory/$sku"
        curl -s -o /dev/null "$INVENTORY_URL/inventory"
        ;;
      supplier)
        curl -s -o /dev/null "$SUPPLIER_URL/suppliers/stock/$sku"
        ;;
      payment)
        curl -s -o /dev/null -X POST "$PAYMENT_URL/payments" \
          -H "Content-Type: application/json" -d "{\"orderId\": \"$pid\"}"
        curl -s -o /dev/null "$PAYMENT_URL/payments/$pid"
        ;;
      shipping)
        curl -s -o /dev/null -X POST "$SHIPPING_URL/shipments" \
          -H "Content-Type: application/json" -d "{\"sku\": \"$sku\"}"
        curl -s -o /dev/null "$SHIPPING_URL/shipments/$sku"
        ;;
      notification)
        curl -s -o /dev/null -X POST "$NOTIFICATION_URL/notifications" \
          -H "Content-Type: application/json" -d "{\"paymentId\": \"$pid\", \"sku\": \"$sku\"}"
        ;;
    esac
    count=$((count + 1))
  done
  echo "[$name] finalizado (~$count ciclos)"
}

echo
echo "Iniciando carga por ${DURATION_SECONDS}s, concorrência ${CONCURRENCY} por rota..."
echo "Pressione Ctrl+C para interromper antes do fim."
echo

for i in $(seq 1 "$CONCURRENCY"); do
  [ -n "$ORDER_IP" ] && { worker orders & worker hello & }
  [ -n "$INVENTORY_IP" ] && worker inventory &
  [ -n "$SUPPLIER_IP" ] && worker supplier &
  [ -n "$PAYMENT_IP" ] && worker payment &
  [ -n "$SHIPPING_IP" ] && worker shipping &
  [ -n "$NOTIFICATION_IP" ] && worker notification &
done

wait
echo
echo "Carga finalizada."
