package com.file.gen.designacoes.dto;

import java.time.LocalDate;

public record ParteUnica(

        LocalDate semana,
        String nmPrincipal,
        String nmAjudante,
        Integer codigoParte,
        boolean isSalaA
) {
}