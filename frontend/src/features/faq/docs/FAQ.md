# FinPro — Perguntas Frequentes (FAQ)
___

*Dúvidas comuns de quem usa o FinPro no dia a dia.*
___

## Conta e acesso

**Como eu crio uma conta no FinPro?**
Na tela de login, clique no link **Cadastre-se**. Na tela de cadastro você informa nome e sobrenome, e-mail, regime tributário, CPF ou CNPJ, telefone (opcional), endereço e senha (mínimo 8 caracteres, com confirmação), além de aceitar os termos de uso. O CEP preenche automaticamente rua, bairro, cidade e estado — só confira e complete o número (e o complemento, se houver).

**Preciso informar CPF ou CNPJ?**
Depende do regime tributário escolhido: **CNPJ** para MEI, Simples Nacional e Lucro Presumido; **CPF** para Autônomo e Outro. O sistema valida o documento (inclusive os dígitos verificadores) e não deixa cadastrar um CPF com um regime de pessoa jurídica (ou vice-versa). O e-mail e o CPF/CNPJ não podem já estar em uso por outra conta.

**Esqueci minha senha, como recupero?**
Na tela de login, clique em **Esqueceu sua senha?** e informe o e-mail da sua conta. Você recebe um e-mail com um link para criar uma nova senha (mínimo 8 caracteres, com confirmação). Depois de salvar, é só entrar com a senha nova.

**Não recebi o e-mail de redefinição de senha — e agora?**
Confira a caixa de spam e se o e-mail informado é o mesmo do cadastro. Por segurança, o sistema mostra a mesma mensagem exista ou não uma conta com aquele e-mail, então um e-mail digitado errado não gera aviso. Se ainda assim não chegar, peça um novo link.

**O link de redefinição diz que é inválido ou expirado. Por quê?**
O link vale por 30 minutos e só pode ser usado uma vez. Além disso, pedir um novo link cancela o anterior — só o e-mail mais recente funciona. Nesses casos, é só solicitar outro link.

**Posso alterar meus dados cadastrais depois?**
Sim. Clique no ícone de usuário no canto superior direito, depois em **Configurações** → **Dados cadastrais**. Dá pra alterar nome, e-mail, regime tributário, CPF/CNPJ, telefone e endereço, com as mesmas regras do cadastro — inclusive o documento precisa combinar com o regime escolhido. Para trocar o **e-mail**, que é o seu login, o sistema pede a senha atual.

**Como troco minha senha estando logado?**
Clique no ícone de usuário no canto superior direito, depois em **Configurações** → **Alterar senha**: informe a senha atual e a nova (mínimo 8 caracteres). Você continua conectado nesse navegador, mas as outras sessões abertas (outros navegadores e aparelhos) são encerradas.

**Fui desconectado do nada no meio do uso — o que aconteceu?**
Seu login expira automaticamente depois de 1 hora (por segurança). Também acontece quando a senha da conta é redefinida: todas as sessões abertas (outros navegadores, celular, etc.) são encerradas na hora. Em qualquer um dos casos, você é redirecionado pra tela de login com um aviso explicando que a sessão expirou — é só entrar de novo. Para sair manualmente, clique no ícone de usuário no canto superior direito e depois em **Sair**.

**Troquei a senha e fui deslogado em outro aparelho. Isso é normal?**
Sim, é de propósito. Redefinir a senha encerra todas as sessões que estavam abertas — assim, se alguém tinha acesso à sua conta com a senha antiga, perde o acesso imediatamente. Basta entrar de novo com a senha nova.

**Meus dados financeiros são visíveis para outros usuários?**
Não. Toda informação (contas, transações, clientes, etc.) é isolada por usuário — mesmo tentando acessar um registro de outra pessoa diretamente pela URL, o sistema trata como se ele não existisse.

**Dá pra usar o sistema no modo escuro?**
Sim. Use o botão de tema no topo da tela (também disponível na tela de login). Na primeira visita o sistema segue a preferência do seu sistema operacional, e depois lembra a sua escolha neste navegador.
___

## Contas bancárias

**Qual a diferença entre os tipos de conta?**
São só categorias pra você organizar: **Conta corrente**, **Poupança** e **Carteira** (dinheiro físico/uso avulso). Não há diferença de comportamento entre elas — todas funcionam igual, é só pra você identificar visualmente.

**Posso mudar o saldo inicial de uma conta depois de criada?**
Não. O saldo inicial é fixado na criação e não pode ser editado depois — na edição, só dá pra mudar nome e tipo (o saldo inicial e o saldo atual aparecem apenas para consulta). Isso é de propósito: o saldo *atual* é sempre calculado a partir do saldo inicial mais todas as transações já pagas, nunca é um número editável à parte, então deixar o saldo inicial mutável abriria brecha pra ele ficar inconsistente com o histórico.

**Por que não consigo excluir uma conta?**
Uma conta com transações ou transferências vinculadas não pode ser excluída — o sistema bloqueia pra evitar perder histórico financeiro sem querer. Remova as transações (e as transferências, na tela de Transferências) primeiro.
___

## Categorias

**O que são categorias "padrão do sistema"?**
É um conceito que existe no modelo de dados (categorias sem dono, visíveis pra todo mundo), mas hoje **nenhuma categoria vem pré-cadastrada** — toda categoria que você vê foi criada por você mesmo. Todas as suas categorias podem ser editadas e removidas livremente (respeitando as regras abaixo).

**Posso mudar uma categoria de receita para despesa (ou vice-versa) depois de criada?**
Não. O tipo é fixado na criação (na edição, só nome e cor podem ser alterados). Se você errou o tipo, crie uma categoria nova com o tipo certo. Como hoje não é possível editar transações, as transações já lançadas na categoria errada precisam ser excluídas e lançadas de novo.

**Por que não consigo excluir uma categoria?**
Categoria com transações vinculadas não pode ser excluída, pelo mesmo motivo das contas: evitar perder histórico financeiro. Exclua as transações dela antes de remover a categoria.

**O que acontece com regras e orçamentos quando excluo uma categoria?**
Eles são removidos junto. As regras de categorização automática e os orçamentos ligados àquela categoria não impedem a exclusão — são apagados automaticamente com ela.
___

## Transações (receitas e despesas)

**Como eu lanço uma receita ou despesa?**
Na tela de Transações, clique no botão **+** (Nova transação): escolha a conta, opcionalmente uma categoria e um cliente, o tipo, a data, a descrição e o valor (sempre positivo). O tipo (receita/despesa) define o sinal — não existe "valor negativo" no formulário. Ao escolher uma categoria, o tipo é preenchido automaticamente com o tipo dela e fica travado. É preciso ter ao menos uma conta cadastrada antes de lançar transações.

**Dá pra editar uma transação depois de criada?**
Hoje não — a tela de Transações só permite **criar** e **excluir**, não tem opção de editar. Se você errou algo, exclua e lance de novo. A única exceção são as transações importadas de extrato: categoria e cliente delas podem ser ajustados na tela de revisão da importação.

**Como encontro uma transação específica?**
Use os filtros da tela de Transações: conta, categoria, cliente, tipo, situação (paga ou pendente) e período (de/até). A lista é sempre ordenada da data mais recente para a mais antiga.

**Por que uma transação aparece com o botão de excluir desabilitado?**
Ela faz parte de uma transferência (tem a etiqueta "Transferência"). Transações desse tipo só podem ser removidas junto com a transferência inteira, na tela de Transferências — isso porque toda transferência sempre lança duas transações (uma em cada conta) e as duas precisam sumir juntas pra manter os saldos corretos.

**Qual a diferença entre transação paga e pendente?**
Paga é dinheiro que já entrou ou saiu da conta; pendente é algo **a receber** ou **a pagar** (ex.: uma nota emitida que o cliente ainda não pagou, ou um boleto com vencimento futuro). O **saldo atual** das contas considera só as pagas. As pendentes aparecem no Dashboard nos cards "A receber" e "A pagar" e entram no **saldo previsto**. Relatórios, orçamentos e a estimativa de imposto contam as duas, pela data do lançamento.

**Como a situação é definida quando lanço uma transação?**
No campo "Situação" do formulário, a opção **Automática** deixa pendente o que tem data futura e marca como paga o que é de hoje ou do passado; você também pode escolher Paga ou Pendente manualmente. Transações importadas de extrato e transferências são sempre pagas. As ocorrências de lançamentos recorrentes entram como pendentes até você confirmar.

**Como marco uma transação como paga?**
Pelo botão com ícone de check no próprio card da transação (no celular, abra os detalhes do card). O mesmo botão, numa transação paga, volta ela para pendente. Transações de transferência não têm esse botão, porque são sempre pagas.
___

## Transferências

**Como faço uma transferência entre minhas contas?**
Na tela de Transferências, clique no botão **+** (Nova transferência) e informe conta de origem, conta de destino (têm que ser diferentes), valor, data e uma descrição opcional. O sistema não deixa transferir mais do que o saldo disponível na conta de origem.

**Por que uma transferência gera duas transações?**
Porque, financeiramente, uma transferência é uma saída de uma conta e uma entrada em outra ao mesmo tempo — o sistema modela isso como duas transações (uma despesa na origem, uma receita no destino) ligadas entre si, pra que o saldo de cada conta continue batendo individualmente.

**Transferências contam como receita ou despesa no Dashboard?**
Não. Elas são excluídas de todo cálculo de receita/despesa (nos cards, gráficos, orçamentos, estimativa de imposto e relatórios consolidados) — afinal, transferir dinheiro entre suas próprias contas não é ganhar nem gastar, só reorganizar onde o dinheiro está. A única exceção é a **Exportação de transações** (em Relatórios), que traz o extrato bruto completo, incluindo transferências.
___

## Lançamentos recorrentes

**O que é um lançamento recorrente?**
É um modelo para receitas ou despesas que se repetem (aluguel, assinaturas, mensalidade fixa de um cliente) com frequência semanal, mensal ou anual. Em cada data, o sistema lança sozinho uma transação real na conta escolhida, com a etiqueta "Recorrente" na tela de Transações. Cada ocorrência entra como **pendente** e só passa a contar no saldo atual quando você a marca como paga. Opcionalmente, você define uma data final.

**Criei uma recorrência com data inicial no passado. O que acontece?**
Todas as ocorrências com data até hoje são lançadas na hora. Por exemplo, um aluguel mensal com início em janeiro, cadastrado em setembro, gera de uma vez as transações de janeiro a setembro. As próximas entram automaticamente no dia de cada uma.

**E se a data inicial for dia 31?**
Nos meses mais curtos, a ocorrência cai no último dia do mês (28 ou 29 de fevereiro, 30 de abril etc.) e volta para o dia 31 nos meses que têm 31 dias.

**O que muda quando edito ou pauso uma recorrência?**
Descrição, valor, categoria, cliente e data final podem ser alterados, e a mudança vale só para as próximas ocorrências; as transações já lançadas não mudam. Conta, tipo, frequência e data inicial ficam fixos. Enquanto a recorrência estiver pausada, nada é lançado. Ao reativar, as datas que caíram durante a pausa são puladas, em vez de lançadas todas de uma vez.

**Excluir uma recorrência apaga as transações que ela já lançou?**
Não. Só o modelo é removido; as transações já lançadas continuam no extrato e podem ser excluídas individualmente na tela de Transações. Uma conta com recorrência vinculada não pode ser excluída: remova a recorrência antes.
___

## Clientes

**Pra que serve cadastrar clientes?**
Pra organizar sua receita por cliente/projeto — o Dashboard mostra um gráfico de "receita por cliente" no mês, e a tela de Relatórios gera recibos e demonstrativos anuais por cliente.

**Quais dados preciso informar para cadastrar um cliente?**
Nome, e-mail, telefone (com DDD), tipo de trabalho (PJ ou Autônomo/Freelancer), tipo de documento (CPF ou CNPJ) e o número do documento, que é validado. Cor e observações (até 1000 caracteres) são opcionais. Você também pode marcar o cliente como ativo ou inativo — isso serve só para organizar e filtrar a lista; clientes inativos continuam disponíveis nos lançamentos.

**Preciso vincular um cliente em toda transação?**
Não, o campo cliente é opcional em qualquer transação. Receitas sem cliente vinculado aparecem agrupadas como "Sem cliente" no gráfico de receita por cliente.

**Por que não consigo excluir um cliente?**
Cliente com transações vinculadas não pode ser excluído, para não perder o histórico de receitas dele. Exclua as transações vinculadas antes — ou, se só quiser tirá-lo da lista do dia a dia, marque-o como inativo.
___

## Importação de extrato

**Que formatos de arquivo o sistema aceita?**
**CSV** e **OFX** — o formato é detectado pela extensão do arquivo (.csv ou .ofx). No CSV, o cabeçalho é fixo `date,description,amount` (data no formato `aaaa-mm-dd`, valor com ponto decimal — positivo é receita, negativo é despesa); extratos em CSV geralmente precisam ser ajustados pra esse formato antes de subir. O OFX é o extrato exportado diretamente pelo banco: cada lançamento (bloco `<STMTTRN>`) vira uma transação, usando a data (`DTPOSTED`), o valor (`TRNAMT`, com o sinal indicando receita ou despesa) e a descrição (`MEMO`, ou `NAME` se o MEMO estiver vazio).

**Como faço uma importação?**
Na tela de Importações, aba **Importar arquivo**, escolha a conta onde as transações serão lançadas, selecione o arquivo e clique em **Importar extrato**. Ao final, o sistema mostra quantas transações foram importadas e quantas ficaram sem categoria. Se o arquivo tiver algum problema (data, valor ou descrição inválidos, ou nenhuma transação), nada é importado e a mensagem indica a linha com erro.

**Se eu importar o mesmo arquivo duas vezes, as transações duplicam?**
Sim. Hoje o sistema não detecta lançamentos repetidos — cada importação lança todas as transações do arquivo. Evite reimportar o mesmo extrato ou períodos que se sobrepõem; se acontecer, exclua as duplicadas na tela de Transações.

**Como funciona a categorização automática na importação?**
O sistema mantém uma lista de regras (padrão de texto → categoria). Ao importar, ele procura, entre as regras de maior peso primeiro, uma cujo padrão esteja **contido** na descrição da transação (sem diferenciar maiúsculas/minúsculas) e cuja categoria seja do mesmo tipo da transação (receita ou despesa). Quando não encontra nenhuma, a transação entra sem categoria — você categoriza manualmente na tela de revisão, e ao fazer isso o sistema **aprende**: reforça a regra existente pra aquela descrição (ou cria uma nova, se ainda não existia), ficando cada vez melhor com o uso.

**Posso cadastrar regras de categorização manualmente?**
Sim. Na tela de Importações, aba **Regras de categorização**, você cria regras informando um padrão de texto e a categoria, e pode remover as que não quiser mais. Remover uma regra não altera transações já importadas — só deixa de categorizar as próximas.

**Preciso revisar toda importação manualmente?**
Só as transações que o sistema não conseguiu categorizar automaticamente (a lista de importações mostra quantas ficaram "sem categoria" em cada uma). Na revisão, além da categoria, você também pode vincular um cliente a cada transação importada.
___

## Orçamentos

**Como funciona a meta de gasto por categoria?**
Na tela de Orçamentos, clique no botão **+** (Novo orçamento) e defina um limite de gasto pra uma categoria de despesa num mês específico. O sistema soma automaticamente tudo que você já gastou naquela categoria naquele mês, em todas as contas (a partir das transações reais, sem contar transferências — não é algo que você preenche à mão), e mostra o progresso numa barra.

**O que acontece se eu ultrapassar o limite?**
A barra de progresso fica vermelha e aparece o aviso "limite ultrapassado" — é só um alerta visual, não bloqueia você de continuar lançando despesas naquela categoria.

**Dá pra alterar o limite de um orçamento?**
Hoje não — orçamentos só podem ser criados e removidos. Para mudar o limite, remova o orçamento e crie outro para o mesmo mês e categoria.
___

## Estimativa de imposto

**A estimativa de imposto substitui um contador?**
Não. É uma estimativa simplificada e educacional, baseada em alíquotas de referência por regime tributário — útil pra ter uma ideia de quanto guardar, mas não substitui orientação contábil profissional (há um aviso fixo na própria tela sobre isso).

**De onde vem a receita bruta do mês?**
Ao escolher o mês de referência, o sistema preenche a receita bruta com a soma das receitas lançadas naquele mês (sem contar transferências). Você pode ajustar o valor livremente antes de salvar — por exemplo, para desconsiderar receitas que não são tributáveis.

**Como a alíquota sugerida é calculada?**
Depende do regime escolhido no formulário: MEI e Simples Nacional usam uma alíquota fixa de referência (6%), Lucro Presumido uma alíquota efetiva aproximada pra serviços (11,33%), Autônomo segue as faixas da tabela progressiva mensal do IRPF/carnê-leão (quanto maior a receita do mês, maior a faixa — de isento a 27,5%, aplicada sobre o total, sem a parcela a deduzir), e "Outro" não sugere nada — você preenche a alíquota manualmente. A sugestão é só um ponto de partida — você pode ajustar o valor antes de salvar, e o valor estimado é recalculado na hora.
___

## Relatórios

**Quais relatórios o sistema gera?**
Na tela de Relatórios você escolhe o tipo de relatório e o formato do arquivo. Os tipos disponíveis são: **Recibo por cliente** (receitas do cliente no mês, com dados de emissor e cliente), **Extrato de conta** (saldo de abertura, movimentações do mês e saldo final), **Demonstrativo anual por cliente** (total recebido do cliente em cada mês do ano), **Despesas por categoria** (total gasto por categoria no mês, somando todas as contas), **Resultado do período (DRE)** (receita, despesa e resultado consolidados por mês, trimestre ou ano), **Orçamento vs. realizado** (limite de cada orçamento do mês comparado ao gasto real) e **Exportação de transações** (extrato bruto de todas as transações do mês, com a situação de cada uma: paga ou pendente).

**Em quais formatos posso baixar os relatórios?**
Todos os relatórios podem ser baixados em **PDF** ou **CSV**. O CSV usa ponto e vírgula como separador e codificação compatível com o Excel em português, então abre direto no Excel ou no Google Sheets sem precisar configurar nada.

**Os relatórios incluem transferências entre contas?**
Os relatórios consolidados (Despesas por categoria, DRE e Orçamento vs. realizado) não incluem transferências, pelo mesmo motivo do Dashboard. Já a **Exportação de transações** inclui, porque o objetivo dela é auditar o extrato completo. O **Extrato de conta** mostra todas as movimentações da conta, incluindo as transferências de entrada e saída, para que o saldo final bata com o saldo real.

**Por que meu recibo ou demonstrativo saiu sem valores?**
Recibo e demonstrativo anual consideram só as **receitas vinculadas àquele cliente** no período. Receitas lançadas sem cliente (ou despesas) não entram — vincule o cliente no lançamento (ou na revisão da importação) para que elas apareçam.

**O que aparece como "Sem categoria" ou "Categoria removida" no relatório de despesas?**
"Sem categoria" agrupa despesas lançadas sem categoria. "Categoria removida" aparece quando a categoria de uma despesa antiga não existe mais — o histórico da transação é preservado mesmo assim.
___

## Dashboard

**O que o Dashboard mostra?**
Cards com o saldo atual (somando todas as contas, só com transações pagas), a receita e a despesa do mês corrente, o total a receber e a pagar (transações pendentes) e o saldo previsto (saldo atual + a receber − a pagar); gráficos de fluxo mensal (pagas e pendentes, pelo mês da transação) e de evolução do saldo nos últimos 6 meses (só transações pagas, então o último ponto bate com o saldo atual); saldo por conta; despesas por categoria, receita por categoria e receita por cliente no mês corrente; e a projeção de fluxo de caixa para os próximos 3 meses.

**Os gráficos do Dashboard atualizam em tempo real?**
Eles refletem os dados quando você entra ou volta pra tela, e também quando você volta para a aba do navegador depois de usar outra janela — mas não há atualização contínua em segundo plano enquanto você está com a tela aberta. Criar ou excluir uma transação em outra tela atualiza o Dashboard na próxima vez que você o visitar.

**Por que os gráficos aparecem em momentos diferentes, um de cada vez?**
De propósito: cada gráfico busca seus próprios dados de forma independente, então ele aparece assim que sua informação chega, em vez de a tela inteira esperar tudo ficar pronto pra mostrar qualquer coisa.

**O que é a projeção de fluxo de caixa?**
Uma estimativa de como seu saldo deve evoluir nos próximos 3 meses, partindo do saldo ao fim do mês atual, já contando o que está pendente até lá: pra meses em que você já tem transações futuras lançadas (ex: uma fatura já agendada), usa o resultado real desses lançamentos; pra meses sem nada lançado ainda, usa a média do resultado (receitas menos despesas) dos últimos 3 meses. Os **lançamentos recorrentes** ativos entram por cima disso, cada ocorrência no mês em que cai (inclusive as que ainda vão cair no mês atual). As transações já lançadas por uma recorrência ativa ficam fora da média, para não serem contadas duas vezes. No gráfico, o histórico real aparece em linha sólida e a projeção em linha tracejada.
___
