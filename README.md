# 📦 order-worker-ms

Microsserviço **worker** responsável por processar eventos de pedidos confirmados, atualizar o banco de dados e publicar mensagens de despacho em filas de mensageria.

## 🚀 Arquitetura

O `order-worker-ms` foi construído com **Spring Boot** e integra:

* **AWS SQS (via LocalStack)** → consumo e publicação de mensagens
* **PostgreSQL** → armazenamento e atualização de pedidos
* **TestContainers** → ambiente de testes totalmente isolado

### Fluxo do Worker

1. Consome mensagens da fila **`order-confirmed-queue`**
2. Consulta o pedido no banco **Postgres**
3. Atualiza o status do pedido (ex: notificado)
4. Publica evento na fila **`shipping-queue`**

## 🧪 Testes de Integração

Este projeto não expõe endpoints HTTP. Os testes garantem a confiabilidade do fluxo de mensageria através de:

* **TestContainers** → provisiona containers do **PostgreSQL** e do **LocalStack**
* **LocalStack** → simulação das filas **SQS**
* **Awaitility** → controle de assincronia para aguardar o processamento das mensagens

### Cenários testados

<p> ✅ Pedido existente → deve publicar na fila de shipping </p>
<p> ✅ Pedido existente → deve atualizar o banco de dados </p>
<p> ✅ Pedido inexistente → não deve publicar na fila de shipping </p>

## 🛠️ Tecnologias

* Java 21
* Spring Boot
* Spring Cloud AWS
* PostgreSQL
* TestContainers
* LocalStack
* JUnit 5 + Awaitility

## ▶️ Como executar

### Requisitos

* Docker
* Java 21
* Maven

### Passos

```bash
# Clone o repositório
git clone https://github.com/wallacevilela7/orderworker-ms-integration
cd orderworkerms

# Rodar os testes (subirá containers automaticamente)
./mvnw test

# Rodar a aplicação
./mvnw spring-boot:run
```

## 📂 Estrutura do Projeto

```
order-worker-ms
 ┣ src
 ┃ ┣ main
 ┃ ┃ ┣ java/tech/buildrun/orderworkerms
 ┃ ┃ ┃ ┣ consumer  # Consumo das filas
 ┃ ┃ ┃ ┣ producer  # Publicação em filas
 ┃ ┃ ┃ ┣ entity    # Entidades do banco
 ┃ ┃ ┃ ┣ repository # Repositórios JPA
 ┃ ┣ test
 ┃ ┃ ┣ java/tech/buildrun/orderworkerms
 ┃ ┃ ┃ ┣ consumer  # Testes de integração do Worker
 ┣ pom.xml
```

## 📖 Aprendizados

* Estratégias para testar microsserviços **sem endpoints HTTP**
* Uso do **LocalStack** e **TestContainers** para simular infraestrutura cloud
* Garantia de resiliência em cenários de mensageria distribuída

---

💡 Obrigado por conferir este projeto!  
Contribuições, sugestões e feedbacks são sempre bem-vindos.

✨ Happy Coding! 🚀🚀🚀
