import ReactMarkdown from "react-markdown";
import faqContent from "@/features/faq/docs/faq.md?raw";

export function FaqPage() {
    return (
        <div style={{ padding: "24px" }}>
            <ReactMarkdown>
                {faqContent}
            </ReactMarkdown>
        </div>
    );
}