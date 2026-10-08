import { LEGAL_ENTITY } from '../legalEntity'
import { LegalDocumentLayout, LegalList, LegalSection } from './LegalDocumentLayout'

/**
 * Política de Privacidade (minuta), alinhada à LGPD (Lei 13.709/2018). Ao alterar este texto, mude
 * `PRIVACY_VERSION` em `LegalDocuments.java`. Os fatos técnicos (BCrypt, cookie httpOnly, ViaCEP,
 * SMTP, push) vêm do código; revise-os sempre que a infraestrutura mudar.
 */
export function PrivacyPage() {
  return (
    <LegalDocumentLayout
      title="Política de Privacidade do FinPro"
      document="privacy"
      summary="Usamos seus dados só para fazer o FinPro funcionar. Não vendemos dados. Você pode baixar tudo e excluir a conta pelo próprio aplicativo, em Configurações → Privacidade e dados."
    >
      <LegalSection title="1. Quem é o controlador dos seus dados">
        <p>
          {LEGAL_ENTITY.name} (CNPJ {LEGAL_ENTITY.cnpj}), com sede em {LEGAL_ENTITY.address}, decide como os seus
          dados pessoais são tratados no FinPro. O encarregado pelo tratamento de dados pessoais é{' '}
          {LEGAL_ENTITY.dpoName}, contato: {LEGAL_ENTITY.privacyEmail}.
        </p>
      </LegalSection>

      <LegalSection title="2. Quais dados tratamos">
        <LegalList>
          <li>
            <strong>Cadastro:</strong> nome, e-mail, CPF ou CNPJ, telefone, endereço, regime tributário e foto
            de perfil (opcional).
          </li>
          <li>
            <strong>Dados financeiros que você lança:</strong> contas, saldos, lançamentos, transferências,
            categorias, clientes, orçamentos, metas, dívidas, investimentos, recorrências e extratos
            importados.
          </li>
          <li>
            <strong>Anexos:</strong> comprovantes e notas fiscais que você envia (PDF ou imagem).
          </li>
          <li>
            <strong>Grupos compartilhados:</strong> nome do grupo, quem participa e quem criou cada lançamento.
          </li>
          <li>
            <strong>Segurança e funcionamento:</strong> sessões de acesso (guardamos apenas um código
            irreversível), registros técnicos de uso (identificador da requisição e da conta, sem os valores
            dos seus lançamentos) e controle de tentativas de acesso por endereço IP.
          </li>
          <li>
            <strong>Preferências e notificações:</strong> alertas escolhidos e, se você ativar, o identificador
            do navegador que recebe as notificações push.
          </li>
          <li>
            <strong>Aceite dos documentos:</strong> qual versão dos Termos e desta Política você aceitou e quando.
          </li>
        </LegalList>
      </LegalSection>

      <LegalSection title="3. Para que usamos e em que base legal">
        <LegalList>
          <li>
            <strong>Prestar o serviço que você contratou</strong> (criar a conta, mostrar seus lançamentos,
            calcular impostos e relatórios): execução de contrato, art. 7º, V, da LGPD.
          </li>
          <li>
            <strong>Segurança e prevenção a fraudes</strong> (limitar tentativas de acesso, registrar eventos
            técnicos): legítimo interesse, art. 7º, IX.
          </li>
          <li>
            <strong>Cumprir obrigações legais e defender direitos</strong> (guardar o registro de aceite dos
            Termos, atender autoridades): art. 7º, II e VI.
          </li>
          <li>
            <strong>Alertas por e-mail e notificações push</strong>: com a sua escolha nas configurações,
            que você pode desligar quando quiser (consentimento, art. 7º, I).
          </li>
        </LegalList>
        <p>Não usamos seus dados financeiros para publicidade e não os vendemos.</p>
      </LegalSection>

      <LegalSection title="4. Com quem compartilhamos">
        <LegalList>
          <li>
            <strong>Outros membros dos grupos que você escolher:</strong> veem o que for lançado no grupo.
            Seu espaço pessoal não é visível a ninguém.
          </li>
          <li>
            <strong>Prestadores que nos ajudam a operar</strong> (operadores): hospedagem e armazenamento
            [preencher: provedor e região, por exemplo AWS], envio de e-mails [preencher: provedor, por exemplo
            Amazon SES] e consulta de endereço pelo CEP (ViaCEP, que recebe apenas o CEP digitado). As
            cotações de moedas do Banco Central não recebem dados seus.
          </li>
          <li>
            <strong>Serviço de notificação do seu navegador:</strong> se você ativar o push, o fabricante do
            navegador entrega a notificação.
          </li>
          <li>
            <strong>Autoridades</strong>, quando a lei ou uma ordem judicial exigir.
          </li>
        </LegalList>
        <p>
          [Preencher: se algum operador armazenar dados fora do Brasil, informar o país e a base da
          transferência internacional, conforme os arts. 33 a 36 da LGPD.]
        </p>
      </LegalSection>

      <LegalSection title="5. Por quanto tempo guardamos">
        <LegalList>
          <li>
            <strong>Enquanto sua conta existir,</strong> mantemos os dados para prestar o serviço.
          </li>
          <li>
            <strong>Ao excluir a conta,</strong> apagamos do banco de dados e do armazenamento, de imediato,
            seus dados cadastrais, o seu espaço pessoal inteiro (com anexos e foto) e os grupos compartilhados
            em que só você participa. Em grupos com outras pessoas, os lançamentos continuam com o grupo,
            sem o seu nome.
          </li>
          <li>
            <strong>Cópias de segurança</strong> podem conter os dados por até [preencher: prazo, por exemplo
            30 dias] depois da exclusão, e então são descartadas. Elas não são usadas para restaurar contas
            excluídas.
          </li>
          <li>
            <strong>Registros técnicos</strong> são mantidos por até [preencher: prazo], conforme o Marco Civil
            da Internet, art. 15, quando aplicável.
          </li>
          <li>
            Guardamos apenas o registro de que a conta foi excluída (um número interno e a data, sem dados
            pessoais) e, se necessário para nos defender em processos, o comprovante de aceite pelo prazo
            legal.
          </li>
        </LegalList>
      </LegalSection>

      <LegalSection title="6. Como protegemos">
        <LegalList>
          <li>Senhas guardadas com hash irreversível (BCrypt); nunca em texto aberto.</li>
          <li>
            Sessão com código de acesso de curta duração e cookie protegido que o JavaScript do navegador não
            lê; trocar a senha encerra as outras sessões.
          </li>
          <li>Limite de tentativas de login, cadastro e redefinição de senha.</li>
          <li>Comunicação protegida por HTTPS e acesso aos dados restrito à sua conta e aos seus grupos.</li>
        </LegalList>
        <p>
          Nenhum sistema é totalmente imune a falhas. Se houver um incidente que possa causar risco ou dano
          relevante a você, avisaremos você e a ANPD nos termos da lei.
        </p>
      </LegalSection>

      <LegalSection title="7. Seus direitos (LGPD, art. 18)">
        <p>Você pode, a qualquer momento e sem custo:</p>
        <LegalList>
          <li>
            <strong>Confirmar e acessar</strong> os seus dados, e <strong>levá-los com você</strong> (portabilidade):
            Configurações → Privacidade e dados → Baixar meus dados. Você recebe um arquivo ZIP com tudo, em
            formato legível por máquina (JSON), mais os anexos e a foto.
          </li>
          <li>
            <strong>Corrigir</strong> dados incompletos ou desatualizados: Configurações → Dados cadastrais.
          </li>
          <li>
            <strong>Eliminar</strong> os dados e encerrar a conta: Configurações → Privacidade e dados →
            Excluir minha conta.
          </li>
          <li>
            <strong>Revogar o consentimento</strong> às notificações em Configurações → Notificações. Os Termos
            e esta Política são condição para usar o FinPro; retirar o aceite equivale a excluir a conta.
          </li>
          <li>
            <strong>Saber com quem compartilhamos</strong> os dados (seção 4), <strong>pedir informações</strong> e
            <strong> se opor</strong> a um tratamento: escreva para {LEGAL_ENTITY.privacyEmail}.
          </li>
          <li>
            Reclamar à Autoridade Nacional de Proteção de Dados (ANPD) caso entenda que seus direitos não foram
            respeitados.
          </li>
        </LegalList>
        <p>Respondemos aos pedidos feitos por e-mail em até [preencher: prazo, por exemplo 15 dias].</p>
      </LegalSection>

      <LegalSection title="8. Cookies e armazenamento no navegador">
        <p>
          Usamos um cookie estritamente necessário para manter você conectado, e o armazenamento local do
          navegador para lembrar seu nome, o tema escolhido e o grupo ativo. Não usamos cookies de publicidade
          nem de rastreamento de terceiros.
        </p>
      </LegalSection>

      <LegalSection title="9. Crianças e adolescentes">
        <p>O FinPro é destinado a maiores de 18 anos e não coleta dados de menores de forma intencional.</p>
      </LegalSection>

      <LegalSection title="10. Alterações desta Política">
        <p>
          Podemos atualizar esta Política. Quando a mudança for relevante, você verá uma tela pedindo a
          leitura e o novo aceite no próximo acesso. A data de vigência está no topo desta página.
        </p>
      </LegalSection>

      <LegalSection title="11. Contato">
        <p>
          Encarregado: {LEGAL_ENTITY.dpoName}. E-mail: {LEGAL_ENTITY.privacyEmail}.
        </p>
      </LegalSection>
    </LegalDocumentLayout>
  )
}
