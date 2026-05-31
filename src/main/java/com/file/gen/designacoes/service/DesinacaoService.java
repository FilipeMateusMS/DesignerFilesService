package com.file.gen.designacoes.service;

import com.file.gen.designacoes.dto.DesignacaoRequest;
import com.file.gen.designacoes.dto.Parte;
import com.file.gen.designacoes.dto.ParteSemana;
import com.file.gen.designacoes.dto.ParteUnica;
import com.file.gen.designacoes.util.ZipUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.JodConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.apache.pdfbox.io.IOUtils.createMemoryOnlyStreamCache;

@Service
@Log4j2
public class DesinacaoService {

    @Value("${application.folderOutput}")
    private String folderOutput;

    @Value("${application.folderPdfOutput}")
    private String folderPdfOutput;

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern(
                    "dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR")
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

        List<String> arquivosEmpacotar = new ArrayList<>();

        processarSala(
                arquivosEmpacotar,
                parteSalaA,
                "templates/SalaADesignacao.odt"
        );
        log.info( "Quantidade de partes da sala A= {}", parteSalaA.size() );

        processarSala(
                arquivosEmpacotar,
                parteSalaB,
                "templates/SalaBDesignacao.odt"
        );

        empacotar( arquivosEmpacotar );

        log.info( "Geração finalizado partes da sala A= {}, partes da sala B={}", parteSalaA.size(), parteSalaB.size() );
    }

    private void empacotar( List<String> arquivosEmpacotar ) throws IOException {

        Path pastaDestino = Path.of( folderPdfOutput );

        if (!Files.exists(pastaDestino)) {
            Files.createDirectories(pastaDestino);
        }

        Path pdfFinal = pastaDestino.resolve(
                "Designção-"
                        + formatar( LocalDate.now() )
                        + " .pdf");

        PDFMergerUtility merger = new PDFMergerUtility();
        for (String arquivo : arquivosEmpacotar) {
            if( !arquivo.endsWith( ".pdf" ) )
                throw new IllegalStateException( "Arquivo deve ser um .pdf" );
            Path pdf = Path.of(arquivo);

            if (Files.exists(pdf)) {
                merger.addSource(pdf.toFile());
            } else {
                throw new FileNotFoundException( "Arquivo não encontrado: " + pdf);
            }
        }

        merger.setDestinationFileName(pdfFinal.toString());
        merger.mergeDocuments( createMemoryOnlyStreamCache() );
        log.info( "PDF gerado: {}", pdfFinal);
    }



    private void processarSala(
            List<String> arquivosEmpacotar,
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

            arquivosEmpacotar.add( gerarDocumento( grupo, template, contador++ ) );
        }
    }

    private String obterNmArquivo( List<ParteUnica> grupo, int contador ) {

        if (grupo.isEmpty()) return "parte_vazio";
        ParteUnica primeiraParte = grupo.getFirst();

        String sala = primeiraParte.isSalaA() ? "SalaA" : "SalaB";

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

    private String gerarDocumento(
            List<ParteUnica> grupo,
            String templatePath,
            int numeroArquivo
    ) throws Exception {
        Path pastaSaida = Path.of(folderOutput);
        String nmArquivo = obterNmArquivo( grupo, numeroArquivo );
        Path odtSaida = pastaSaida.resolve(nmArquivo + ".odt");
        Path pdfSaida = pastaSaida.resolve(nmArquivo + ".pdf" );

        Files.createDirectories(pastaSaida);
        ClassPathResource resource = new ClassPathResource(templatePath);

        Files.copy(
                resource.getInputStream(),
                odtSaida,
                StandardCopyOption.REPLACE_EXISTING
        );
        substituirVariaveisOdt( odtSaida, grupo );
        converterPdf( odtSaida, pdfSaida );
        return pdfSaida
                .toAbsolutePath()
                .normalize()
                .toString();
    }

    private void substituirVariaveisOdt( Path odtPath, List<ParteUnica> grupo ) throws Exception {

        Path tempDir = Files.createTempDirectory("odt-temp" );
        ZipUtils.unzip( odtPath, tempDir );

        Path contentXml = tempDir.resolve("content.xml");
        String xml = Files.readString(contentXml);

        for (int i = 0; i < 4; i++) {

           // Parte pode ser inexistente
            ParteUnica parte = i < grupo.size() ? grupo.get(i) : null;
            log.info( "item={}, ParteUnica ={}", i, parte );

            if (parte == null) {
                xml = replaceXml( xml,"nmPrincipal", "", i, 13  );
                xml = replaceXml( xml,"semana", "", i, 13  );
                xml = replaceXml( xml, "nmAjudante", "", i, 13 );
                xml = replaceXml( xml, "cdPrt", "", i, 13 );
            }
            else{
                xml = replaceXml( xml,"nmPrincipal", parte.nmPrincipal() , i, 13  );
                xml = replaceXml( xml,"semana", formatar( parte.semana() ), i, 13  );
                xml = replaceXml( xml, "nmAjudante", parte.nmAjudante(), i, 13 );
                xml = replaceXml( xml, "cdPrt", Integer.toString( parte.codigoParte() ), i, 13 );
            }
        }

        Files.writeString( contentXml, xml );
        Files.delete(odtPath);
        ZipUtils.zip( tempDir, odtPath );
        FileUtils.deleteDirectory( tempDir.toFile() );
    }

    private String replaceXml( String xml, String key, String value, int ordem, int qtSpaces ) throws Exception {
        if (xml == null) return null;
        if( value == null ) value = "";

        int qtCaracteresValue = value.length();
        if( ( ordem == 0 || ordem == 2 ) && qtCaracteresValue < qtSpaces )
        {
            value = value + " ".repeat( qtSpaces - qtCaracteresValue );
            log.info( "Substituição gerada para a chave: '" + key +"' com valor'" + value + "'" );
        }
        String fullKey = "{{" + key + "[" + ordem  + "]}}";

        // Valida se existe a chave no documento
        //if( xml.contains( fullKey ) ) throw new Exception( "Chave: '" + fullKey + "' não encontrada no documento" );

        return xml.replace( fullKey, value );
    }

    private void converterPdf( Path arquivoOdt, Path arquivoPdf ) throws Exception
    {
        OfficeManager officeManager = LocalOfficeManager.builder()
                        .install()
                        .build();
        officeManager.start();
        try {
            JodConverter.convert(arquivoOdt.toFile())
                    .to(arquivoPdf.toFile())
                    .execute();
        } finally {
            officeManager.stop();
        }
    }
}
