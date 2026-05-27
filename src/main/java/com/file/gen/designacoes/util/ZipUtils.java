package com.file.gen.designacoes.util;

import java.io.IOException;
import java.nio.file.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private ZipUtils() {
    }

    public static void unzip(
            Path zipFile,
            Path targetDir
    ) throws IOException {

        try (ZipInputStream zis =
                     new ZipInputStream(
                             Files.newInputStream(zipFile)
                     )) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                Path novoPath =
                        targetDir.resolve(entry.getName());

                if (entry.isDirectory()) {

                    Files.createDirectories(novoPath);

                } else {

                    Files.createDirectories(
                            novoPath.getParent()
                    );

                    Files.copy(
                            zis,
                            novoPath,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                zis.closeEntry();
            }
        }
    }
    public static void zip2(
            Path sourceDir,
            Path output
    ) throws IOException {

        try (ZipOutputStream zos =
                     new ZipOutputStream(
                             Files.newOutputStream(output)
                     )) {

            // O arquivo mimetype precisa ser o primeiro
            // e SEM compressão

            Path mimeTypeFile =
                    sourceDir.resolve("mimetype");

            if (Files.exists(mimeTypeFile)) {

                byte[] mimeBytes =
                        Files.readAllBytes(mimeTypeFile);

                ZipEntry mimeEntry =
                        new ZipEntry("mimetype");

                mimeEntry.setMethod(ZipEntry.STORED);
                mimeEntry.setSize(mimeBytes.length);

                CRC32 crc = new CRC32();
                crc.update(mimeBytes);

                mimeEntry.setCrc(crc.getValue());

                zos.putNextEntry(mimeEntry);

                zos.write(mimeBytes);

                zos.closeEntry();
            }

            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path ->
                            !path.getFileName()
                                    .toString()
                                    .equals("mimetype"))
                    .forEach(path -> {

                        try {

                            String entryName =
                                    sourceDir
                                            .relativize(path)
                                            .toString()
                                            .replace("\\", "/");

                            ZipEntry zipEntry =
                                    new ZipEntry(entryName);

                            zos.putNextEntry(zipEntry);

                            Files.copy(path, zos);

                            zos.closeEntry();

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }


    public static void zip(
            Path sourceDir,
            Path output
    ) throws IOException {

        try (ZipOutputStream zos =
                     new ZipOutputStream(
                             Files.newOutputStream(output)
                     )) {

            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {

                        ZipEntry zipEntry =
                                new ZipEntry(
                                        sourceDir
                                                .relativize(path)
                                                .toString()
                                );

                        try {

                            zos.putNextEntry(zipEntry);

                            Files.copy(path, zos);

                            zos.closeEntry();

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
