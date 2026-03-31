# 🏃 Bio Survival — Backend API

> Backend REST API do aplicativo fitness **Bio Survival**, desenvolvido como Trabalho de Conclusão de Curso (TCC) no Inatel. A aplicação oferece autenticação segura via JWT, gerenciamento de sessões de treino, dados biométricos, conquistas, rankings e sistema de amizades.

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" />
  <img src="https://img.shields.io/badge/Flyway-Migration-CC0200?style=for-the-badge&logo=flyway&logoColor=white" />
</p>

---

## 📋 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Modelo de Dados](#-modelo-de-dados)
- [Endpoints da API](#-endpoints-da-api)
- [Pré-requisitos](#-pré-requisitos)
- [Configuração e Execução](#-configuração-e-execução)
- [Variáveis de Ambiente](#-variáveis-de-ambiente)
- [Testes](#-testes)
- [Estrutura de Pastas](#-estrutura-de-pastas)

---

## 🎯 Sobre o Projeto

**Bio Survival** é um aplicativo fitness que transforma treinos em uma experiência gamificada. Os usuários podem registrar sessões de corrida, monitorar dados biométricos em tempo real (BPM, cadência, pace, calorias), desbloquear conquistas e competir em rankings globais.

Este repositório contém exclusivamente o **backend**, responsável por:

- 🔐 Autenticação e autorização via **JWT (JSON Web Tokens)**
- 🏃 Gerenciamento de sessões de treino e dados biométricos
- 🏆 Sistema de conquistas e rankings
- 👥 Sistema de amizades entre usuários
- 👾 Gestão de Hordes (desafios gamificados de corrida)

---

## 🛠 Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| [Kotlin](https://kotlinlang.org/) | 2.1.20 | Linguagem principal |
| [Spring Boot](https://spring.io/projects/spring-boot) | 3.4.4 | Framework web |
| [Spring Security](https://spring.io/projects/spring-security) | — | Autenticação e autorização |
| [Spring Data JPA](https://spring.io/projects/spring-data-jpa) | — | Persistência de dados |
| [PostgreSQL](https://www.postgresql.org/) | 16+ | Banco de dados relacional |
| [Flyway](https://flywaydb.org/) | — | Migrations de banco de dados |
| [JJWT](https://github.com/jwtk/jjwt) | 0.12.6 | Geração e validação de tokens JWT |
| [BCrypt](https://en.wikipedia.org/wiki/Bcrypt) | — | Hash de senhas |
| [Maven](https://maven.apache.org/) | — | Gerenciador de dependências |
| [JUnit 5](https://junit.org/junit5/) + [Mockito](https://site.mockito.org/) | — | Testes unitários |
| [H2](https://www.h2database.com/) | — | Banco de dados em memória para testes |

---

## 🏛 Arquitetura

O projeto segue a arquitetura em camadas do Spring Boot:

```
┌─────────────────────────────────────────────────────────┐
│                   Client (Mobile App)                   │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP/REST
┌──────────────────────────▼──────────────────────────────┐
│              Security Layer  (JWT Auth Filter)          │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                   Controller Layer                      │
│              (AuthController, ...)                      │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                    Service Layer                        │
│              (AuthService, JwtService, ...)             │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                   Repository Layer                      │
│              (UserRepository, ...)                      │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                     PostgreSQL                          │
│              (Flyway Migrations)                        │
└─────────────────────────────────────────────────────────┘
```

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
 ├── url_icon          ├── difficulty
 ├── criterion         ├── estimated_duration
 └── active            ├── target_pace
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

user_achievements         rankings               friendships
 ├── user_id (FK)          ├── id (UUID)           ├── id (UUID)
 ├── achievement_id (FK)   ├── user_id (FK)         ├── requester_id (FK)
 └── unlock_date           ├── position            ├── recipient_id (FK)
                           ├── score               ├── request_date
                           ├── period              ├── response_date
                           └── calcule_date         └── status
```

---

## 📡 Endpoints da API

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

### Rotas Protegidas

> Todas as rotas abaixo requerem o header:
> ```
> Authorization: Bearer <seu_token_jwt>
> ```

> ⚠️ Endpoints de domínio (treinos, conquistas, amizades, etc.) estão em desenvolvimento.

### Respostas de Erro Padronizadas

| Status | Cenário |
|---|---|
| `400 Bad Request` | Campos inválidos ou ausentes |
| `401 Unauthorized` | Token ausente, inválido ou expirado |
| `409 Conflict` | Email já cadastrado |
| `500 Internal Server Error` | Erro interno do servidor |

---

## ✅ Pré-requisitos

Certifique-se de ter instalado:

- **Java 25+** (JDK)
- **Maven 3.8+**
- **PostgreSQL 16+**

---

## 🚀 Configuração e Execução

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/tcc.git
cd tcc
```

### 2. Configure o banco de dados

Crie o banco de dados no PostgreSQL:

```sql
CREATE DATABASE tcc;
CREATE USER seu_usuario WITH ENCRYPTED PASSWORD 'sua_senha';
GRANT ALL PRIVILEGES ON DATABASE tcc TO seu_usuario;
```

### 3. Configure as variáveis de ambiente

Copie o arquivo de exemplo e edite com suas configurações:

```bash
cp src/main/resources/application.properties src/main/resources/application-local.properties
```

Edite `application.properties` (veja a seção [Variáveis de Ambiente](#-variáveis-de-ambiente)).

### 4. Execute as migrations

As migrations são executadas **automaticamente** pelo Flyway ao iniciar a aplicação.

### 5. Inicie a aplicação

```bash
./mvnw spring-boot:run
```

A API estará disponível em: `http://localhost:8080`

---

## ⚙️ Variáveis de Ambiente

Configure o arquivo `src/main/resources/application.properties`:

| Propriedade | Descrição | Exemplo |
|---|---|---|
| `spring.datasource.url` | URL de conexão com o PostgreSQL | `jdbc:postgresql://localhost:5432/tcc` |
| `spring.datasource.username` | Usuário do banco de dados | `meu_usuario` |
| `spring.datasource.password` | Senha do banco de dados | `minha_senha` |
| `jwt.secret` | Chave secreta para geração de tokens JWT (mín. 256 bits) | `UmaSenhaGrandeESegura...` |
| `jwt.expiration` | Tempo de expiração do token em milissegundos | `86400000` (24h) |

> ⚠️ **Segurança:** Nunca versione o `application.properties` com credenciais reais. Use variáveis de ambiente ou um cofre de segredos em produção.

---

## 🧪 Testes

O projeto possui testes unitários e de integração. O banco H2 em memória é utilizado automaticamente durante os testes.

### Executar todos os testes

```bash
./mvnw test
```

### Cobertura de testes

| Camada | Arquivo de Teste | Cobertura |
|---|---|---|
| **Service** | `AuthServiceTest.kt` | Registro, login, credenciais inválidas, email duplicado |
| **Service** | `JwtServiceTest.kt` | Geração, validação e extração de claims do token |
| **Controller** | `AuthControllerTest.kt` | Integração dos endpoints `/auth/register` e `/auth/login` |

---

## 📁 Estrutura de Pastas

```
tcc/
├── src/
│   ├── main/
│   │   ├── kotlin/br/inatel/tcc/
│   │   │   ├── TccApplication.kt          # Ponto de entrada da aplicação
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.kt      # Configuração do Spring Security e CORS
│   │   │   │   ├── JwtAuthFilter.kt       # Filtro de validação do token JWT
│   │   │   │   └── GlobalExceptionHandler.kt # Tratamento global de exceções
│   │   │   ├── controller/
│   │   │   │   └── AuthController.kt      # Endpoints de autenticação
│   │   │   ├── domain/
│   │   │   │   ├── user/                  # Entidade User e repositório
│   │   │   │   ├── achievement/           # Conquistas
│   │   │   │   ├── biometricdata/         # Dados biométricos
│   │   │   │   ├── friendship/            # Amizades
│   │   │   │   ├── horde/                 # Hordes (desafios)
│   │   │   │   ├── ranking/               # Rankings
│   │   │   │   ├── trainsession/          # Sessões de treino
│   │   │   │   └── userachievement/       # Conquistas por usuário
│   │   │   ├── dto/
│   │   │   │   ├── AuthResponse.kt        # Resposta de autenticação
│   │   │   │   ├── LoginRequest.kt        # Request de login
│   │   │   │   └── RegisterRequest.kt     # Request de registro
│   │   │   └── service/
│   │   │       ├── AuthService.kt         # Lógica de negócio de autenticação
│   │   │       └── JwtService.kt          # Geração e validação de JWT
│   │   └── resources/
│   │       ├── application.properties     # Configurações da aplicação
│   │       └── db/migration/
│   │           ├── V1__create_user_table.sql      # Criação da tabela users
│   │           └── V2__create_domain_tables.sql   # Criação das tabelas de domínio
│   └── test/
│       └── kotlin/br/inatel/tcc/
│           ├── controller/
│           │   └── AuthControllerTest.kt  # Testes de integração dos endpoints
│           ├── service/
│           │   ├── AuthServiceTest.kt     # Testes unitários do AuthService
│           │   └── JwtServiceTest.kt      # Testes unitários do JwtService
│           └── TccApplicationTests.kt     # Teste de contexto do Spring
├── pom.xml                                # Dependências Maven
└── README.md
```

---

<p align="center">
  Desenvolvido por <strong>Dimitri, José, Pedro Augusto, Pedro Bressan</strong> — Inatel © 2026
</p>
