#!/usr/bin/env bash
set -e

BASE_URL="http://localhost:8080"

echo "=================================================="
echo "1. Login / Signup como ADMIN (admin_gutemberg)"
echo "=================================================="
ADMIN_RES=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"nome":"admin_gutemberg"}')

if echo "$ADMIN_RES" | grep -q '"error"'; then
  ADMIN_RES=$(curl -s -X POST "$BASE_URL/api/auth/signup" \
    -H "Content-Type: application/json" \
    -d '{"nome":"admin_gutemberg","idade":30,"pcd":true,"papel":"ADMIN"}')
fi

echo "$ADMIN_RES"
ADMIN_TOKEN=$(echo "$ADMIN_RES" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "-> Token ADMIN obtido com sucesso!"
echo ""

echo "=================================================="
echo "2. Login / Signup como USUARIO (maria_silva)"
echo "=================================================="
USER_RES=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"nome":"maria_silva"}')

if echo "$USER_RES" | grep -q '"error"'; then
  USER_RES=$(curl -s -X POST "$BASE_URL/api/auth/signup" \
    -H "Content-Type: application/json" \
    -d '{"nome":"maria_silva","idade":72,"pcd":true,"papel":"USUARIO"}')
fi

echo "$USER_RES"
USER_TOKEN=$(echo "$USER_RES" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
USER_ID=$(echo "$USER_RES" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "-> Token USUARIO obtido! ID da Maria: $USER_ID"
echo ""

echo "=================================================="
echo "3. Teste de Seguranca: USUARIO tentando criar Semaforo (Esperado: HTTP 403)"
echo "=================================================="
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/semaforos" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Paulista x Augusta","latitude":-23.5587,"longitude":-46.6602,"ativo":true}')
echo "-> Status retornado para USUARIO: HTTP $HTTP_STATUS"
echo ""

echo "=================================================="
echo "4. Criando Semaforo INATIVO como ADMIN (ativo: false)"
echo "=================================================="
SEMAFORO_RES=$(curl -s -X POST "$BASE_URL/api/semaforos" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Av. Paulista x Rua Augusta","latitude":-23.5587,"longitude":-46.6602,"ativo":false}')
echo "$SEMAFORO_RES"
SEMAFORO_ID=$(echo "$SEMAFORO_RES" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "-> Semaforo criado INATIVO com ID: $SEMAFORO_ID"
echo ""

echo "=================================================="
echo "5. Tentando Solicitar Travessia em Semaforo INATIVO (Deve bloquear)"
echo "=================================================="
INATIVO_RES=$(curl -s -X POST "$BASE_URL/api/solicitacoes" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"usuarioId\":$USER_ID,\"semaforoId\":$SEMAFORO_ID,\"latitudeUsuario\":-23.5587,\"longitudeUsuario\":-46.6602}")
echo "-> Resposta para semaforo inativo: $INATIVO_RES"
echo ""

echo "=================================================="
echo "6. Ativando o Semaforo como ADMIN (PUT /api/semaforos/$SEMAFORO_ID -> ativo: true)"
echo "=================================================="
ATIVADO_RES=$(curl -s -X PUT "$BASE_URL/api/semaforos/$SEMAFORO_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Av. Paulista x Rua Augusta","latitude":-23.5587,"longitude":-46.6602,"ativo":true}')
echo "-> Semaforo atualizado: $ATIVADO_RES"
echo ""

echo "=================================================="
echo "7. Verificando Proximidade GPS LONGE (~600m) -> Botao deve estar BLOQUEADO (false)"
echo "=================================================="
PROX_LONGE=$(curl -s -X GET "$BASE_URL/api/semaforos/$SEMAFORO_ID/proximidade?latitude=-23.5640&longitude=-46.6602" \
  -H "Authorization: Bearer $USER_TOKEN")
echo "-> Status do botao (LONGE): $PROX_LONGE"
echo ""

echo "=================================================="
echo "8. Tentando Solicitar Travessia LONGE do Semaforo (Deve bloquear com HTTP 400)"
echo "=================================================="
LONGE_RES=$(curl -s -X POST "$BASE_URL/api/solicitacoes" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"usuarioId\":$USER_ID,\"semaforoId\":$SEMAFORO_ID,\"latitudeUsuario\":-23.5640,\"longitudeUsuario\":-46.6602}")
echo "-> Resposta com usuario longe: $LONGE_RES"
echo ""

echo "=================================================="
echo "9. Verificando Proximidade GPS PERTO (~5m) -> Botao deve estar DESBLOQUEADO (true)"
echo "=================================================="
PROX_PERTO=$(curl -s -X GET "$BASE_URL/api/semaforos/$SEMAFORO_ID/proximidade?latitude=-23.55875&longitude=-46.6602" \
  -H "Authorization: Bearer $USER_TOKEN")
echo "-> Status do botao (PERTO): $PROX_PERTO"
echo ""

echo "=================================================="
echo "10. Criando Solicitacao de Travessia PERTO do Semaforo ATIVO (Clima pt_br + IA 5s/10s)"
echo "=================================================="
SOLICITACAO_RES=$(curl -s -X POST "$BASE_URL/api/solicitacoes" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"usuarioId\":$USER_ID,\"semaforoId\":$SEMAFORO_ID,\"latitudeUsuario\":-23.55875,\"longitudeUsuario\":-46.6602}")
echo "$SOLICITACAO_RES"
echo ""
