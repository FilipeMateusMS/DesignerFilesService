package com.file.gen.designacoes.service;

import com.file.gen.designacoes.dto.DesignacaoRequest;
import com.file.gen.designacoes.dto.Parte;
import com.file.gen.designacoes.dto.ParteSemana;
import com.file.gen.designacoes.dto.ParteUnica;
import com.file.gen.designacoes.util.ZipUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.JodConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Log4j2
public class DesinacaoService2 {

    @Value("${application.folderOutput}")
    private String folderOutput;

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern(
                    "dd 'de' MMMM 'de' yyyy",
                    new Locale("pt", "BR")
            );

    public static String formatar(LocalDate data) {
        String texto = data.format(FORMATADOR_DATA);

        return texto.substring(0, 6)
                + Character.toUpperCase(texto.charAt(6))
                + texto.substring(7);
    }

    public void gerar(DesignacaoRequest request) throws Exception {

        List<ParteUnica> parteSalaA = new ArrayList<>();
        List<ParteUnica> parteSalaB = new ArrayList<>();

        // Separar salas
        for (ParteSemana parteSemana : request.parteSemana()) {

            for (Parte parte : parteSemana.partes()) {

                ParteUnica parteUnica = new ParteUnica(
                        parteSemana.semana(),
                        parte.nmPrincipal(),
                        parte.nmAjudante(),
                        parte.codigoParte(),
                        parte.isSalaA()
                );

                if (Boolean.TRUE.equals(parte.isSalaA())) {
                    parteSalaA.add(parteUnica);
                } else {
                    parteSalaB.add(parteUnica);
                }
            }
        }

        processarSala(
                parteSalaA,
                "templates/SalaADesignacao.odt"
        );
        log.info( "Quantidade de partes da sala A= {}", parteSalaA.size() );

        processarSala(
                parteSalaB,
                "templates/SalaBDesignacao.odt"
        );

        log.info( "Quantidade de partes da sala B= {}", parteSalaB.size() );
    }


    private void processarSala(
            List<ParteUnica> partes,
            String template
    ) throws Exception {

        int contador = 1;

        for (int i = 0; i < partes.size(); i += 4) {

            List<ParteUnica> grupo =
                    partes.subList(
                            i,
                            Math.min(i + 4, partes.size())
                    );

            gerarDocumento(
                    grupo,
                    template,
                    contador++
            );
        }
    }

    private String obterNmArquivo( List<ParteUnica> grupo, int contador ) {

        if (grupo.isEmpty()) return "parte_vazio";
        ParteUnica primeiraParte = grupo.getFirst();

        String sala = primeiraParte.isSalaA()
                        ? "SalaA"
                        : "SalaB";

        StringBuilder nomeArquivo = new StringBuilder(sala)
                .append("_")
                .append(contador)
                .append("_");

        LocalDate ultimaSemana = null;

        for (ParteUnica parte : grupo) {
            if (!parte.semana().equals(ultimaSemana)) {
                nomeArquivo.append("_").append(parte.semana());
                ultimaSemana = parte.semana();
            }
        }
        return nomeArquivo.toString();
    }

    private void gerarDocumento(
            List<ParteUnica> grupo,
            String templatePath,
            int numeroArquivo
    ) throws Exception {

        Path pastaSaida = Path.of(folderOutput);

        String nmArquivo = obterNmArquivo( grupo, numeroArquivo );

        Path odtSaida = pastaSaida.resolve(nmArquivo + ".odt");

        Path pdfSaida =
                pastaSaida.resolve(
                        nmArquivo + ".pdf"
                );

        Files.createDirectories(pastaSaida);

        ClassPathResource resource =
                new ClassPathResource(templatePath);

        Files.copy(
                resource.getInputStream(),
                odtSaida,
                StandardCopyOption.REPLACE_EXISTING
        );

        substituirVariaveisOdt(
                odtSaida,
                grupo
        );

        converterPdf1(
                odtSaida,
                pdfSaida
        );
    }

    private void substituirVariaveisOdt(
            Path odtPath,
            List<ParteUnica> grupo
    ) throws Exception {

        Path tempDir =
                Files.createTempDirectory(
                        "odt-temp"
                );

        ZipUtils.unzip(
                odtPath,
                tempDir
        );

        Path contentXml =
                tempDir.resolve("content.xml");

        String xml = Files.readString(contentXml);

        for (int i = 0; i < 4; i++) {
            ParteUnica parte =
                    i < grupo.size()
                            ? grupo.get(i)
                            : null;
            log.info( "item={}, ParteUnica ={}", i, parte );

            xml = xml.replace(
                    "{{nmPrincipal[" + i + "]}}",
                    parte != null
                            ? parte.nmPrincipal()
                            : ""
            );

            String dataFormatada = "";
            if( parte != null ) dataFormatada = formatar( parte.semana() );

            xml = xml.replace("{{semana[" + i + "]}}", dataFormatada );

            xml = xml.replace(
                    "{{nmAjudante[" + i + "]}}",
                    parte != null
                            ? parte.nmAjudante()
                            : ""
            );

            xml = xml.replace(
                    "{{codigoParte[" + i + "]}}",
                    parte != null
                            ? parte.codigoParte().toString()
                            : ""
            );
        }

        Files.writeString(
                contentXml,
                xml
        );

        Files.delete(odtPath);

        ZipUtils.zip2(
                tempDir,
                odtPath
        );

        FileUtils.deleteDirectory(
                tempDir.toFile()
        );
    }

    private void converterPdf2(
            Path arquivoOdt,
            Path arquivoPdf
    ) throws Exception {

        JodConverter
                .convert(arquivoOdt.toFile())
                .to(arquivoPdf.toFile())
                .execute();
    }

    private void converterPdf1(
            Path arquivoOdt,
            Path arquivoPdf
    ) throws Exception {
        OfficeManager officeManager =
                LocalOfficeManager.builder()
                        .install()
                        .build();

        officeManager.start();

        try {

            JodConverter
                    .convert(arquivoOdt.toFile())
                    .to(arquivoPdf.toFile())
                    .execute();

        } finally {
            officeManager.stop();
        }
    }
}
