package com.file.gen.designacoes.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DesignacaoRequest(
        @NotEmpty( message = "Deve informar as partes da semana")
        List<ParteSemana> parteSemana ) {
}
