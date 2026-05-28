package com.file.gen.designacoes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Parte(

        @NotNull( message = "Deve informar se é na sala A" )
        Boolean isSalaA,

        @NotBlank( message = "Deve informar o nome principal da parte")
        String nmPrincipal,

        ///@NotBlank( message = "Deve informar o nome do ajundante da parte")
        String nmAjudante,

        @NotNull( message = "Deve informar o número da parte")
        Integer codigoParte
) {}
