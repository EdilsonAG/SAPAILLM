package com.example.demo.domain.dto;

public record ResponseIA(
        String resposta,            // o que mostrar ao usuário
        CriarTicketStatus status,
        String description,         // título curto do ticket (só quando PRONTO)
        String longText,            // corpo consolidado (só quando PRONTO)
        String priority,            // BAIXA | MEDIA | ALTA
        String type                 // SAP | HARDWARE | ACESSO | ...
) {}