package com.file.gen.designacoes.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ParteSemana(

        @NotEmpty( message = "Deve informar a semana" )
        LocalDate semana,

        @NotEmpty( message = "Deve informar a partes dessa semana" )
        List<Parte> partes
) { }
