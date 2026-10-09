import { useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { authLinkClassName } from '@/features/auth/components/AuthPageShell'
import { LEGAL_ENTITY } from '../legalEntity'
import { LegalDocumentLayout, LegalList, LegalSection } from './LegalDocumentLayout'

/**
 * Termos de Uso (minuta). Ao alterar este texto, mude `TERMS_VERSION` em `LegalDocuments.java`.
 */
export function TermsPage() {
  const { hash } = useLocation()

  // Num app de página única o navegador não rola até a âncora (ex.: /termos#aviso-fiscal) sozinho.
  useEffect(() => {
    if (hash) document.getElementById(hash.slice(1))?.scrollIntoView()
  }, [hash])

  return (
    <LegalDocumentLayout
      title="Termos de Uso do FinPro"
      document="terms"
      summary="O FinPro organiza suas finanças e estima impostos e pró-labore, mas essas estimativas não substituem um contador: confirme os valores com ele antes de pagar. Você cuida da sua senha e dos dados que lança. Você pode baixar tudo o que é seu e excluir a conta quando quiser."
    >
      <LegalSection title="1. Quem somos e o que é o FinPro">
        <p>
          O FinPro é um aplicativo de controle financeiro para autônomos, freelancers e pequenos negócios,
          oferecido por {LEGAL_ENTITY.name} (CNPJ {LEGAL_ENTITY.cnpj}), com sede em {LEGAL_ENTITY.address}. Estes
          Termos regulam o uso do aplicativo. Ao criar uma conta ou continuar usando o FinPro depois de uma nova
          versão destes Termos, você declara que leu e concorda com eles.
        </p>
      </LegalSection>

      <LegalSection title="2. Conta e cadastro">
        <LegalList>
          <li>Você precisa ter 18 anos ou mais e informar dados verdadeiros, completos e atualizados.</li>
          <li>
            O e-mail e a senha são pessoais. Guarde a senha em segredo: o que for feito com ela é de sua
            responsabilidade. Se suspeitar de acesso indevido, troque a senha em Configurações e avise-nos.
          </li>
          <li>Cada CPF/CNPJ e cada e-mail podem ter uma única conta.</li>
        </LegalList>
      </LegalSection>

      <LegalSection title="3. O que o FinPro faz e o que não faz">
        <p>
          O FinPro é uma ferramenta de organização. As projeções de fluxo de caixa e os relatórios são{' '}
          <strong>referências calculadas a partir do que você lançou</strong>. Não são consultoria contábil,
          fiscal, jurídica ou de investimentos.
        </p>
        <p>
          A exatidão dos resultados depende da exatidão e da completude dos dados lançados ou importados por
          você. Confira os valores antes de tomar decisões financeiras.
        </p>
      </LegalSection>

      <LegalSection id="aviso-fiscal" title="4. Aviso fiscal: estimativas de imposto e pró-labore">
        <p>
          <strong>
            As estimativas de imposto e o cálculo de pró-labore do FinPro não substituem um contador.
          </strong>{' '}
          São simplificações feitas com alíquotas, tabelas e limites de referência, vigentes na data do
          cálculo, e com base apenas no que você lançou no aplicativo.
        </p>
        <LegalList>
          <li>
            Elas não consideram toda a sua situação: atividades, anexos e fator R do Simples Nacional, créditos,
            retenções na fonte, deduções, benefícios, receitas e despesas lançadas fora do FinPro, regras
            municipais e estaduais, entre outros pontos que mudam o valor devido.
          </li>
          <li>
            As regras tributárias mudam, e o aplicativo pode demorar a refletir uma mudança. O valor calculado
            pode diferir do valor realmente devido, para mais ou para menos.
          </li>
          <li>
            O FinPro não apura nem transmite guias, declarações ou obrigações acessórias, e não faz o seu
            enquadramento tributário. Quem define o regime, o valor do pró-labore e o que recolher é você, com a
            orientação do seu contador.
          </li>
          <li>
            Antes de pagar qualquer tributo, contribuição (como INSS e IRRF) ou transferir pró-labore, confirme os
            valores com um contador. Não respondemos por multas, juros, diferenças de tributo ou outras
            consequências de decisões tomadas com base nessas estimativas.
          </li>
        </LegalList>
      </LegalSection>

      <LegalSection title="5. Seus dados">
        <LegalList>
          <li>Os dados financeiros que você lança, importa ou anexa continuam sendo seus.</li>
          <li>
            Você nos autoriza a armazená-los e processá-los apenas para prestar o serviço, nos termos da{' '}
            <Link to="/privacidade" className={authLinkClassName}>
              Política de Privacidade
            </Link>
            .
          </li>
          <li>
            Você pode baixar uma cópia completa dos seus dados e excluir a conta a qualquer momento em
            Configurações → Privacidade e dados.
          </li>
        </LegalList>
      </LegalSection>

      <LegalSection title="6. Grupos compartilhados (casal ou família)">
        <LegalList>
          <li>
            Ao entrar em um grupo compartilhado, todos os membros veem e editam as contas e os lançamentos do
            grupo. Compartilhe apenas o que quiser que as outras pessoas vejam.
          </li>
          <li>
            Quem cria o grupo é o dono: convida e remove membros e transfere a posse. O dono não pode excluir a
            própria conta enquanto houver outros membros; antes, deve transferir a posse.
          </li>
          <li>
            Se um membro sair ou excluir a conta, os dados que ele lançou ou trouxe para o grupo permanecem com
            o grupo, sem o nome dele como autor. Para levar suas contas embora, devolva-as ao seu espaço
            pessoal antes de sair.
          </li>
          <li>Quem tem o link de convite pode entrar no grupo; trate o link como uma chave de acesso.</li>
        </LegalList>
      </LegalSection>

      <LegalSection title="7. Uso adequado">
        <p>É proibido:</p>
        <LegalList>
          <li>usar o FinPro para fins ilícitos ou para lançar dados de terceiros sem autorização;</li>
          <li>tentar acessar contas, sistemas ou dados que não são seus, ou burlar limites e proteções;</li>
          <li>enviar arquivos com vírus ou conteúdo que viole direitos de terceiros;</li>
          <li>copiar, revender ou fazer engenharia reversa do aplicativo, salvo quando a lei permitir.</li>
        </LegalList>
        <p>
          Podemos suspender ou encerrar contas que violem estes Termos, avisando sempre que possível e
          preservando o seu direito de baixar os dados.
        </p>
      </LegalSection>

      <LegalSection title="8. Disponibilidade e mudanças no serviço">
        <p>
          Trabalhamos para manter o FinPro disponível, mas não garantimos funcionamento ininterrupto: pode haver
          manutenções, falhas e indisponibilidade de serviços de terceiros (como cotações e consulta de CEP).
          Funcionalidades podem ser alteradas, acrescentadas ou retiradas, e avisaremos mudanças relevantes.
        </p>
      </LegalSection>

      <LegalSection title="9. Planos e pagamento">
        <p>
          [Preencher: condições comerciais do serviço: gratuidade ou período de teste, planos, preços, forma de
          cobrança, renovação, cancelamento e reembolso. Se o serviço for gratuito no lançamento, informar que a
          cobrança poderá ser introduzida mediante aviso prévio e sem efeito retroativo.]
        </p>
      </LegalSection>

      <LegalSection title="10. Propriedade intelectual">
        <p>
          O aplicativo, a marca, o código, o desenho das telas e os textos são de {LEGAL_ENTITY.name} ou de seus
          licenciantes. Estes Termos não transferem nenhum direito sobre eles além do uso pessoal do serviço.
        </p>
      </LegalSection>

      <LegalSection title="11. Limitação de responsabilidade">
        <p>
          Na extensão permitida pela lei, não respondemos por perdas indiretas, lucros cessantes ou decisões
          tomadas com base nos cálculos e nos relatórios do FinPro (inclusive multas, juros e diferenças de
          tributo decorrentes das estimativas descritas no Aviso fiscal), nem por danos causados por uso indevido da
          conta, perda de senha ou falhas de serviços de terceiros. Nada aqui limita direitos que a lei garante
          ao consumidor.
        </p>
      </LegalSection>

      <LegalSection title="12. Encerramento da conta">
        <p>
          Você pode encerrar a conta quando quiser, em Configurações → Privacidade e dados. A exclusão é
          definitiva: os seus dados são apagados dos nossos sistemas, conforme descrito na Política de
          Privacidade, e não podem ser recuperados.
        </p>
      </LegalSection>

      <LegalSection title="13. Alterações destes Termos">
        <p>
          Podemos atualizar estes Termos. Quando a mudança for relevante, você verá uma tela pedindo a leitura e
          o novo aceite no próximo acesso. Guardamos a data e a versão de cada aceite. Se não concordar com a
          nova versão, você pode baixar seus dados e excluir a conta.
        </p>
      </LegalSection>

      <LegalSection title="14. Lei aplicável e foro">
        <p>
          Estes Termos seguem as leis do Brasil. Fica eleito o foro de {LEGAL_ENTITY.forum}, ressalvado o foro do
          domicílio do consumidor quando a lei assim determinar.
        </p>
      </LegalSection>

      <LegalSection title="15. Contato">
        <p>
          Dúvidas sobre estes Termos ou sobre o serviço: {LEGAL_ENTITY.privacyEmail} ou {LEGAL_ENTITY.phone}.
        </p>
      </LegalSection>
    </LegalDocumentLayout>
  )
}
