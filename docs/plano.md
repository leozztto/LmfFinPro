# FinPro — Controle Financeiro Inteligente para Autônomos

*Plano de escopo do projeto*

## 1. Status atual da implementação

*Leitura feita direto no código do repositório em 16/09/2026, após as branches de estrutura de backend e frontend. A fundação (modelagem de dados, ambiente local, CI) está pronta; a camada de API, a lógica de negócio e as telas reais do MVP ainda não foram escritas.*

**Backend (~30%)**

- ✅ 9 entidades JPA + repositories (User, Account, Category, CategoryRule, Client, ImportBatch, Transaction, TaxEstimate, Budget)
- ✅ Schema completo via Flyway (`V1__init_schema.sql`)
- ✅ Scaffold de segurança (BCrypt, filter chain stateless) e exception handler global
- ⬜ Controllers, Services e DTOs (nenhum ainda existe)
- ⬜ Filtro JWT real — hoje todo endpoint está com `permitAll()`
- ⬜ Importação de CSV e motor de categorização
- ⬜ Testes além do `contextLoads` padrão

**Frontend (~15%)**

- ✅ Projeto Vite + React 19 + TypeScript + Tailwind CSS 4
- ✅ Dependências instaladas: Router, React Query, Recharts, React Hook Form + Zod
- ✅ Dockerfile + Nginx para build de produção
- ⬜ Tela única hoje, com cards de saldo/receita/despesa mockados em "R$ 0,00"
- ⬜ Nenhuma chamada à API, nenhuma rota, nenhum formulário funcional

**Infra & docs (~85%)**

- ✅ Docker Compose com postgres + backend + frontend
- ✅ CI no GitHub Actions, separado por pasta (`mvn verify` / `npm build`)
- ✅ README com setup local e este plano de escopo
- ⬜ Deploy real (Railway/Render + Vercel)
- ⬜ Seed de dados de demonstração e badge de CI no README

Em resumo: o "esqueleto" dos dois projetos e o modelo de dados estão sólidos, mas nenhuma funcionalidade do MVP é operável de ponta a ponta ainda — é o próximo passo (ver seções 5 e 12).

## 2. Pitch (resumo de 30 segundos)

Uma aplicação de controle financeiro que, diferente dos "trackers" genéricos, resolve três dores reais de quem trabalha como autônomo/freelancer: (1) importar e categorizar automaticamente o extrato bancário sem digitação manual, (2) organizar receita por cliente/projeto e estimar impostos e fluxo de caixa irregular, e (3) ser tecnicamente sólida o suficiente (arquitetura limpa, testes, deploy real) para servir como prova de competência full-stack em entrevistas.

## 3. Público-alvo e problema

Freelancers e autônomos (devs, designers, consultores) não têm contracheque fixo nem RH cuidando de imposto. Eles pagam vários clientes, têm meses bons e ruins, e normalmente controlam tudo em planilha solta. O app resolve: "quanto eu realmente ganhei líquido este mês, de quem, e quanto devo guardar pra imposto?"

## 4. Os três diferenciais combinados

1. **Importação + categorização automática**: usuário sobe um CSV/OFX do banco; o sistema categoriza as transações sozinho (regras + aprendizado com correções do usuário).
2. **Foco em freelancer/autônomo**: receita organizada por cliente/projeto, estimativa simplificada de imposto e projeção de fluxo de caixa irregular.
3. **Qualidade técnica de portfólio**: arquitetura em camadas, testes automatizados, CI, deploy ao vivo, dados seed realistas.

## 5. Escopo

### MVP (Fase 1)

- Cadastro/login de usuário (Spring Security + JWT) — *scaffold*
- Cadastro manual de contas bancárias/carteiras — *modelo pronto*
- CRUD de transações (receita/despesa) — *modelo pronto*
- Cadastro de categorias (padrão do sistema + customizadas) — *modelo pronto*
- Importação de extrato via CSV — *pendente*
- Categorização automática por regras (palavra-chave no descritivo → categoria) — *pendente*
- Cadastro de clientes/projetos e vínculo de receitas a eles — *modelo pronto*
- Dashboard: saldo atual, receita x despesa no mês, gráfico por categoria, receita por cliente — *pendente*
- Deploy funcional com dados de exemplo — *pendente*

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

## 6. Modelo de dados (entidades principais)

✅ *Implementado — todas as entidades abaixo já existem como classes JPA com repositories Spring Data, e o schema está aplicado via Flyway (`V1__init_schema.sql`).*

- **User**: id, nome, email, senha (hash), regime_tributario
- **Account**: id, user_id, nome, tipo, saldo_inicial
- **Category**: id, user_id (null = padrão do sistema), nome, tipo, cor, ícone
- **CategoryRule**: id, user_id, padrão_texto, category_id, peso/confiança
- **Client**: id, user_id, nome, ativo
- **Transaction**: id, account_id, category_id, client_id, descrição, valor, data, tipo, origem, import_batch_id
- **ImportBatch**: id, user_id, account_id, arquivo_original, formato, data_importação, status
- **TaxEstimate**: id, user_id, mês/ano, receita_bruta, alíquota_aplicada, valor_estimado
- **Budget**: id, user_id, category_id, mês/ano, valor_limite

## 7. Arquitetura técnica

**Backend — Java 21 + Spring Boot 3 + Maven + PostgreSQL**

- Camadas: Controller → Service → Repository (Spring Data JPA), DTOs separados de entidades — *só a camada Repository existe hoje; Controller, Service e DTOs ainda não foram escritos*
- Autenticação: Spring Security + JWT — *scaffold de segurança presente, filtro JWT ainda não plugado*
- Migrations: Flyway — *feito (V1)*
- Documentação da API: springdoc-openapi (Swagger UI) — *dependência configurada*
- Testes: JUnit 5 + Mockito (unidade), Testcontainers + Postgres real (integração) — *dependências presentes, nenhum teste de negócio escrito ainda*

**Frontend — React + Tailwind CSS + Vite**

- React Query para estado de chamadas à API — *instalado, não consumido*
- React Router — *instalado, não consumido*
- Recharts para os gráficos do dashboard — *instalado, não consumido*
- React Hook Form + Zod para formulários — *instalado, não consumido*

**Infraestrutura**

- Docker Compose (postgres + backend + frontend) para desenvolvimento local — *feito*
- GitHub Actions (CI separado para backend e frontend, por path) — *feito*
- Deploy: backend em Railway/Render, frontend em Vercel — *pendente*

## 8. Motor de categorização automática

*Pendente — só a modelagem (`ImportBatch`, `CategoryRule`) existe hoje; a lógica abaixo ainda não foi implementada.*

1. Importação cria um `ImportBatch` e lê cada linha como transação
2. Busca `CategoryRule` do usuário com match no descritivo
3. Se não encontrar, tenta regras padrão do sistema (ex: "UBER" → Transporte)
4. Se nada bater, marca como "Sem categoria" para revisão manual
5. Correção manual do usuário reforça/cria uma regra para descritivos parecidos

## 9. Módulo freelancer

- Receita vinculada a `Client`
- Dashboard de receita por cliente no período
- Estimativa de imposto configurável por regime (aviso: estimativa educacional, não substitui contador)
- Projeção de fluxo de caixa por média móvel

## 10. Telas principais

*Pendente — existe apenas um dashboard estático com valores mockados; as telas abaixo ainda não foram construídas.*

- Visão geral (saldo, receita/despesa, variação)
- Extrato (filtros por conta/categoria/cliente/período)
- Importação (upload + preview + revisão de categorização)
- Clientes (receita acumulada e ao longo do tempo)
- Impostos/Fluxo de caixa
- Categorias e regras

## 11. Checklist de qualidade técnica

- Testes automatizados nos fluxos críticos (importação, categorização, saldo) — *pendente*
- Lint/formatação consistente — *pendente*
- CI verde (badge no README) — *CI ok, sem badge*
- Dados seed realistas (script com usuário demo + ~6 meses de transações) — *pendente*
- Deploy ao vivo com usuário de demonstração — *pendente*
- README completo: problema, diferenciais, prints, stack, setup local, link do deploy — *sem prints ainda*

## 12. Roadmap sugerido

1. Semana 1-2: modelagem, setup Spring Boot + Postgres + Flyway, auth JWT, CRUD básico — *modelagem e setup concluídos; auth JWT real e CRUD básico (controllers/services) ainda faltam*
2. Semana 3: importação CSV + motor de regras + tela de revisão
3. Semana 4: dashboard (visão geral, extrato, gráficos)
4. Semana 5: clientes/projetos + receita por cliente
5. Semana 6: estimativa de imposto + projeção de fluxo de caixa
6. Semana 7: testes automatizados, CI, seed de dados, polimento
7. Semana 8: deploy, README, vídeo curto de demo

## 13. Stack resumida

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3, Maven, Spring Security, Spring Data JPA |
| Banco | PostgreSQL, Flyway |
| Testes | JUnit 5, Mockito, Testcontainers |
| Frontend | React, Tailwind CSS, Vite, React Query, Recharts |
| Infra | Docker Compose, GitHub Actions, Railway/Render + Vercel |
