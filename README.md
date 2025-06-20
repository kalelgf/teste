# 🗡️ RPG Market - Marketplace de Itens RPG

Um marketplace medieval fantástico para compra e venda de itens RPG, desenvolvido com Spring Boot e MongoDB.

## 🎯 **Novidades & Melhorias**

✨ **Sistema de leilões aprimorado com:**
- Auto-atualização de lances em tempo real
- Animações e efeitos visuais modernos
- Logs detalhados para depuração
- Interface aprimorada com melhor usabilidade

📖 **[Ver Resumo Completo das Melhorias](SOLUTION_SUMMARY.md)**

## 🚀 Instalação Rápida

### Para usuários que querem começar rapidamente:

**Windows:**
```bash
# Execute o script de instalação automática
install.bat
```

**Linux/macOS:**
```bash
# Execute o script de instalação automática
chmod +x install.sh
./install.sh
```

### Para instalação manual detalhada:
📖 **[Consulte o Guia Completo de Instalação](GUIA_INSTALACAO.md)**

## 📋 Pré-requisitos (Resumo)

- **Java JDK 17+** 
- **MongoDB 7.0+**
- **Git** (opcional)

## ⚡ Execução Rápida

Se você já tem as dependências instaladas:

```bash
# Clone o projeto
git clone <url-do-repositorio>
cd rpg_market

# Execute
./mvnw spring-boot:run
```

Acesse: `http://localhost:8080`
4. Obtenha a string de conexão
5. Atualize o `application.properties`:
```properties
spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/rpgmarket
```

### 3. Configuração do Diretório de Upload
Crie o diretório para upload de imagens:
```bash
# Windows
mkdir uploads\images

# Linux/Mac
mkdir -p uploads/images
```

### 4. Configuração das Propriedades
Edite o arquivo `src/main/resources/application.properties` se necessário:

```properties
# Configuração da Aplicação
spring.application.name=rpg_market
server.port=8080

# MongoDB - Ajuste se necessário
spring.data.mongodb.uri=mongodb://localhost:27017/rpgmarket

# Upload de Arquivos - Ajuste o caminho se necessário  
rpg.market.file.upload-dir=uploads/images
```

## 🛠️ Compilação e Execução

### 1. Compilar o Projeto
```bash
# Usando Maven Wrapper (recomendado)
./mvnw clean compile

# Ou usando Maven instalado
mvn clean compile
```

### 2. Executar Testes
```bash
./mvnw test
```

### 3. Executar a Aplicação

#### Opção A: Via Maven
```bash
./mvnw spring-boot:run
```

#### Opção B: Via JAR
```bash
./mvnw clean package
java -jar target/rpg_market-0.0.1-SNAPSHOT.jar
```

#### Opção C: Via IDE
1. Abra o projeto na sua IDE
2. Execute a classe `RpgMarketApplication.java`

### 4. Acesso à Aplicação
- **URL**: http://localhost:8080
- **Usuário Admin**: `admin` / `admin`
- **Usuário Teste**: `testuser` / `password`

## 📁 Estrutura do Projeto

```
rpg_market/
├── src/
│   ├── main/
│   │   ├── java/com/programacao_web/rpg_market/
│   │   │   ├── config/          # Configurações (Security, MongoDB, etc.)
│   │   │   ├── controller/      # Controllers MVC
│   │   │   ├── dto/            # Data Transfer Objects
│   │   │   ├── model/          # Entidades/Modelos
│   │   │   ├── repository/     # Repositórios MongoDB
│   │   │   ├── service/        # Lógica de negócio
│   │   │   └── RpgMarketApplication.java
│   │   └── resources/
│   │       ├── static/         # CSS, JS, imagens estáticas
│   │       ├── templates/      # Templates Thymeleaf
│   │       └── application.properties
│   └── test/                   # Testes unitários e integração
├── uploads/                    # Upload de imagens (criado em runtime)
├── pom.xml                     # Dependências Maven
└── README.md                   # Este arquivo
```

## 🎮 Funcionalidades

### Para Usuários (Aventureiros)
- ✅ Registro e login de usuários
- ✅ Navegação por categorias de itens
- ✅ Sistema de compra direta
- ✅ Sistema de leilão com lances
- ✅ Gerenciamento de inventário pessoal
- ✅ Histórico de compras e vendas
- ✅ Perfil de usuário com estatísticas RPG

### Para Administradores (Mestres)
- ✅ Moderação de produtos
- ✅ Gestão de usuários
- ✅ Monitoramento de transações

### Recursos Técnicos
- ✅ Interface responsiva com tema medieval
- ✅ Upload de imagens para produtos
- ✅ Sistema de endereços com geolocalização
- ✅ Validação de formulários
- ✅ Segurança com Spring Security
- ✅ Auto-refresh em páginas de leilão

## 🐛 Solução de Problemas

### Erro de Conexão MongoDB
```
com.mongodb.MongoSocketException: Exception opening socket
```
**Solução**: Verifique se o MongoDB está executando:
```bash
# Windows
net start MongoDB

# Linux/Mac
sudo systemctl start mongod
```

### Erro de Porta Ocupada
```
Web server failed to start. Port 8080 was already in use
```
**Solução**: 
1. Mate o processo na porta 8080: `netstat -ano | findstr :8080`
2. Ou altere a porta no `application.properties`: `server.port=8081`

### Erro de JAVA_HOME
```
Error: JAVA_HOME is not defined correctly
```
**Solução**: Configure a variável de ambiente JAVA_HOME apontando para o diretório do JDK.

### Erro de Permissão de Upload
```
Could not create directory: uploads/images
```
**Solução**: 
1. Crie o diretório manualmente
2. Verifique as permissões de escrita
3. Execute a aplicação com privilégios apropriados

## 📦 Dependências Principais

- **Spring Boot 3.2.4** - Framework principal
- **Spring Security** - Autenticação e autorização
- **Spring Data MongoDB** - Persistência de dados
- **Thymeleaf** - Template engine
- **Bootstrap 5.3** - Framework CSS
- **Font Awesome** - Ícones
- **MongoDB** - Banco de dados NoSQL

## 🔧 Configurações Avançadas

### Configuração de Produção
Para ambiente de produção, considere:

1. **MongoDB com autenticação**:
```properties
spring.data.mongodb.uri=mongodb://username:password@localhost:27017/rpgmarket
```

2. **HTTPS e certificados SSL**
3. **Configuração de proxy reverso (Nginx/Apache)**
4. **Monitoramento com Spring Actuator**
5. **Logging configurado**

### Variáveis de Ambiente
Para maior segurança, use variáveis de ambiente:
```bash
export MONGODB_URI="mongodb://localhost:27017/rpgmarket"
export UPLOAD_DIR="/var/uploads/images"
```

## 🤝 Contribuição

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/NovaFuncionalidade`)
3. Commit suas mudanças (`git commit -am 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/NovaFuncionalidade`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo `LICENSE` para mais detalhes.

## 📞 Suporte

Para dúvidas e suporte:
- Abra uma issue no repositório
- Consulte a documentação do Spring Boot
- Verifique os logs da aplicação em `logs/application.log`

---

⚔️ **Que a sorte esteja com você, aventureiro!** ⚔️
