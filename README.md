# 🏃 Bio Survival — Backend API

> Backend REST API do aplicativo fitness **Bio Survival**, desenvolvido como Trabalho de Conclusão de Curso (TCC) no Inatel. A aplicação oferece autenticação JWT, sessões de treino em tempo real via WebSocket, dados biométricos, conquistas, rankings com cache Redis e sistema gamificado de Hordes.

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white" />
  <img src="https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" />
  <img src="https://img.shields.io/badge/WebSocket-STOMP-010101?style=for-the-badge&logo=socketdotio&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/Jenkins-CI%2FCD-D24939?style=for-the-badge&logo=jenkins&logoColor=white" />
  <img src="https://img.shields.io/badge/Swagger-OpenAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" />
  <img src="https://img.shields.io/badge/Flyway-Migration-CC0200?style=for-the-badge&logo=flyway&logoColor=white" />
</p>

---

## 📋 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Modelo de Dados](#-modelo-de-dados)
- [Endpoints da API](#-endpoints-da-api)
- [WebSocket — Biometria em Tempo Real](#-websocket--biometria-em-tempo-real)
- [Pré-requisitos](#-pré-requisitos)
- [Configuração e Execução](#-configuração-e-execução)
- [Variáveis de Ambiente](#-variáveis-de-ambiente)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Testes](#-testes)
- [Estrutura de Pastas](#-estrutura-de-pastas)

---

## 🎯 Sobre o Projeto

**Bio Survival** é um aplicativo fitness que transforma treinos em uma experiência gamificada. Os usuários podem registrar sessões de corrida, monitorar dados biométricos em tempo real (BPM, cadência, pace, calorias), desbloquear conquistas e competir em rankings globais.

Este repositório contém exclusivamente o **backend**, responsável por:

- 🔐 Autenticação e autorização via **JWT (JSON Web Tokens)**
- 🏃 Gerenciamento de sessões de treino e dados biométricos em tempo real via **WebSocket STOMP**
- 🏆 Sistema de conquistas e rankings com cache **Redis**
- 👥 Sistema de amizades entre usuários
- 👾 Gestão de **Hordes** (desafios gamificados de corrida com horda virtual)
- 💓 Monitoramento de zona cardíaca em tempo real
- 📊 Leaderboard em tempo real com broadcast via WebSocket

---

## 🛠 Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| [Kotlin](https://kotlinlang.org/) | 2.1.20 | Linguagem principal |
| [Spring Boot](https://spring.io/projects/spring-boot) | 3.4.4 | Framework web |
| [Spring Security](https://spring.io/projects/spring-security) | — | Autenticação e autorização |
| [Spring Data JPA](https://spring.io/projects/spring-data-jpa) | — | Persistência de dados |
| [Spring WebSocket](https://docs.spring.io/spring-framework/reference/web/websocket.html) | — | Comunicação em tempo real (STOMP) |
| [Spring Data Redis](https://spring.io/projects/spring-data-redis) | — | Cache e leaderboards em tempo real |
| [PostgreSQL](https://www.postgresql.org/) | 17 | Banco de dados relacional |
| [Redis](https://redis.io/) | 7 | Cache de sessões e leaderboards |
| [Flyway](https://flywaydb.org/) | — | Migrations de banco de dados |
| [SpringDoc OpenAPI](https://springdoc.org/) | 2.8.5 | Documentação Swagger/OpenAPI |
| [JJWT](https://github.com/jwtk/jjwt) | 0.12.6 | Geração e validação de tokens JWT |
| [BCrypt](https://en.wikipedia.org/wiki/Bcrypt) | — | Hash de senhas |
| [Docker](https://www.docker.com/) | — | Containerização (multi-stage build) |
| [Jenkins](https://www.jenkins.io/) | — | CI/CD Pipeline |
| [JaCoCo](https://www.jacoco.org/) | 0.8.13 | Cobertura de testes |
| [Maven](https://maven.apache.org/) | — | Gerenciador de dependências |
| [JUnit 5](https://junit.org/junit5/) + [Mockito](https://site.mockito.org/) | — | Testes unitários |
| [H2](https://www.h2database.com/) | — | Banco de dados em memória para testes |

---

## 🏛 Arquitetura

O projeto segue a arquitetura em camadas do Spring Boot com comunicação em tempo real via WebSocket e cache Redis:

<p align="center">
  <img src="docs/architecture.svg" alt="Architecture Diagram" width="100%" />
</p>

### Fluxo de Autenticação

```
1. Client → POST /auth/register ou /auth/login
2. AuthController → AuthService (valida dados)
3. AuthService → UserRepository (busca/salva usuário)
4. AuthService → JwtService (gera token JWT)
5. Resposta: { token, name, email }

Para rotas protegidas:
1. Client → Header: Authorization: Bearer <token>
2. JwtAuthFilter → JwtService (valida token)
3. Request passa para o Controller
```

### Fluxo de Biometria em Tempo Real

```
1. Galaxy Watch → dados de sensores via Wear OS Data Layer API (Bluetooth)
2. App React Native → WebSocket STOMP: /app/train/data (BiometricDataMessage)
3. BiometricWebSocketController:
   a. Persiste no PostgreSQL de forma assíncrona
   b. Calcula zona cardíaca (cache Redis)
   c. Atualiza estado do jogador no Redis (< 1ms por operação)
   d. Atualiza leaderboard no Redis (Sorted Set)
   e. Calcula posição virtual da horda
   f. Broadcast leaderboard → /topic/session/{id}/leaderboard
   g. Broadcast game state → /topic/session/{id}/game-state
```

---

## 🗄 Modelo de Dados

```
users
 ├── id (UUID, PK)
 ├── email (VARCHAR, UNIQUE)
 ├── name (VARCHAR)
 ├── password (VARCHAR, bcrypt)
 ├── birthday_date (DATE)
 ├── height (DOUBLE)
 ├── weight (DOUBLE)
 └── max_heart_rate (INT)

achievements          hordes (self-ref)
 ├── id (UUID)         ├── id (UUID)
 ├── title             ├── name
 ├── description       ├── description
 ├── url_icon          ├── difficulty (EASY/MEDIUM/HARD)
 ├── criterion         ├── estimated_duration
 └── active            ├── target_pace
                       ├── adaptive (BOOLEAN)
                       └── horde_id (FK → hordes)

train_sessions                    biometric_data
 ├── id (UUID)                     ├── id (UUID)
 ├── user_id (FK → users)          ├── timestamp
 ├── start_date                    ├── bpm
 ├── end_date                      ├── cadence
 ├── train_type                    ├── speed
 ├── total_distance                ├── pace
 ├── estimated_calories            ├── accumulated_distance
 └── horde_id (FK → hordes)        ├── accumulated_calories
                                   ├── cardiac_zone
                                   └── train_session_id (FK)

user_achievements         rankings                  friendships
 ├── user_id (FK)          ├── id (UUID)              ├── id (UUID)
 ├── achievement_id (FK)   ├── user_id (FK)           ├── requester_id (FK)
 └── unlock_date           ├── position               ├── recipient_id (FK)
                           ├── score                  ├── request_date
                           ├── period                 ├── response_date
                           ├── calcule_date           └── status
                           └── target_distance
```

---

## 📡 Endpoints da API

> Documentação interativa disponível em: `http://134.122.112.15:8080/swagger-ui/index.html`

### Autenticação

> Rotas públicas — não requerem token JWT.

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/auth/register` | Cria uma nova conta de usuário |
| `POST` | `/auth/login` | Autentica um usuário existente |

#### `POST /auth/register`

**Request Body:**
```json
{
  "email": "usuario@exemplo.com",
  "name": "João Silva",
  "password": "minhasenha123",
  "birthdayDate": "2000-05-15",
  "height": 1.80,
  "weight": 75.5
}
```

**Response `201 Created`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "name": "João Silva",
  "email": "usuario@exemplo.com"
}
```

#### `POST /auth/login`

**Request Body:**
```json
{
  "email": "usuario@exemplo.com",
  "password": "minhasenha123"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "name": "João Silva",
  "email": "usuario@exemplo.com"
}
```

### Usuários

> Rotas protegidas — requerem `Authorization: Bearer <token>`

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/users/{id}` | Retorna os dados de um usuário |
| `PATCH` | `/users/{id}` | Atualiza parcialmente um usuário |

### Sessões de Treino

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/sessions/start` | Inicia uma nova sessão de treino |
| `POST` | `/sessions/{sessionId}/finish` | Finaliza uma sessão |
| `GET` | `/sessions/{sessionId}` | Retorna os dados de uma sessão |
| `GET` | `/sessions/{sessionId}/leaderboard` | Retorna o leaderboard da sessão |
| `GET` | `/sessions/hordes` | Lista todas as hordes disponíveis |
| `GET` | `/sessions/ranking/{period}` | Retorna o ranking global por período |

#### `POST /sessions/start`

**Request Body:**
```json
{
  "hordeId": "uuid-optional",
  "trainType": "RUN",
  "goalDistanceKm": 5.0
}
```

**Response `201 Created`:**
```json
{
  "sessionId": "uuid-da-sessao"
}
```

### Rankings

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/rankings/{period}` | Rankings históricos por período (`WEEKLY`, `MONTHLY`) |
| `GET` | `/rankings/user/{userId}` | Rankings históricos de um usuário |

### Respostas de Erro Padronizadas

| Status | Cenário |
|---|---|
| `400 Bad Request` | Campos inválidos ou ausentes |
| `401 Unauthorized` | Token ausente, inválido ou expirado |
| `404 Not Found` | Recurso não encontrado |
| `409 Conflict` | Email já cadastrado |
| `500 Internal Server Error` | Erro interno do servidor |

---

## 🔌 WebSocket — Biometria em Tempo Real

A comunicação de biometria durante as sessões de treino é feita via **WebSocket STOMP**.

### Conexão

```
ws://host:8080/ws
```

### Canal de Entrada

**Destino:** `/app/train/data`

**Payload (BiometricDataMessage):**
```json
{
  "sessionId": "uuid-da-sessao",
  "userId": "uuid-do-usuario",
  "timestamp": 1717970400000,
  "bpm": 142,
  "cadence": 85.5,
  "speed": 10.2,
  "pace": 5.88,
  "accumulatedDistance": 2.35,
  "accumulatedCalories": 180.5
}
```

### Canais de Saída (Broadcast)

**Leaderboard:** `/topic/session/{sessionId}/leaderboard`
```json
{
  "sessionId": "uuid",
  "userRank": 2,
  "hordeVirtualDistanceKm": 1.8,
  "entries": [
    { "userId": "uuid", "rank": 1, "distanceKm": 2.5, "cardiacZone": "VIGOROUS" }
  ],
  "isBehindHorde": false,
  "distanceToHorde": -0.55
}
```

**Game State:** `/topic/session/{sessionId}/game-state`
```json
{
  "sessionId": "uuid",
  "userId": "uuid",
  "playerPosition": 2.35,
  "hordePosition": 1.80,
  "distanceToGoal": 2.65,
  "distancePlayerToHorde": 0.55,
  "playerSpeed": 10.2,
  "hordeSpeed": 8.5,
  "raceProgress": 47.0,
  "gameStatus": "RUNNING",
  "serverTimestampMs": 1717970400000
}
```

> **Game Status:** `RUNNING` | `CAUGHT` (horde alcançou o jogador) | `ESCAPED` (jogador completou a meta)
>
> **Unidades do Game State:** `playerPosition`, `hordePosition`, `distanceToGoal` e `distancePlayerToHorde` usam quilometros; `playerSpeed` e `hordeSpeed` usam km/h; `raceProgress` e percentual no intervalo `0.0..100.0`; `serverTimestampMs` usa epoch ms gerado pelo backend.

---

## ✅ Pré-requisitos

Certifique-se de ter instalado:

- **Java 25+** (JDK)
- **Maven 3.8+**
- **PostgreSQL 17+**
- **Redis 7+**
- **Docker & Docker Compose** (opcional, para execução containerizada)

---

## 🚀 Configuração e Execução

### Opção 1: Docker Compose (recomendado)

```bash
git clone https://github.com/seu-usuario/tcc.git
cd tcc
./mvnw clean package -DskipTests
docker-compose up -d
```

A stack completa será iniciada: **API + PostgreSQL + Redis + Jenkins + Ngrok**.

### Opção 2: Execução Local

#### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/tcc.git
cd tcc
```

#### 2. Configure o banco de dados

```sql
CREATE DATABASE tcc;
CREATE USER seu_usuario WITH ENCRYPTED PASSWORD 'sua_senha';
GRANT ALL PRIVILEGES ON DATABASE tcc TO seu_usuario;
```

#### 3. Inicie o Redis

```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

#### 4. Configure as variáveis de ambiente

Edite `src/main/resources/application.properties` ou defina as variáveis de ambiente (veja a seção [Variáveis de Ambiente](#-variáveis-de-ambiente)).

#### 5. Inicie a aplicação

```bash
./mvnw spring-boot:run
```

A API estará disponível em: `http://localhost:8080`

A documentação Swagger: `http://localhost:8080/swagger-ui.html`


---

## 🔄 CI/CD Pipeline

O projeto utiliza **Jenkins** com pipeline declarativo e notificação por email.

```
GitHub Push → Jenkins (webhook via Ngrok)
  ├── 1. Build        → mvnw clean install -DskipTests
  ├── 2. Tests        → mvnw clean verify (JaCoCo coverage)
  └── 3. Deploy       → SCP JAR + SSH docker-compose up (DigitalOcean Droplet)
                      → Email notification (sucesso/falha)
```

O Jenkins roda como container Docker dentro do `docker-compose.yml`, com o Ngrok expondo o webhook para o GitHub.

---

## 🧪 Testes

O projeto possui **30 arquivos de teste** cobrindo todas as camadas. Redis em container temporário é usado nos testes de integração e H2 como banco em memória.

### Executar todos os testes

```bash
./mvnw clean verify
```

<p align="center">
  Desenvolvido por <strong>Dimitri, José, Pedro Augusto, Pedro Bressan</strong> — Inatel © 2026
</p>
