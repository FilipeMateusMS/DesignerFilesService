package com.file.gen.designacoes.controller;

import com.file.gen.designacoes.dto.DesignacaoRequest;
import com.file.gen.designacoes.service.DesinacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/designacoes")
@RequiredArgsConstructor
public class DesignacaoController {

    private final DesinacaoService desinacaoService;

    @PostMapping
    public ResponseEntity<Void> gerarDesinacoes(@RequestBody @Valid DesignacaoRequest designacoes ) throws Exception {
        desinacaoService.gerar( designacoes );
        return ResponseEntity.ok().build();
    }
}
