/*-
 * #%L
 * Reference Validator Library
 * %%
 * Copyright (C) 2024 - 2026 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.lib.snapshot.control;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageArchiveUtilsTest {
  private static final byte[] EMPTY_CONTENT = new byte[0];

  @TempDir Path tempDir;

  @DisplayName("Given a directory with files, when compressing, then a .tgz archive is created")
  @Test
  void expectCompressCreatesArchive() throws Exception {
    // Given a source directory with a file
    final var source = Files.createDirectory(tempDir.resolve("my.pkg-1.0.0"));
    Files.writeString(source.resolve("file.txt"), "content");
    final var output = Files.createDirectory(tempDir.resolve("out"));

    // When compressing
    final var archiveName = PackageArchiveUtils.compress(source, output);

    // Then the archive exists
    assertTrue(archiveName.endsWith(".tgz"));
    assertTrue(Files.exists(Path.of(archiveName)));
  }

  @DisplayName("Given a non-directory source, when compressing, then an IOException is thrown")
  @Test
  void expectCompressNonDirectoryThrows() throws Exception {
    final var file = Files.createFile(tempDir.resolve("a-file.txt"));
    Assertions.assertThrows(IOException.class, () -> PackageArchiveUtils.compress(file, tempDir));
  }

  @DisplayName("Given a compressed archive, when decompressing, then the files are extracted")
  @Test
  void expectDecompressExtractsFiles() throws Exception {
    // Given a source directory compressed into an archive
    final var source = Files.createDirectory(tempDir.resolve("my.pkg-2.0.0"));
    Files.writeString(source.resolve("entry.txt"), "hello");
    final var output = Files.createDirectory(tempDir.resolve("out"));
    final var archiveName = PackageArchiveUtils.compress(source, output);

    // When decompressing
    final var extractDir = Files.createDirectory(tempDir.resolve("extract"));
    PackageArchiveUtils.decompress(archiveName, extractDir.toFile());

    // Then the file is extracted
    assertTrue(Files.exists(extractDir.resolve("entry.txt")));
    Assertions.assertEquals("hello", Files.readString(extractDir.resolve("entry.txt")));
  }

  @Test
  void compressShouldCreateArchiveContainingFilesAndDirectories() throws Exception {
    Path source = tempDir.resolve("example-package");
    Path output = tempDir.resolve("output");

    Files.createDirectories(source.resolve("package/nested"));
    Files.createDirectories(source.resolve("empty-directory"));

    Files.writeString(
        source.resolve("package.json"),
        """
            {
              "name": "example.package",
              "version": "1.0.0"
            }
            """,
        StandardCharsets.UTF_8);

    Files.writeString(
        source.resolve("package/StructureDefinition-example.json"),
        """
            {
              "resourceType": "StructureDefinition"
            }
            """,
        StandardCharsets.UTF_8);

    Files.writeString(
        source.resolve("package/nested/example.txt"), "nested content", StandardCharsets.UTF_8);

    String archiveName = PackageArchiveUtils.compress(source, output);

    Path archive = Path.of(archiveName);

    assertTrue(Files.isRegularFile(archive));
    Assertions.assertEquals(
        output.toAbsolutePath().normalize().resolve("example-package.tgz"),
        archive.toAbsolutePath().normalize());

    List<ArchiveEntryData> entries = readArchive(archive);

    assertTrue(entries.stream().anyMatch(entry -> entry.name().equals("package.json")));
    assertTrue(entries.stream().anyMatch(entry -> entry.name().equals("package/")));
    assertTrue(
        entries.stream()
            .anyMatch(entry -> entry.name().equals("package/StructureDefinition-example.json")));
    assertTrue(entries.stream().anyMatch(entry -> entry.name().equals("package/nested/")));
    assertTrue(
        entries.stream().anyMatch(entry -> entry.name().equals("package/nested/example.txt")));
    assertTrue(entries.stream().anyMatch(entry -> entry.name().equals("empty-directory/")));

    Assertions.assertFalse(
        entries.stream().anyMatch(entry -> entry.name().startsWith("example-package/")));
  }

  @Test
  void compressShouldPreserveEmptyDirectories() throws Exception {
    Path source = tempDir.resolve("package");
    Path output = tempDir.resolve("output");

    Files.createDirectories(source.resolve("empty/nested"));

    Path archive = Path.of(PackageArchiveUtils.compress(source, output));

    List<ArchiveEntryData> entries = readArchive(archive);

    assertTrue(entries.stream().anyMatch(entry -> entry.name().equals("empty/")));
    assertTrue(entries.stream().anyMatch(entry -> entry.name().equals("empty/nested/")));

    ArchiveEntryData emptyDirectory =
        entries.stream().filter(entry -> entry.name().equals("empty/")).findFirst().orElseThrow();

    assertTrue(emptyDirectory.directory());
    Assertions.assertEquals(0, emptyDirectory.content().length);
  }

  @Test
  void compressShouldUseDeterministicEntryOrdering() throws Exception {
    Path source = tempDir.resolve("ordered-package");
    Path output = tempDir.resolve("output");

    Files.createDirectories(source);

    Files.writeString(source.resolve("z-file.txt"), "z");
    Files.writeString(source.resolve("a-file.txt"), "a");
    Files.writeString(source.resolve("m-file.txt"), "m");

    Path archive = Path.of(PackageArchiveUtils.compress(source, output));

    List<String> entryNames = readArchive(archive).stream().map(ArchiveEntryData::name).toList();

    Assertions.assertEquals(List.of("a-file.txt", "m-file.txt", "z-file.txt"), entryNames);
  }

  @Test
  void compressShouldNormalizeTarMetadata() throws Exception {
    Path source = tempDir.resolve("metadata-package");
    Path output = tempDir.resolve("output");

    Files.createDirectories(source.resolve("directory"));
    Files.writeString(source.resolve("file.txt"), "content");

    Path archive = Path.of(PackageArchiveUtils.compress(source, output));

    List<ArchiveEntryData> entries = readArchive(archive);

    ArchiveEntryData directory =
        entries.stream()
            .filter(entry -> entry.name().equals("directory/"))
            .findFirst()
            .orElseThrow();

    ArchiveEntryData file =
        entries.stream().filter(entry -> entry.name().equals("file.txt")).findFirst().orElseThrow();

    Assertions.assertEquals(0L, directory.modificationTime());
    Assertions.assertEquals(0L, file.modificationTime());

    Assertions.assertEquals("", directory.userName());
    Assertions.assertEquals("", directory.groupName());
    Assertions.assertEquals("", file.userName());
    Assertions.assertEquals("", file.groupName());

    Assertions.assertEquals(0L, directory.userId());
    Assertions.assertEquals(0L, directory.groupId());
    Assertions.assertEquals(0L, file.userId());
    Assertions.assertEquals(0L, file.groupId());

    Assertions.assertEquals(0755, directory.mode());
    Assertions.assertEquals(0644, file.mode());
  }

  @Test
  void compressShouldPreserveExistingArchiveWhenCompressionFails() throws Exception {

    Path source = tempDir.resolve("failed-package");
    Path output = tempDir.resolve("output");

    Files.createDirectories(source);
    Files.createDirectories(output);

    Path existingArchive = output.resolve("failed-package.tgz");

    byte[] existingContent = "previous valid archive placeholder".getBytes(StandardCharsets.UTF_8);

    Files.write(existingArchive, existingContent);

    Path target = tempDir.resolve("outside.txt");
    Files.writeString(target, "outside");

    Path symbolicLink = source.resolve("symbolic-link");

    assumeSymlinkCanBeCreated(symbolicLink, target);

    Assertions.assertThrows(IOException.class, () -> PackageArchiveUtils.compress(source, output));

    Assertions.assertArrayEquals(existingContent, Files.readAllBytes(existingArchive));
  }

  @Test
  void compressShouldRejectMissingSourceDirectory() {
    var missingSource = tempDir.resolve("missing");
    var output = tempDir.resolve("output");

    var exception =
        Assertions.assertThrows(
            IOException.class, () -> PackageArchiveUtils.compress(missingSource, output));

    assertTrue(exception.getMessage().contains("does not exist"));
  }

  @Test
  void compressShouldRejectSourceThatIsARegularFile() throws Exception {
    var sourceFile = tempDir.resolve("source.txt");
    var output = tempDir.resolve("output");

    Files.writeString(sourceFile, "content");

    var exception =
        Assertions.assertThrows(
            IOException.class, () -> PackageArchiveUtils.compress(sourceFile, output));

    assertTrue(exception.getMessage().contains("not a directory"));
  }

  @Test
  void compressShouldRejectSourceDirectorySymlink() throws Exception {
    var actualSource = tempDir.resolve("actual-source");
    var sourceLink = tempDir.resolve("source-link");
    var output = tempDir.resolve("output");

    Files.createDirectories(actualSource);
    Files.writeString(actualSource.resolve("file.txt"), "content");

    assumeSymlinkCanBeCreated(sourceLink, actualSource);

    var exception =
        Assertions.assertThrows(
            IOException.class, () -> PackageArchiveUtils.compress(sourceLink, output));

    Assertions.assertTrue(exception.getMessage().contains("symbolic link"));
  }

  @Test
  void compressShouldRejectSymbolicLinkInsideSourceDirectory() throws Exception {
    var source = tempDir.resolve("source");
    var output = tempDir.resolve("output");
    var externalFile = tempDir.resolve("external.txt");

    Files.createDirectories(source);
    Files.writeString(externalFile, "external");

    Path symbolicLink = source.resolve("external-link");

    assumeSymlinkCanBeCreated(symbolicLink, externalFile);

    var exception =
        Assertions.assertThrows(
            IOException.class, () -> PackageArchiveUtils.compress(source, output));

    Assertions.assertTrue(exception.getMessage().contains("Symbolic links are not supported"));
  }

  @Test
  void compressShouldRejectOutputPathThatIsAFile() throws Exception {
    Path source = tempDir.resolve("source");
    Path outputFile = tempDir.resolve("output-file");

    Files.createDirectories(source);
    Files.writeString(source.resolve("file.txt"), "content");
    Files.writeString(outputFile, "not a directory");

    Assertions.assertThrows(
        IOException.class, () -> PackageArchiveUtils.compress(source, outputFile));
  }

  @Test
  void compressShouldRejectOutputDirectorySymlink() throws Exception {
    Path source = tempDir.resolve("source");
    Path actualOutput = tempDir.resolve("actual-output");
    Path outputLink = tempDir.resolve("output-link");

    Files.createDirectories(source);
    Files.createDirectories(actualOutput);
    Files.writeString(source.resolve("file.txt"), "content");
    assumeSymlinkCanBeCreated(outputLink, actualOutput);

    var exception =
        Assertions.assertThrows(
            IOException.class, () -> PackageArchiveUtils.compress(source, outputLink));

    assertTrue(exception.getMessage().contains("symbolic link"));
  }

  @Test
  void decompressShouldExtractRegularFilesDirectoriesAndEmptyDirectories() throws Exception {
    Path archive =
        createArchive(
            "valid.tgz",
            directory("package/"),
            regularFile("package/file.txt", "content"),
            directory("package/empty/"),
            regularFile("package/nested/value.json", "{\"value\": true}"));

    Path output = tempDir.resolve("extracted");
    PackageArchiveUtils.decompress(archive.toString(), output.toFile());

    assertTrue(Files.isDirectory(output.resolve("package")));
    assertTrue(Files.isDirectory(output.resolve("package/empty")));
    assertTrue(Files.isDirectory(output.resolve("package/nested")));
    Assertions.assertEquals(
        "content", Files.readString(output.resolve("package/file.txt"), StandardCharsets.UTF_8));
    Assertions.assertEquals(
        "{\"value\": true}",
        Files.readString(output.resolve("package/nested/value.json"), StandardCharsets.UTF_8));
  }

  @Test
  void decompressShouldCreateOutputDirectoryWhenItDoesNotExist() throws Exception {
    Path archive = createArchive("create-output.tgz", regularFile("file.txt", "content"));
    Path output = tempDir.resolve("missing-output");

    Assertions.assertFalse(Files.exists(output));
    PackageArchiveUtils.decompress(archive.toString(), output.toFile());

    Assertions.assertTrue(Files.isDirectory(output));
    Assertions.assertEquals("content", Files.readString(output.resolve("file.txt")));
  }

  @Test
  void decompressShouldUseAnExistingEmptyOutputDirectory() throws Exception {
    Path archive = createArchive("empty-output.tgz", regularFile("file.txt", "content"));
    Path output = tempDir.resolve("existing-output");
    Files.createDirectories(output);

    PackageArchiveUtils.decompress(archive.toString(), output.toFile());
    Assertions.assertEquals("content", Files.readString(output.resolve("file.txt")));
  }

  @Test
  void decompressShouldRejectBlankArchiveName() {
    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(" ", tempDir.resolve("output").toFile()));

    Assertions.assertTrue(exception.getMessage().contains("must not be blank"));
  }

  @Test
  void decompressShouldRejectMissingArchive() {
    Path missingArchive = tempDir.resolve("missing.tgz");
    Path output = tempDir.resolve("output");

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(missingArchive.toString(), output.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("does not exist"));
  }

  @Test
  void decompressShouldRejectArchiveSymlink() throws Exception {
    Path actualArchive = createArchive("actual.tgz", regularFile("file.txt", "content"));
    Path archiveLink = tempDir.resolve("archive-link.tgz");
    Path output = tempDir.resolve("output");

    assumeSymlinkCanBeCreated(archiveLink, actualArchive);

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archiveLink.toString(), output.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("symbolic link"));
  }

  @Test
  void decompressShouldRejectNonGzipInputAndRemovePartialOutput() throws Exception {
    Path invalidArchive = tempDir.resolve("invalid.tgz");
    Path output = tempDir.resolve("output");

    Files.writeString(invalidArchive, "this is not a gzip archive", StandardCharsets.UTF_8);

    Assertions.assertThrows(
        IOException.class,
        () -> PackageArchiveUtils.decompress(invalidArchive.toString(), output.toFile()));

    Assertions.assertFalse(Files.exists(output));
  }

  @Test
  void decompressShouldRejectNonEmptyOutputDirectory() throws Exception {
    Path archive = createArchive("valid.tgz", regularFile("file.txt", "content"));
    Path output = tempDir.resolve("output");

    Files.createDirectories(output);
    Files.writeString(output.resolve("existing.txt"), "existing");

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archive.toString(), output.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("must be empty"));
    Assertions.assertTrue(Files.exists(output.resolve("existing.txt")));
  }

  @Test
  void decompressShouldRejectOutputPathThatIsAFile() throws Exception {
    Path archive = createArchive("valid.tgz", regularFile("file.txt", "content"));
    Path outputFile = tempDir.resolve("output-file");
    Files.writeString(outputFile, "not a directory");

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archive.toString(), outputFile.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("not a directory"));
  }

  @Test
  void decompressShouldRejectOutputDirectorySymlink() throws Exception {
    Path archive = createArchive("valid.tgz", regularFile("file.txt", "content"));
    Path actualOutput = tempDir.resolve("actual-output");
    Path outputLink = tempDir.resolve("output-link");

    Files.createDirectories(actualOutput);
    assumeSymlinkCanBeCreated(outputLink, actualOutput);

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archive.toString(), outputLink.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("symbolic link"));
  }

  @Test
  void decompressShouldRejectParentDirectoryTraversal() throws Exception {
    Path archive = createArchive("zip-slip.tgz", regularFile("../../escaped.txt", "malicious"));
    Path output = tempDir.resolve("output");
    Path escapedFile = tempDir.getParent().resolve("escaped.txt");
    Files.deleteIfExists(escapedFile);

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archive.toString(), output.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("escapes"));
    Assertions.assertFalse(Files.exists(escapedFile));
    Assertions.assertFalse(Files.exists(output));
  }

  @Test
  void decompressShouldRejectBackslashDirectoryTraversal() throws Exception {
    var archive = createArchive("backslash-slip.tgz", regularFile("..\\escaped.txt", "malicious"));
    var output = tempDir.resolve("output");
    var escapedFile = tempDir.resolve("escaped.txt");

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archive.toString(), output.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("escapes"));
    Assertions.assertFalse(Files.exists(escapedFile));
    Assertions.assertFalse(Files.exists(output));
  }

  @Test
  void decompressShouldRejectWindowsDriveQualifiedPath() throws Exception {
    var archive =
        createArchive("windows-drive-path.tgz", regularFile("C:/outside.txt", "malicious"));
    var output = tempDir.resolve("output");

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archive.toString(), output.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("drive-qualified"));
    Assertions.assertFalse(Files.exists(output));
  }

  @Test
  void decompressShouldRejectWindowsReservedDeviceNames() throws Exception {
    var archive = createArchive("reserved-name.tgz", regularFile("package/CON.txt", "malicious"));
    var output = tempDir.resolve("output");

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archive.toString(), output.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("reserved device name"));
    Assertions.assertFalse(Files.exists(output));
  }

  @Test
  void decompressShouldRejectWindowsIllegalCharacters() throws Exception {
    var archive =
        createArchive("illegal-chars.tgz", regularFile("package/file?.json", "malicious"));
    var output = tempDir.resolve("output");

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archive.toString(), output.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("illegal on Windows"));
    Assertions.assertFalse(Files.exists(output));
  }

  @Test
  void decompressShouldRejectTrailingDotsAndSpacesInSegments() throws Exception {
    var archive =
        createArchive("trailing-dot.tgz", regularFile("package/file.txt./nested.json", "x"));
    var output = tempDir.resolve("output");

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archive.toString(), output.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("unsafe path segment"));
    Assertions.assertFalse(Files.exists(output));
  }

  @Test
  void decompressShouldRejectHardLinkEntries() throws Exception {
    Path archive = createArchive("hard-link-entry.tgz", hardLink());
    Path output = tempDir.resolve("output");

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archive.toString(), output.toFile()));

    Assertions.assertTrue(exception.getMessage().contains("Hard links"));
    Assertions.assertFalse(Files.exists(output));
  }

  @Test
  void decompressShouldRejectDuplicateFileEntries() throws Exception {
    var archive =
        createArchive(
            "duplicates.tgz",
            regularFile("duplicate.txt", "first"),
            regularFile("duplicate.txt", "second"));
    var output = tempDir.resolve("output");

    var exception =
        Assertions.assertThrows(
            IOException.class,
            () -> PackageArchiveUtils.decompress(archive.toString(), output.toFile()));

    assertTrue(
        exception.getMessage().contains("duplicate")
            || exception.getMessage().contains("conflicting"));
    Assertions.assertFalse(Files.exists(output));
  }

  @Test
  void decompressShouldRejectFileConflictingWithDirectory() throws Exception {
    Path archive =
        createArchive(
            "file-directory-conflict.tgz",
            regularFile("conflict", "file"),
            regularFile("conflict/nested.txt", "nested"));
    Path output = tempDir.resolve("output");

    Assertions.assertThrows(
        IOException.class,
        () -> PackageArchiveUtils.decompress(archive.toString(), output.toFile()));

    Assertions.assertFalse(Files.exists(output));
  }

  @Test
  void decompressShouldRemoveAlreadyExtractedFilesIfLaterEntryFails() throws Exception {
    Path archive =
        createArchive(
            "partial-extraction.tgz",
            regularFile("valid.txt", "valid"),
            regularFile("../../escaped.txt", "malicious"));
    Path output = tempDir.resolve("output");

    Assertions.assertThrows(
        IOException.class,
        () -> PackageArchiveUtils.decompress(archive.toString(), output.toFile()));

    Assertions.assertFalse(Files.exists(output));
  }

  @Test
  void compressAndDecompressShouldRoundTripBinaryContent() throws Exception {
    Path source = tempDir.resolve("binary-package");
    Path archiveOutput = tempDir.resolve("archives");
    Path extractionOutput = tempDir.resolve("extracted");

    Files.createDirectories(source.resolve("package"));

    byte[] binaryContent = new byte[16_384];
    for (int index = 0; index < binaryContent.length; index++) {
      binaryContent[index] = (byte) (index % 256);
    }

    Files.write(source.resolve("package/binary.dat"), binaryContent);

    Path archive = Path.of(PackageArchiveUtils.compress(source, archiveOutput));
    PackageArchiveUtils.decompress(archive.toString(), extractionOutput.toFile());

    Assertions.assertArrayEquals(
        binaryContent, Files.readAllBytes(extractionOutput.resolve("package/binary.dat")));
  }

  @Test
  void compressAndDecompressShouldRoundTripUnicodeFileNames() throws Exception {
    Path source = tempDir.resolve("unicode-package");
    Path archiveOutput = tempDir.resolve("archives");
    Path extractionOutput = tempDir.resolve("extracted");

    Files.createDirectories(source.resolve("package"));

    String fileName = "Überprüfung-患者.json";
    String content = "{\"description\":\"Grüße 世界\"}";

    Files.writeString(source.resolve("package").resolve(fileName), content, StandardCharsets.UTF_8);

    Path archive = Path.of(PackageArchiveUtils.compress(source, archiveOutput));
    PackageArchiveUtils.decompress(archive.toString(), extractionOutput.toFile());

    Assertions.assertEquals(
        content,
        Files.readString(
            extractionOutput.resolve("package").resolve(fileName), StandardCharsets.UTF_8));
  }

  @Test
  void decompressShouldAllowAnEmptyArchive() throws Exception {
    Path archive = createArchive("empty.tgz");
    Path output = tempDir.resolve("output");

    PackageArchiveUtils.decompress(archive.toString(), output.toFile());

    assertTrue(Files.isDirectory(output));
    try (var entries = Files.list(output)) {
      assertTrue(entries.findAny().isEmpty());
    }
  }

  @Test
  void compressShouldCreateAnEmptyArchiveForAnEmptySourceDirectory() throws Exception {
    Path source = tempDir.resolve("empty-source");
    Path output = tempDir.resolve("output");

    Files.createDirectories(source);

    Path archive = Path.of(PackageArchiveUtils.compress(source, output));

    assertTrue(Files.isRegularFile(archive));
    assertTrue(readArchive(archive).isEmpty());
  }

  private Path createArchive(String archiveName, ArchiveSpecification... specifications)
      throws IOException {

    Path archive = tempDir.resolve(archiveName);

    try (OutputStream fileOutput = Files.newOutputStream(archive);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);
        GzipCompressorOutputStream gzipOutput = new GzipCompressorOutputStream(bufferedOutput);
        TarArchiveOutputStream tarOutput = new TarArchiveOutputStream(gzipOutput)) {

      tarOutput.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
      tarOutput.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

      for (ArchiveSpecification specification : specifications) {
        TarArchiveEntry entry = new TarArchiveEntry(specification.name(), specification.linkFlag());

        if (specification.linkTarget() != null) {
          entry.setLinkName(specification.linkTarget());
        }

        if (specification.linkFlag() == TarConstants.LF_NORMAL) {
          entry.setSize(specification.content().length);
        } else {
          entry.setSize(0);
        }

        tarOutput.putArchiveEntry(entry);

        if (specification.linkFlag() == TarConstants.LF_NORMAL
            && specification.content().length > 0) {
          tarOutput.write(specification.content());
        }

        tarOutput.closeArchiveEntry();
      }

      tarOutput.finish();
    }

    return archive;
  }

  private List<ArchiveEntryData> readArchive(Path archive) throws IOException {
    List<ArchiveEntryData> entries = new ArrayList<>();

    try (InputStream fileInput = Files.newInputStream(archive);
        BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
        GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(bufferedInput);
        TarArchiveInputStream tarInput = new TarArchiveInputStream(gzipInput)) {

      TarArchiveEntry entry;

      while ((entry = tarInput.getNextEntry()) != null) {
        byte[] content = entry.isFile() ? tarInput.readAllBytes() : EMPTY_CONTENT;

        entries.add(
            new ArchiveEntryData(
                entry.getName(),
                entry.isDirectory(),
                content,
                entry.getModTime().getTime(),
                entry.getUserName(),
                entry.getGroupName(),
                entry.getLongUserId(),
                entry.getLongGroupId(),
                entry.getMode()));
      }
    }

    return List.copyOf(entries);
  }

  private ArchiveSpecification regularFile(String name, String content) {
    return new ArchiveSpecification(
        name, TarConstants.LF_NORMAL, content.getBytes(StandardCharsets.UTF_8), null);
  }

  private ArchiveSpecification directory(String name) {
    return new ArchiveSpecification(name, TarConstants.LF_DIR, EMPTY_CONTENT, null);
  }

  private ArchiveSpecification symbolicLink() {
    return new ArchiveSpecification(
        "link", TarConstants.LF_SYMLINK, EMPTY_CONTENT, "../../outside.txt");
  }

  private ArchiveSpecification hardLink() {
    return new ArchiveSpecification("hard-link", TarConstants.LF_LINK, EMPTY_CONTENT, "target.txt");
  }

  private void assumeSymlinkCanBeCreated(Path symbolicLink, Path target) {
    try {
      Files.createSymbolicLink(symbolicLink, target);
    } catch (UnsupportedOperationException | IOException | SecurityException e) {
      Assumptions.assumeTrue(
          false, "Symbolic links are not supported in this test environment: " + e.getMessage());
    }
    Assumptions.assumeTrue(
        Files.isSymbolicLink(symbolicLink), "The filesystem did not create the symbolic link");
  }

  private record ArchiveSpecification(
      String name, byte linkFlag, byte[] content, String linkTarget) {}

  private record ArchiveEntryData(
      String name,
      boolean directory,
      byte[] content,
      long modificationTime,
      String userName,
      String groupName,
      long userId,
      long groupId,
      int mode) {}
}
