# FinPro — Perguntas Frequentes (FAQ)

*Dúvidas comuns de quem usa o FinPro no dia a dia.*

## Conta e acesso

**Como eu crio uma conta no FinPro?**
Na tela de cadastro que pode ser acessado pela tela de login, no link Cadastre-se, nessa página você informa nome, e-mail, senha (mínimo 8 caracteres), CPF ou CNPJ, telefone, regime tributário e endereço. O CEP preenche automaticamente rua, bairro e cidade — só confira e complete o número.

**Preciso informar CPF ou CNPJ?**
Depende do regime tributário escolhido: **CNPJ** para MEI, Simples Nacional e Lucro Presumido; **CPF** para Autônomo e Outro. O sistema valida isso automaticamente e não deixa cadastrar um CPF com um regime de pessoa jurídica (ou vice-versa).

**Esqueci minha senha, como recupero?**
Hoje não existe recuperação de senha self-service (não tem "esqueci minha senha" na tela de login). Isso ainda não foi implementado.

**Fui desconectado do nada no meio do uso — o que aconteceu?**
Seu login expira automaticamente depois de 1 hora (por segurança). Quando isso acontece, você é deslogado e redirecionado pra tela de login, com um aviso explicando o motivo — é só entrar de novo.

**Meus dados financeiros são visíveis para outros usuários?**
Não. Toda informação (contas, transações, clientes, etc.) é isolada por usuário — mesmo tentando acessar um registro de outra pessoa diretamente pela URL, o sistema trata como se ele não existisse.

## Contas bancárias

**Qual a diferença entre os tipos de conta?**
São só categorias pra você organizar: **Conta corrente**, **Poupança** e **Carteira** (dinheiro físico/uso avulso). Não há diferença de comportamento entre elas — todas funcionam igual, é só pra você identificar visualmente.

**Posso mudar o saldo inicial de uma conta depois de criada?**
Não. O saldo inicial é fixado na criação e não pode ser editado depois — só dá pra mudar nome e tipo. Isso é de propósito: o saldo *atual* que você vê no sistema é sempre calculado a partir de todas as transações lançadas, nunca é um número editável à parte, então deixar o saldo inicial mutável abriria brecha pra ele ficar inconsistente com o histórico.

**Por que não consigo excluir uma conta?**
Uma conta com transações ou transferências vinculadas não pode ser excluída — o sistema bloqueia pra evitar perder histórico financeiro sem querer. Remova (ou mova) as transações primeiro.

## Categorias

**O que são categorias "padrão do sistema"?**
É um conceito que existe no modelo de dados (categorias sem dono, visíveis pra todo mundo), mas hoje **nenhuma categoria vem pré-cadastrada** — toda categoria que você vê foi criada por você mesmo. Todas as suas categorias podem ser editadas e removidas livremente (respeitando as regras abaixo).

**Posso mudar uma categoria de receita para despesa (ou vice-versa) depois de criada?**
Não. O tipo é fixado na criação. Se você errou o tipo, crie uma categoria nova com o tipo certo e mova as transações pra ela (editando cada transação — veja a limitação de edição de transação abaixo).

**Por que não consigo excluir uma categoria?**
Categoria com transações ou regras de categorização automática vinculadas não pode ser excluída, pelo mesmo motivo das contas: evitar perder histórico ou quebrar uma regra que ainda está em uso.

## Transações (receitas e despesas)

**Como eu lanço uma receita ou despesa?**
Na tela de Transações, botão "Nova transação": escolha a conta, opcionalmente uma categoria e um cliente, descrição, valor (sempre positivo) e a data. O tipo (receita/despesa) define o sinal — não existe "valor negativo" no formulário.

**Dá pra editar uma transação depois de criada?**
Hoje não — a tela de Transações só permite **criar** e **excluir**, não tem opção de editar. Se você errou algo, exclua e lance de novo.

**Por que uma transação aparece com o botão de excluir desabilitado?**
Ela faz parte de uma transferência (tem a etiqueta "Transferência"). Transações desse tipo só podem ser removidas junto com a transferência inteira, na tela de Transferências — isso porque toda transferência sempre lança duas transações (uma em cada conta) e as duas precisam sumir juntas pra manter os saldos corretos.

## Transferências

**Como faço uma transferência entre minhas contas?**
Na tela de Transferências: conta de origem, conta de destino (tem que ser diferentes), valor, data e uma descrição opcional. O sistema não deixa transferir mais do que o saldo disponível na conta de origem.

**Por que uma transferência gera duas transações?**
Porque, financeiramente, uma transferência é uma saída de uma conta e uma entrada em outra ao mesmo tempo — o sistema modela isso como duas transações (uma despesa na origem, uma receita no destino) ligadas entre si, pra que o saldo de cada conta continue batendo individualmente.

**Transferências contam como receita ou despesa no Dashboard?**
Não. Elas são excluídas de todo cálculo de receita/despesa (nos cards, gráficos e orçamentos) — afinal, transferir dinheiro entre suas próprias contas não é ganhar nem gastar, só reorganizar onde o dinheiro está.

## Clientes

**Pra que serve cadastrar clientes?**
Pra organizar sua receita por cliente/projeto — o Dashboard mostra um gráfico de "receita por cliente" no mês, e isso ajuda a enxergar quem são seus melhores clientes ao longo do tempo.

**Preciso vincular um cliente em toda transação?**
Não, o campo cliente é opcional em qualquer transação. Receitas sem cliente vinculado aparecem agrupadas como "Sem cliente" no gráfico de receita por cliente.

## Importação de extrato

**Que formato de arquivo o sistema aceita?**
Só CSV, com cabeçalho fixo `date,description,amount` (data no formato `aaaa-mm-dd`, valor com ponto decimal — positivo é receita, negativo é despesa). Extratos de banco geralmente precisam ser ajustados pra esse formato antes de subir.

**Como funciona a categorização automática na importação?**
O sistema mantém uma lista de regras (palavra-chave → categoria) e, ao importar, tenta casar a descrição de cada transação com alguma regra existente. Quando não encontra nenhuma, a transação entra sem categoria — você categoriza manualmente na tela de revisão, e ao fazer isso o sistema **aprende**: reforça a regra existente pra aquele padrão de descrição (ou cria uma nova, se ainda não existia), ficando cada vez melhor com o uso.

**Preciso revisar toda importação manualmente?**
Só as transações que o sistema não conseguiu categorizar automaticamente (ele mostra quantas ficaram "sem categoria" em cada importação). O resto já entra pronto.

## Orçamentos

**Como funciona a meta de gasto por categoria?**
Você define um limite de gasto pra uma categoria de despesa num mês específico. O sistema soma automaticamente tudo que você já gastou naquela categoria naquele mês (a partir das transações reais, não é algo que você preenche à mão) e mostra o progresso numa barra.

**O que acontece se eu ultrapassar o limite?**
A barra de progresso muda visualmente pra indicar que o limite foi estourado — é só um alerta visual, não bloqueia você de continuar lançando despesas naquela categoria.

## Estimativa de imposto

**A estimativa de imposto substitui um contador?**
Não. É uma estimativa simplificada e educacional, baseada em alíquotas de referência por regime tributário — útil pra ter uma ideia de quanto guardar, mas não substitui orientação contábil profissional (há um aviso fixo na própria tela sobre isso).

**Como a alíquota sugerida é calculada?**
Depende do regime: MEI e Simples Nacional usam uma alíquota fixa de referência, Lucro Presumido uma alíquota efetiva aproximada pra serviços, Autônomo segue a tabela progressiva mensal do IRPF/carnê-leão (quanto maior a receita do mês, maior a faixa), e "Outro" não sugere nada — você preenche a alíquota manualmente. A sugestão é só um ponto de partida — você pode ajustar o valor antes de salvar.

## Dashboard

**Os gráficos do Dashboard atualizam em tempo real?**
Eles refletem os dados assim que você entra ou volta pra tela — não há atualização automática em segundo plano enquanto você está com a tela aberta. Criar/editar uma transação em outra tela atualiza o Dashboard na próxima vez que você o visitar.

**Por que os gráficos aparecem em momentos diferentes, um de cada vez?**
De propósito: cada gráfico busca seus próprios dados de forma independente, então ele aparece assim que sua informação chega, em vez de a tela inteira esperar tudo ficar pronto pra mostrar qualquer coisa.

**O que é a projeção de fluxo de caixa?**
Uma estimativa de como seu saldo deve evoluir nos próximos meses: pra meses em que você já tem transações futuras lançadas (ex: uma fatura já agendada), usa esse valor real; pra meses sem nada lançado ainda, usa a média do que entrou/saiu nos últimos 3 meses. No gráfico, o histórico real aparece em linha sólida e a projeção em linha tracejada.
