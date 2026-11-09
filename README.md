# TravessIA - Backend de Travessia Inteligente para Pedestres

O **TravessIA** é um sistema de microsserviços em Java (Spring Boot) desenvolvido para auxiliar **idosos e 
pessoas com deficiência (PCD)** na travessia segura de semáforos inteligentes.

---

## 1. Sobre o Projeto e Regras de Negócio

O sistema resolve o problema do tempo curto do semaforos de travessia de pedestres em cruzamentos urbanos. A proposta é 
o usuario que deve ter mais 60 anos e/ou pcd ganhar mais tempo para atravessar a rua, sendo que em dias "secos" esse 
tempo a mais é de 5 segundos e dias chuvosos é de 10 segundos.

1. **Perfis de Acesso (`ADMIN` e `USUARIO`) e Regra de Cadastro (PCD ou 60+):**
   - **Regra Obrigatória de Cadastro:** O cadastro no sistema só pode ser feito por uma pessoa **PCD (`pcd: true`)** 
   ou **60+ (`idade >= 60`)**. Tentativas de cadastro com `idade < 60` e `pcd: false` são bloqueadas com 
   - `HTTP 400 Bad Request`.
   - **`ADMIN` (Gestor):** Único perfil com permissão para cadastrar, editar,    ativar/desativar e  remover semáforos,
   além de gerenciar usuários e solicitações.
   - **`USUARIO` (Cidadão):** Pode consultar semáforos, verificar sua proximidade via GPS 
   e solicitar tempo adicional de travessia.
2. **Trava de Proximidade por GPS (Raio de 10 metros):**
   - O aplicativo mobile envia a latitude e longitude atuais do aparelho (`latitudeUsuario` e `longitudeUsuario`).
   - O sistema calcula a distância exata até o semáforo usando a **Fórmula de Haversine**. 
   O botão de travessia só é liberado (e a solicitação só é aceita) se o semáforo estiver **ativo** (`ativo: true`)
   - e o usuário estiver a **no minimo a 10 metros** do semáforo.
3. **Clima em Tempo Real + Inteligência Artificial (Spring AI):**
   - Ao solicitar a travessia, o microsserviço consulta o clima atual na coordenada do semáforo via **OpenWeatherMap**.
   - O modelo **OpenAI (`gpt-4o-mini`)** via **Spring AI** analisa o perfil do pedestre (idade e condição PCD) 
   junto com o clima real e define automaticamente o tempo adicional do semáforo:
   - **Tempo seco / sem chuva:** `+5` segundos extras.
   - **Chuva / garoa / tempestade / pista molhada:** `+10` segundos extras.

---

## 2. Tecnologias Utilizadas

- **Java 21** e **Maven** (Projeto Multi-Módulo)
- **Spring Boot 3.3.0**
- **Spring Data JPA** + **Flyway** + **Oracle Database (FIAP)**
- **Spring Security** + **JWT (JSON Web Token)** com validade de 1 ano para manter a sessão no aplicativo mobile
- **Spring AI 1.0.0** (Integração com LLM da OpenAI `gpt-4o-mini`)
- **Spring Cloud Netflix Eureka** (Service Discovery)
- **Spring Cloud OpenFeign** (Comunicação entre microsserviços e com a API OpenWeatherMap)
- **Bean Validation (`jakarta.validation`)** + Tratamento Global de Erros (`@RestControllerAdvice`)
- **Springdoc OpenAPI / Swagger UI**
- **Docker**, **Docker Compose** e Blueprint para **Render (`render.yaml`)**

---

## 3. Arquitetura dos Microsserviços

O projeto é dividido em 4 módulos Maven:

1. **`eureka-server` (Porta `8761`):** Servidor de descoberta onde os microsserviços se registram.
2. **`biblioteca-comum`:** Módulo compartilhado contendo DTOs com Bean Validation, Enum de Papéis (`USUARIO`, `ADMIN`), 
utilitários de segurança JWT (`JwtUtil`, `JwtAuthenticationFilter`), cliente Feign de Clima (`ClimaCliente`) 
e tratamento global de exceções (`ManipuladorGlobalExcecao`).
3. **`gateway-service` (Porta `8080`):** Porta de entrada da API REST. Valida o token JWT, 
aplica o controle de acesso por papel (`ADMIN` vs `USUARIO`), expõe o Swagger UI e encaminha as chamadas 
ao `traffic-service` via OpenFeign (`TrafficClient`).
4. **`traffic-service` (Porta `8081`):** Microsserviço de domínio conectado ao banco Oracle da FIAP 
(`TB_TRAVESSIA_USUARIO`, `TB_TRAVESSIA_SEMAFORO`, `TB_TRAVESSIA_SOLICITACAO`). 
Executa o cálculo de distância GPS (Haversine), consulta o OpenWeatherMap e aciona a OpenAI via Spring AI.

---

## 4. Como Rodar o Projeto

### Pré-requisitos
- **Docker** e **Docker Compose** instalados (ou Java 21 + Maven).
- Arquivo `.env` na raiz da pasta `GS_TravessIA` com as variáveis:

```env
ORACLE_URL=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
ORACLE_USER=rm562267
ORACLE_PASSWORD=sua_senha_oracle
OPENAI_API_KEY=sua_chave_openai_aqui
WEATHER_API_KEY=sua_chave_openweather_aqui
JWT_SECRET=TravessIASecretKeyForHS256AlgorithmMinimum256BitsRequired2026Fiap
JWT_EXPIRATION_MS=31536000000
```

### Passo a passo com Docker Compose (Recomendado)

No terminal, dentro da pasta `GS_TravessIA`:

```bash
docker compose up -d --build
```

Para verificar se os 3 containers (`eureka`, `gateway`, `traffic`) estão rodando:

```bash
docker compose ps
```

Para executar a bateria automática de testes de ponta a ponta no terminal:

```bash
./test-api.sh
```

### Links Úteis (Ambiente Local)
- **API Base URL (Gateway):** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Painel Eureka:** `http://localhost:8761`

---

## 5. Credenciais de Exemplo (Admin e Usuário)

O login no TravessIA é simplificado para acessibilidade mobile (autenticação pelo campo `nome`,
que gera o **Token JWT Bearer** com validade de 1 ano).

| Perfil | Nome de Login (`nome`) | Idade | PCD | Permissão |
| :--- | :--- | :---: | :---: | :--- |
| **Administrador (`ADMIN`)** | `admin_gutemberg` | 30 | `true` | Acesso total (criar/editar/excluir semáforos, usuários e solicitações) |
| **Usuário Comum (`USUARIO`)** | `maria_silva` | 72 | `true` | Consultar semáforos, checar proximidade GPS e solicitar travessia |

> **Como autenticar no Postman:**
> 1. Faça uma requisição para `POST http://localhost:8080/api/auth/login` 
  (ou `/api/auth/signup` se o usuário ainda não existir).
> 2. Copie o valor do campo `"token"` retornado no JSON.
> 3. Nas demais rotas do Postman, vá na aba **Authorization** -> selecione **Bearer Token** e cole o token 
  (ou adicione o Header `Authorization: Bearer SEU_TOKEN_AQUI`).

---

## 6. Guia Completo de Rotas para o Postman

Todas as requisições devem ser enviadas para o **Gateway** (`http://localhost:8080`) com o Header:
- `Content-Type: application/json`
---

### A) Autenticação (Público - Não precisa de Token)

#### 1. Cadastrar Novo Usuário ou Admin (`POST /api/auth/signup`)
- **URL:** `http://localhost:8080/api/auth/signup`
- - **Authorization:** `Type: No Auth`
- **Body (JSON) - Exemplo Criando ADMIN:**
```json
{
  "nome": "Bruno",
  "idade": 30,
  "pcd": true,
  "papel": "ADMIN"
}
```
- **Body (JSON) - Exemplo Criando USUARIO:**
```json
{
  "nome": "maria_silva",
  "idade": 72,
  "pcd": true,
  "papel": "USUARIO"
}
```

#### 2. Fazer Login (`POST /api/auth/login`)
- **URL:** `http://localhost:8080/api/auth/login`
- **Body (JSON) - Login como ADMIN:**
```json
{
  "nome": "admin_gutemberg"
}
```
- **Body (JSON) - Login como USUARIO:**
```json
{
  "nome": "maria_silva"
}
```
- **Resposta Esperada (`200 OK`):**
```json
{
  "usuario": {
    "id": 1,
    "nome": "admin_gutemberg",
    "idade": 30,
    "pcd": false,
    "papel": "ADMIN"
  },
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### B) Semáforos (`/api/semaforos`)

#### 3. Cadastrar Semáforo (`POST /api/semaforos`) — *Exige Token de `ADMIN`*
- **URL:** `http://localhost:8080/api/semaforos`
- **Header:** `Authorization: Bearer <TOKEN_DO_ADMIN>`
- **Body (JSON):**
```json
{
  "descricao": "Av. Paulista x Rua Augusta",
  "latitude": -23.5587,
  "longitude": -46.6602,
  "ativo": true
}
```

#### 4. Listar Semáforos com Paginação (`GET /api/semaforos`) — *Qualquer usuário logado*
- **URL:** `http://localhost:8080/api/semaforos?page=0&size=10`
- **Header:** `Authorization: Bearer <TOKEN>`

#### 5. Buscar Semáforo por ID (`GET /api/semaforos/{id}`) — *Qualquer usuário logado*
- **URL:** `http://localhost:8080/api/semaforos/1`
- **Header:** `Authorization: Bearer <TOKEN>`

#### 6. Verificar Proximidade GPS para Desbloquear Botão (`GET /api/semaforos/{id}/proximidade`) — *Qualquer usuário logado*
- **Header:** `Authorization: Bearer <TOKEN>`
- Considerando que o Semáforo `ID 1` foi cadastrado em `latitude: -23.5587` e `longitude: -46.6602`:

  - **Para testar o botão BLOQUEADO (simulando que você está longe, a 589m):**
    Você chama no Postman:
    `GET http://localhost:8080/api/semaforos/1/proximidade?latitude=-23.5640&longitude=-46.6602`
    -> O sistema responde: `"distanciaMetros": 589, "botaoDesbloqueado": false`

  - **Para testar o botão LIBERADO (simulando que você chegou a ~5m do semáforo):**
    Você chama no Postman mudando só o finalzinho da latitude:
    `GET http://localhost:8080/api/semaforos/1/proximidade?latitude=-23.55875&longitude=-46.6602`
    -> O sistema responde: `"distanciaMetros": 5, "botaoDesbloqueado": true`

- **Resposta Completa Esperada (`200 OK` quando liberado):**
```json
{
  "semaforoId": 1,
  "semaforoAtivo": true,
  "distanciaMetros": 5,
  "raioMaximoPermitidoMetros": 10.0,
  "botaoDesbloqueado": true
}
```

#### 7. Atualizar / Ativar ou Desativar Semáforo (`PUT /api/semaforos/{id}`) — *Exige Token de `ADMIN`*
- **URL:** `http://localhost:8080/api/semaforos/1`
- **Header:** `Authorization: Bearer <TOKEN_DO_ADMIN>`
- **Body (JSON):**
```json
{
  "descricao": "Av. Paulista x Rua Augusta (Atualizado)",
  "latitude": -23.5587,
  "longitude": -46.6602,
  "ativo": true
}
```

#### 8. Remover Semáforo (`DELETE /api/semaforos/{id}`) — *Exige Token de `ADMIN`*
- **URL:** ` http://localhost:8080/api/semaforos/{id}`
- **Header:** `Authorization: Bearer <TOKEN_DO_ADMIN>`

---

### C) Solicitações de Travessia (`/api/solicitacoes`)

#### 9. Solicitar Travessia com Clima + IA (`POST /api/solicitacoes`) — *Qualquer usuário logado*
> **Atenção:** O semáforo informado precisa estar com `"ativo": true` e as coordenadas `latitudeUsuario` 
  / `longitudeUsuario` precisam estar **10 metros** ou menos da coordenada do semáforo. O sistema busca no banco Oracle
  a coordenada do semáforo informado (latitude e longitude) e faz o cálculo de distância entre o GPS do usuário e esse
  semáforo.

- **URL:** `http://localhost:8080/api/solicitacoes`
- **Header:** `Authorization: Bearer <TOKEN>`
- **Body (JSON):**
```json
{
  "usuarioId": 2,
  "semaforoId": 1,
  "latitudeUsuario": -23.55875,
  "longitudeUsuario": -46.6602
}
```
- **Resposta Esperada (`201 Created`):**
```json
{
  "id": 1,
  "usuario": {
    "id": 2,
    "nome": "maria_silva",
    "idade": 72,
    "pcd": true,
    "papel": "USUARIO"
  },
  "semaforo": {
    "id": 1,
    "descricao": "Av. Paulista x Rua Augusta",
    "latitude": -23.5587,
    "longitude": -46.6602,
    "ativo": true
  },
  "dataHora": "2026-11-08T12:00:00",
  "condicaoClima": "Céu limpo",
  "tempoAdicionalCalculado": 5
}
```

#### 10. Listar Solicitações de Travessia com Paginação (`GET /api/solicitacoes`) — *Qualquer usuário logado*
- **URL:** `http://localhost:8080/api/solicitacoes?page=0&size=10`
- **Header:** `Authorization: Bearer <TOKEN>`

#### 11. Buscar Solicitação por ID (`GET /api/solicitacoes/{id}`) — *Qualquer usuário logado*
- **URL:** `http://localhost:8080/api/solicitacoes/1`
- **Header:** `Authorization: Bearer <TOKEN>`

#### 12. Atualizar Solicitação (`PUT /api/solicitacoes/{id}`) — *Exige Token de `ADMIN`*
- **URL:** `http://localhost:8080/api/solicitacoes/1`
- **Header:** `Authorization: Bearer <TOKEN_DO_ADMIN>`
- **Body (JSON):**
```json
{
  "usuarioId": 2,
  "semaforoId": 1,
  "latitudeUsuario": -23.55875,
  "longitudeUsuario": -46.6602
}
```

#### 13. Remover Solicitação (`DELETE /api/solicitacoes/{id}`) — *Exige Token de `ADMIN`*
- **URL:** `http://localhost:8080/api/solicitacoes/1`
- **Header:** `Authorization: Bearer <TOKEN_DO_ADMIN>`

---

### D) Usuários (`/api/usuarios`)

#### 14. Listar Usuários com Paginação (`GET /api/usuarios`) — *Exige Token de `ADMIN`*
- **URL:** `http://localhost:8080/api/usuarios?page=0&size=10`
- **Header:** `Authorization: Bearer <TOKEN_DO_ADMIN>`

#### 15. Buscar Usuário por ID (`GET /api/usuarios/{id}`) — *Qualquer usuário logado*
- **URL:** `http://localhost:8080/api/usuarios/2`
- **Header:** `Authorization: Bearer <TOKEN>`

#### 16. Atualizar Usuário (`PUT /api/usuarios/{id}`) — *Qualquer usuário logado*
- **URL:** `http://localhost:8080/api/usuarios/2`
- **Header:** `Authorization: Bearer <TOKEN>`
- **Body (JSON):**
```json
{
  "nome": "maria_silva",
  "idade": 73,
  "pcd": true,
  "papel": "USUARIO"
}
```

#### 17. Remover Usuário (`DELETE /api/usuarios/{id}`) — *Exige Token de `ADMIN`*
- **URL:** `http://localhost:8080/api/usuarios/2`
- **Header:** `Authorization: Bearer <TOKEN_DO_ADMIN>`

---

