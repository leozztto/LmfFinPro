package com.lmf.finpro.infrastructure.mail;

import com.lmf.finpro.domain.model.ActivationEmailKind;
import com.lmf.finpro.domain.port.out.ActivationMailerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RequiredArgsConstructor
public class SmtpActivationMailer implements ActivationMailerPort {

    private final JavaMailSender mailSender;
    private final String from;
    private final String appUrl;

    @Override
    public void send(String toEmail, String userName, ActivationEmailKind kind) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject(subject(kind));
        message.setText(buildText(userName, kind));
        MailSending.send(mailSender, message, "activation-" + kind.name().toLowerCase());
    }

    static String subject(ActivationEmailKind kind) {
        return switch (kind) {
            case WELCOME -> "Bem-vindo ao FinPro — veja seu primeiro resultado em minutos";
            case FIRST_IMPORT_REMINDER -> "Falta um passo para ver seu dinheiro organizado";
            case WEEK_ONE_CHECK_IN -> "Podemos ajudar você a começar no FinPro?";
        };
    }

    String buildText(String userName, ActivationEmailKind kind) {
        String greeting = "Olá, %s!%n%n".formatted(userName);
        String body =
                switch (kind) {
                    case WELCOME ->
                            """
                            Sua conta no FinPro está pronta. O caminho mais curto para ver valor:

                              1. Crie sua primeira conta (corrente, cartão ou carteira).
                              2. Importe o extrato do seu banco (CSV ou OFX).
                              3. Veja na hora quanto entrou, quanto saiu e em que você mais gasta.

                            O guia passo a passo também mostra lançamentos, orçamentos, recorrências,
                            calendário, dashboard e relatórios.

                            Abra o guia (leva poucos minutos): %s/primeiros-passos
                            """;
                    case FIRST_IMPORT_REMINDER ->
                            """
                            Você criou sua conta ontem, mas ainda não trouxe nenhum lançamento.

                            Baixe o extrato do último mês no app ou no site do seu banco (CSV ou OFX)
                            e importe no FinPro: o resultado aparece em seguida, já separado por
                            categoria.

                            Continue o guia de onde parou: %s/primeiros-passos
                            """;
                    case WEEK_ONE_CHECK_IN ->
                            """
                            Faz uma semana que você se cadastrou e ainda não há lançamentos na sua
                            conta. Se algo travou no caminho (formato do arquivo, dúvida sobre o
                            banco, qualquer coisa), fale com a gente em
                            %s/suporte.

                            Se preferir retomar sozinho, o guia está aqui: %s/primeiros-passos
                            """;
                };
        String footer =
                """

                Você recebe estes e-mails só nos primeiros dias de uso. Para parar de recebê-los,
                desative em Configurações > Notificações: %s/configuracoes/notificacoes

                Equipe FinPro
                """;
        return greeting + body.replace("%s", appUrl) + footer.formatted(appUrl);
    }
}
