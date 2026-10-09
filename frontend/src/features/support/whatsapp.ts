import { LEGAL_ENTITY } from '@/features/legal/legalEntity'

const WHATSAPP_MESSAGE = 'Olá! Preciso de ajuda com o FinPro.'

/** Conversa no WhatsApp (app no celular, WhatsApp Web no computador): DDI + DDD + número, com mensagem inicial. */
export const WHATSAPP_HREF = `https://wa.me/55${LEGAL_ENTITY.phone.replace(/\D/g, '')}?text=${encodeURIComponent(WHATSAPP_MESSAGE)}`
