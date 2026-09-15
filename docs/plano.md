# FinPro — Controle Financeiro Inteligente para Autônomos

*Plano de escopo do projeto*

## 1. Pitch (resumo de 30 segundos)

Uma aplicação de controle financeiro que, diferente dos "trackers" genéricos, resolve três dores reais de quem trabalha como autônomo/freelancer: (1) importar e categorizar automaticamente o extrato bancário sem digitação manual, (2) organizar receita por cliente/projeto e estimar impostos e fluxo de caixa irregular, e (3) ser tecnicamente sólida o suficiente (arquitetura limpa, testes, deploy real) para servir como prova de competência full-stack em entrevistas.

## 2. Público-alvo e problema

Freelancers e autônomos (devs, designers, consultores) não têm contracheque fixo nem RH cuidando de imposto. Eles pagam vários clientes, têm meses bons e ruins, e normalmente controlam tudo em planilha solta. O app resolve: "quanto eu realmente ganhei líquido este mês, de quem, e quanto devo guardar pra imposto?"

## 3. Os três diferenciais combinados

1. **Importação + categorização automática**: usuário sobe um CSV/OFX do banco; o sistema categoriza as transações sozinho (regras + aprendizado com correções do usuário).
2. **Foco em freelancer/autônomo**: receita organizada por cliente/projeto, estimativa simplificada de imposto e projeção de fluxo de caixa irregular.
3. **Qualidade técnica de portfólio**: arquitetura em camadas, testes automatizados, CI, deploy ao vivo, dados seed realistas.

## 4. Escopo

### MVP (Fase 1)

- Cadastro/login de usuário (Spring Security + JWT)
- Cadastro manual de contas bancárias/carteiras
- CRUD de transações (receita/despesa)
- Cadastro de categorias (padrão do sistema + customizadas)
- Importação de extrato via CSV
- Categorização automática por regras (palavra-chave no descritivo → categoria)
- Cadastro de clientes/projetos e vínculo de receitas a eles
- Dashboard: saldo atual, receita x despesa no mês, gráfico por categoria, receita por cliente
- Deploy funcional com dados de exemplo

### Fase 2

- Importação de OFX
- Aprendizado de categorização a partir das correções do usuário
- Estimativa de imposto simplificada (educacional, não é orientação fiscal)
- Projeção de fluxo de caixa (média móvel + recebíveis futuros)
- Metas/orçamento por categoria

### Fase 3

- Recibo/cobrança em PDF por cliente
- Multi-moeda
- Modo escuro, responsividade mobile completa
- Exportação de relatórios (PDF/Excel)

## 5. Modelo de dados (entidades principais)

- **User**: id, nome, email, senha (hash), regime_tributario
- **Account**: id, user_id, nome, tipo, saldo_inicial
- **Category**: id, user_id (null = padrão do sistema), nome, tipo, cor, ícone
- **CategoryRule**: id, user_id, padrão_texto, category_id, peso/confiança
- **Client**: id, user_id, nome, ativo
- **Transaction**: id, account_id, category_id, client_id, descrição, valor, data, tipo, origem, import_batch_id
- **ImportBatch**: id, user_id, account_id, arquivo_original, formato, data_importação, status
- **TaxEstimate**: id, user_id, mês/ano, receita_bruta, alíquota_aplicada, valor_estimado
- **Budget**: id, user_id, category_id, mês/ano, valor_limite

## 6. Arquitetura técnica

**Backend — Java 21 + Spring Boot 3 + Maven + PostgreSQL**

- Camadas: Controller → Service → Repository (Spring Data JPA), DTOs separados de entidades
- Autenticação: Spring Security + JWT
- Migrations: Flyway
- Documentação da API: springdoc-openapi (Swagger UI)
- Testes: JUnit 5 + Mockito (unidade), Testcontainers + Postgres real (integração)

**Frontend — React + Tailwind CSS + Vite**

- React Query para estado de chamadas à API
- React Router
- Recharts para os gráficos do dashboard
- React Hook Form + Zod para formulários

**Infraestrutura**

- Docker Compose (postgres + backend) para desenvolvimento local
- GitHub Actions (CI separado para backend e frontend, por path)
- Deploy: backend em Railway/Render, frontend em Vercel

## 7. Motor de categorização automática

1. Importação cria um `ImportBatch` e lê cada linha como transação
2. Busca `CategoryRule` do usuário com match no descritivo
3. Se não encontrar, tenta regras padrão do sistema (ex: "UBER" → Transporte)
4. Se nada bater, marca como "Sem categoria" para revisão manual
5. Correção manual do usuário reforça/cria uma regra para descritivos parecidos

## 8. Módulo freelancer

- Receita vinculada a `Client`
- Dashboard de receita por cliente no período
- Estimativa de imposto configurável por regime (aviso: estimativa educacional, não substitui contador)
- Projeção de fluxo de caixa por média móvel

## 9. Telas principais

- Visão geral (saldo, receita/despesa, variação)
- Extrato (filtros por conta/categoria/cliente/período)
- Importação (upload + preview + revisão de categorização)
- Clientes (receita acumulada e ao longo do tempo)
- Impostos/Fluxo de caixa
- Categorias e regras

## 10. Checklist de qualidade técnica

- Testes automatizados nos fluxos críticos (importação, categorização, saldo)
- Lint/formatação consistente
- CI verde (badge no README)
- Dados seed realistas (script com usuário demo + ~6 meses de transações)
- Deploy ao vivo com usuário de demonstração
- README completo: problema, diferenciais, prints, stack, setup local, link do deploy

## 11. Roadmap sugerido

1. Semana 1-2: modelagem, setup Spring Boot + Postgres + Flyway, auth JWT, CRUD básico
2. Semana 3: importação CSV + motor de regras + tela de revisão
3. Semana 4: dashboard (visão geral, extrato, gráficos)
4. Semana 5: clientes/projetos + receita por cliente
5. Semana 6: estimativa de imposto + projeção de fluxo de caixa
6. Semana 7: testes automatizados, CI, seed de dados, polimento
7. Semana 8: deploy, README, vídeo curto de demo

## 12. Stack resumida

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3, Maven, Spring Security, Spring Data JPA |
| Banco | PostgreSQL, Flyway |
| Testes | JUnit 5, Mockito, Testcontainers |
| Frontend | React, Tailwind CSS, Vite, React Query, Recharts |
| Infra | Docker Compose, GitHub Actions, Railway/Render + Vercel |
