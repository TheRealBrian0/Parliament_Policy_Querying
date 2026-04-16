/** Maps GraphQL Persona enum values to short labels (broad professional domains). */
export const PERSONA_OPTIONS: { value: string; label: string; hint: string }[] = [
  { value: "TRADE_AND_SMALL_BUSINESS", label: "Trade & small business", hint: "Retail, vendors, MSMEs" },
  { value: "INDUSTRY_AND_LOGISTICS", label: "Industry & logistics", hint: "Manufacturing, supply chain, transport" },
  { value: "FINANCE_AND_PROFESSIONAL", label: "Finance & professional", hint: "Banking, law, consulting, corporate" },
  { value: "PUBLIC_AND_COMMUNITY", label: "Public & community", hint: "Govt, NGOs, health, civic" },
  { value: "STUDENT_AND_EDUCATOR", label: "Student & educator", hint: "Schools, universities, research" },
  { value: "GENERAL_CITIZEN", label: "General citizen", hint: "Everyday consumer / public" },
];
