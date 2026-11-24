# Minhas Finanças - Gerenciador de Finanças Pessoal

Aplicativo Android nativo para controle e gestão financeira pessoal, desenvolvido em **Kotlin** e **Jetpack Compose** com arquitetura moderna MVVM, Material Design 3 e SQLite nativo.

Criado por **Rodrigo Pinheiro Ink**.

---

## 🚀 Principais Recursos

- **Controle de Movimentações**: Registro simplificado e detalhado de Entradas (Receitas) e Saídas (Despesas).
- **Múltiplos Meios de Pagamento**: Suporte a Pix, Cartão de Crédito, Cartão de Débito, Dinheiro e divisão entre múltiplos meios.
- **Vendas na Maquininha de Cartão**:
  - Cálculo automático de taxas em tempo real (valor cobrado vs. valor recebido em conta).
  - Exibição de taxas em porcentagem (%) e em valor líquido (R$).
  - Painel dedicado de **Taxas da Maquininha** na tela inicial com filtros por período personalizado (calendário em PT-BR) e por banco.
- **Filtros Avançados e Visores Financeiros**:
  - **Filtrar por Marcador**: Visor de saldo, receitas e despesas por categorias/tags selecionadas, com botão "Todos" e gerenciador de marcadores.
  - **Filtrar por Bancos**: Visor segmentado com chips de bancos dinâmicos e cálculo em tempo real.
- **Gráficos Interativos**:
  - Gráfico de barras comparativo mensal de Entradas vs. Saídas.
  - Gráfico Donut de distribuição de despesas por marcador.
- **Parcelamentos e Recorrências**: Gestão de compras parceladas e planos recorrentes com frequências personalizáveis.
- **Múltiplos Perfis de Usuário**: Alternância rápida entre diferentes perfis (ex.: Pessoal, Empresarial).
- **Histórico Imutável de Auditoria**: Registro permanente de criações, edições e exclusões para controle seguro.
- **Backup e Restauração**: Exportação e importação completa de dados com validação.
- **Tema Claro e Escuro**: Suporte dinâmico a Dark Mode e Light Mode.

---

## 🛠️ Tecnologias e Arquitetura

- **Linguagem**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose com Material Design 3
- **Arquitetura**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Gerenciamento de Estado**: Kotlin Coroutines & StateFlow
- **Banco de Dados**: SQLite Nativo (Android SQLiteOpenHelper)
- **Design Adaptativo**: Suporte a Edge-to-Edge e internacionalização (PT-BR)
- **Build System**: Gradle com Kotlin DSL (`build.gradle.kts`) e Version Catalogs (`libs.versions.toml`)

---

## 📦 Como Compilar e Executar

1. Clone o repositório:
   ```bash
   git clone https://github.com/rodrigopinheiroink/GerenciadorDeFinancasPessoal.git
   ```
2. Abra o projeto no **Android Studio** (Ladybug / Meerkat ou superior).
3. Aguarde a sincronização do Gradle.
4. Execute no emulador ou dispositivo físico com Android 8.0 (API 26) ou superior.
