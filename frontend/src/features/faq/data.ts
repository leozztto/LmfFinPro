import faqContent from './docs/FAQ.md?raw'
import { parseFaq } from './utils/parseFaq'

export const FAQ_DATA = parseFaq(faqContent)
