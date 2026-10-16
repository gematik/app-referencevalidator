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

import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for creating and extracting FHIR NPM package TGZ archives.
 *
 * <p>Compression is deterministic with respect to entry ordering and normalized TAR metadata.
 * Archives are written to a temporary file and published only after compression succeeds.
 *
 * <p>Extraction accepts only directories and regular files. Links and special filesystem entries
 * are rejected.
 */
final class PackageArchiveUtils {

  private static final Logger log = LoggerFactory.getLogger(PackageArchiveUtils.class);

  private static final String TGZ_EXTENSION = LocalArchive.ARCHIVE_PACKAGE_EXTENSION;
  private static final int BUFFER_SIZE = 64 * 1024;

  /** Defensive extraction limits. */
  private static final int MAX_ENTRY_COUNT = 100_000;

  private static final long MAX_ENTRY_SIZE = 512L * 1024L * 1024L;
  private static final long MAX_EXTRACTED_SIZE = 2L * 1024L * 1024L * 1024L;

  /** Default permissions. */
  private static final int REGULAR_FILE_MODE = 0644;

  private static final int DIRECTORY_MODE = 0755;

  /**
   * Characters that are illegal in file names on Windows. Archives are shared across platforms, so
   * these are rejected on every host OS to keep extraction portable.
   */
  private static final Pattern WINDOWS_ILLEGAL_CHARS = Pattern.compile("[<>:\"|?*\\p{Cntrl}]");

  /** Device names reserved on Windows, with or without an extension (e.g. {@code CON.txt}). */
  private static final Set<String> WINDOWS_RESERVED_NAMES =
      Set.of(
          "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7",
          "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

  private PackageArchiveUtils() {}

  /**
   * Compresses the content of a directory into a TGZ archive.
   *
   * <p>The source directory itself is not placed inside the TAR. Its children are archived relative
   * to the source directory.
   *
   * <p>The method preserves the original return type and returns the generated archive path as a
   * string.
   *
   * @param sourceDirectory directory whose content will be compressed
   * @param outputDirectory directory in which the archive will be created
   * @return generated archive path
   */
  static @NonNull String compress(@NonNull Path sourceDirectory, @NonNull Path outputDirectory)
      throws IOException {
    Objects.requireNonNull(sourceDirectory, "The source directory must not be null");
    Objects.requireNonNull(outputDirectory, "The output directory must not be null");

    final var normalizedSource = sourceDirectory.toAbsolutePath().normalize();
    final var normalizedOutput = outputDirectory.toAbsolutePath().normalize();
    validateSourceDirectory(normalizedSource);
    validateOutputDirectory(normalizedOutput);

    final var finalArchive = getArchivePath(normalizedSource, normalizedOutput);

    // Create a temp file in the output directory, so an atomic move can be executed
    final var temporaryArchive =
        Files.createTempFile(normalizedOutput, "." + finalArchive.getFileName() + "-", ".tmp");

    // track the publishing of the archive
    boolean published = false;
    try {
      List<Path> entries = collectArchiveEntries(normalizedSource);
      writeArchive(normalizedSource, entries, temporaryArchive);
      publishArchive(temporaryArchive, finalArchive);
      published = true;
      log.debug("Compressed directory {} into {}", normalizedSource, finalArchive);
      return finalArchive.toString();
    } finally {
      if (!published) {
        Files.deleteIfExists(temporaryArchive);
      }
    }
  }

  /**
   * Extracts a TGZ archive into an output directory.
   *
   * <p>The output directory may either not exist or exist as an empty directory. It must not be a
   * symbolic link.
   *
   * <p>If extraction fails, partial extraction content created by this invocation is removed.
   *
   * @param inputFileName input archive filename
   * @param outputDirectory extraction destination
   */
  static void decompress(@NonNull String inputFileName, @NonNull File outputDirectory)
      throws IOException {
    Objects.requireNonNull(inputFileName, "The input filename must not be null");
    Objects.requireNonNull(outputDirectory, "The output directory must not be null");

    if (inputFileName.isBlank()) {
      throw new IOException("The input filename must not be blank");
    }

    Path archive = Path.of(inputFileName).toAbsolutePath().normalize();
    Path extractionRoot = outputDirectory.toPath().toAbsolutePath().normalize();

    validateArchive(archive);
    prepareEmptyOutputDirectory(extractionRoot);

    log.debug("Decompressing {} to {}", archive, extractionRoot);

    boolean completed = false;
    try {
      extractArchive(archive, extractionRoot);
      completed = true;
      log.debug("Successfully decompressed {} to {}", archive, extractionRoot);
    } finally {
      if (!completed) {
        cleanupPartialExtraction(extractionRoot);
      }
    }
  }

  private static List<Path> collectArchiveEntries(@NonNull Path sourceDirectory)
      throws IOException {

    List<Path> entries = new ArrayList<>();

    /*
     * Files.walk does not follow symbolic links unless FOLLOW_LINKS is
     * explicitly supplied.
     */
    try (var paths = Files.walk(sourceDirectory)) {
      paths.filter(path -> !path.equals(sourceDirectory)).forEach(entries::add);
    }

    for (Path entry : entries) {
      if (Files.isSymbolicLink(entry)) {
        throw new IOException("Symbolic links are not supported in package archives: " + entry);
      }

      boolean directory = Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS);
      boolean regularFile = Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS);

      if (!directory && !regularFile) {
        throw new IOException("Unsupported filesystem entry in package directory: " + entry);
      }

      if (!Files.isReadable(entry)) {
        throw new IOException("Filesystem entry is not readable: " + entry);
      }
    }

    /*
     * Filesystem traversal order is not guaranteed. Sorting produces stable
     * TAR entry ordering.
     */
    entries.sort(Comparator.comparing(path -> archiveEntryName(sourceDirectory.relativize(path))));
    return List.copyOf(entries);
  }

  private static void writeArchive(
      @NonNull Path sourceDirectory, @NonNull List<Path> entries, @NonNull Path destination)
      throws IOException {

    try (OutputStream fileOutputStream =
            Files.newOutputStream(
                destination, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        BufferedOutputStream bufferedOutputStream =
            new BufferedOutputStream(fileOutputStream, BUFFER_SIZE);
        GzipCompressorOutputStream gzipOutputStream =
            new GzipCompressorOutputStream(bufferedOutputStream);
        TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(gzipOutputStream)) {

      configureTarOutputStream(tarOutputStream);

      for (Path entry : entries) {
        Path relativePath = sourceDirectory.relativize(entry);
        addEntryToTar(entry, relativePath, tarOutputStream);
      }

      tarOutputStream.finish();
    }
  }

  private static void addEntryToTar(
      @NonNull Path source,
      @NonNull Path relativePath,
      @NonNull TarArchiveOutputStream tarOutputStream)
      throws IOException {

    boolean directory = Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS);
    String entryName = archiveEntryName(relativePath);
    if (directory && !entryName.endsWith("/")) {
      entryName += "/";
    }

    TarArchiveEntry tarEntry =
        new TarArchiveEntry(entryName, directory ? TarConstants.LF_DIR : TarConstants.LF_NORMAL);

    normalizeMetadata(tarEntry, directory);

    if (directory) {
      tarEntry.setSize(0);
      tarOutputStream.putArchiveEntry(tarEntry);
      tarOutputStream.closeArchiveEntry();
      return;
    }

    long fileSize = Files.size(source);
    tarEntry.setSize(fileSize);
    tarOutputStream.putArchiveEntry(tarEntry);
    try (InputStream inputStream =
        new BufferedInputStream(Files.newInputStream(source), BUFFER_SIZE)) {
      inputStream.transferTo(tarOutputStream);
    }
    tarOutputStream.closeArchiveEntry();
  }

  private static void extractArchive(@NonNull Path archive, @NonNull Path outputDirectory)
      throws IOException {

    long totalExtractedSize = 0;
    int entryCount = 0;

    try (var fileInputStream = Files.newInputStream(archive);
        BufferedInputStream bufferedInputStream =
            new BufferedInputStream(fileInputStream, BUFFER_SIZE);
        GzipCompressorInputStream gzipInputStream =
            new GzipCompressorInputStream(bufferedInputStream);
        TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream)) {

      TarArchiveEntry entry;

      while ((entry = tarInputStream.getNextEntry()) != null) {
        entryCount++;
        if (entryCount > MAX_ENTRY_COUNT) {
          throw new IOException(
              "Archive contains more than " + MAX_ENTRY_COUNT + " entries: " + archive);
        }

        if (!tarInputStream.canReadEntryData(entry)) {
          throw new IOException("Unsupported TAR entry encoding: " + entry.getName());
        }

        validateArchiveEntry(entry);
        final var destination = resolveArchiveEntry(outputDirectory, entry.getName());

        if (entry.isDirectory()) {
          createSafeDirectories(outputDirectory, destination);

          continue;
        }

        long entrySize = getEntrySize(archive, entry, totalExtractedSize);

        totalExtractedSize += entrySize;
        Path parent = destination.getParent();

        if (parent == null) {
          throw new IOException("Archive entry has no parent directory: " + entry.getName());
        }

        createSafeDirectories(outputDirectory, parent);
        copyCurrentEntry(tarInputStream, destination, entrySize);
      }
    }
  }

  private static long getEntrySize(
      @NonNull Path archive, TarArchiveEntry entry, long totalExtractedSize) throws IOException {
    long entrySize = entry.getSize();

    if (entrySize < 0) {
      throw new IOException("Archive entry has an invalid size: " + entry.getName());
    }

    if (entrySize > MAX_ENTRY_SIZE) {
      throw new IOException(
          "Archive entry exceeds the maximum permitted size of "
              + MAX_ENTRY_SIZE
              + " bytes: "
              + entry.getName());
    }

    /*
     * Subtraction avoids a possible overflow from
     * totalExtractedSize + entrySize.
     */
    if (totalExtractedSize > MAX_EXTRACTED_SIZE - entrySize) {
      throw new IOException(
          "Archive exceeds the maximum permitted extracted size of "
              + MAX_EXTRACTED_SIZE
              + " bytes: "
              + archive);
    }
    return entrySize;
  }

  private static void copyCurrentEntry(
      @NonNull TarArchiveInputStream tarInputStream, @NonNull Path destination, long expectedSize)
      throws IOException {
    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Archive contains a duplicate or conflicting entry: " + destination);
    }

    long copied;
    /* CREATE_NEW prevents duplicate entries from silently overwriting an earlier extracted file. */
    try (OutputStream outputStream =
        new BufferedOutputStream(
            Files.newOutputStream(
                destination, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
            BUFFER_SIZE)) {

      copied = copyLimited(tarInputStream, outputStream, expectedSize);
    }

    if (copied != expectedSize) {
      throw new IOException(
          "Unexpected extracted size for "
              + destination
              + ": expected "
              + expectedSize
              + " bytes but extracted "
              + copied
              + " bytes");
    }
  }

  private static long copyLimited(
      @NonNull InputStream inputStream, @NonNull OutputStream outputStream, long expectedSize)
      throws IOException {

    byte[] buffer = new byte[BUFFER_SIZE];
    long remaining = expectedSize;
    long copied = 0;

    while (remaining > 0) {
      int requested = (int) Math.min(buffer.length, remaining);
      int read = inputStream.read(buffer, 0, requested);
      if (read < 0) {
        break;
      }

      outputStream.write(buffer, 0, read);
      copied += read;
      remaining -= read;
    }

    return copied;
  }

  private static void validateArchiveEntry(@NonNull TarArchiveEntry entry) throws IOException {
    if (entry.isSymbolicLink()) {
      throw new IOException(
          "Symbolic links are not permitted in package archives: " + entry.getName());
    }

    if (entry.isLink()) {
      throw new IOException("Hard links are not permitted in package archives: " + entry.getName());
    }

    if (!entry.isDirectory() && !entry.isFile()) {
      throw new IOException("Unsupported TAR entry type: " + entry.getName());
    }
  }

  /**
   * Resolves an archive entry and prevents path traversal.
   *
   * <p>This replaces the need for a separate {@code ZipSlipProtect} call while retaining the same
   * protection within the archive utility itself.
   */
  private static Path resolveArchiveEntry(@NonNull Path outputDirectory, @NonNull String entryName)
      throws IOException {

    if (entryName.isBlank()) {
      throw new IOException("Archive contains an entry with an empty name");
    }

    String normalizedEntryName = entryName.replace('\\', '/');

    if (normalizedEntryName.startsWith("/")) {
      throw new IOException("Archive entry uses an absolute path: " + entryName);
    }

    if (hasWindowsDrivePrefix(normalizedEntryName)) {
      throw new IOException("Archive entry uses a drive-qualified path: " + entryName);
    }

    validateEntryNamePortable(normalizedEntryName);

    Path destination = outputDirectory.resolve(normalizedEntryName).normalize();

    if (!destination.startsWith(outputDirectory)) {
      throw new IOException("Archive entry escapes the extraction directory: " + entryName);
    }

    return destination;
  }

  /**
   * Rejects entry names that are illegal or dangerous on Windows, regardless of the host OS, so
   * that archives created on one platform can be extracted on any other.
   */
  private static void validateEntryNamePortable(@NonNull String entryName) throws IOException {
    for (String segment : entryName.split("/")) {
      if (".".equals(segment) || "..".equals(segment)) {
        throw new IOException("Archive entry escapes the extraction directory: " + entryName);
      }
      if (segment.isBlank() || segment.endsWith(".") || segment.endsWith(" ")) {
        throw new IOException("Archive entry has an unsafe path segment: " + entryName);
      }
      if (WINDOWS_ILLEGAL_CHARS.matcher(segment).find()) {
        throw new IOException("Archive entry contains characters illegal on Windows: " + entryName);
      }
      String base = segment.contains(".") ? segment.substring(0, segment.indexOf('.')) : segment;
      if (WINDOWS_RESERVED_NAMES.contains(base.toUpperCase(Locale.ROOT))) {
        throw new IOException("Archive entry uses a reserved device name: " + entryName);
      }
    }
  }

  private static boolean hasWindowsDrivePrefix(@NonNull String entryName) {

    return entryName.length() >= 2
        && Character.isLetter(entryName.charAt(0))
        && entryName.charAt(1) == ':';
  }

  /** Creates directories while rejecting symbolic links in the created or existing path. */
  private static void createSafeDirectories(@NonNull Path extractionRoot, @NonNull Path directory)
      throws IOException {

    final var normalizedRoot = extractionRoot.toAbsolutePath().normalize();
    final var normalizedDirectory = directory.toAbsolutePath().normalize();

    if (!normalizedDirectory.startsWith(normalizedRoot)) {
      throw new IOException("Directory escapes the extraction root: " + normalizedDirectory);
    }

    final var relativePath = normalizedRoot.relativize(normalizedDirectory);
    var current = normalizedRoot;

    for (Path component : relativePath) {
      current = current.resolve(component);

      if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
        if (Files.isSymbolicLink(current)) {
          throw new IOException("Symbolic link encountered during archive extraction: " + current);
        }
        if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
          throw new IOException("Expected a directory during archive extraction: " + current);
        }
      } else {
        Files.createDirectory(current);
      }
    }
  }

  private static void normalizeMetadata(@NonNull TarArchiveEntry entry, boolean directory) {
    // Normalize metadata for reproducibility
    entry.setModTime(0);
    entry.setUserName("");
    entry.setGroupName("");
    entry.setUserId(0);
    entry.setGroupId(0);
    entry.setMode(directory ? DIRECTORY_MODE : REGULAR_FILE_MODE);
  }

  private static String archiveEntryName(@NonNull Path relativePath) {
    List<String> elements = new ArrayList<>();
    for (Path element : relativePath) {
      elements.add(element.toString());
    }
    return String.join("/", elements);
  }

  private static void configureTarOutputStream(@NonNull TarArchiveOutputStream tarOutputStream) {
    tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
    tarOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
  }

  private static void validateSourceDirectory(@NonNull Path source) throws IOException {

    log.debug("Checking source directory {}", source);
    if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Source directory does not exist: " + source);
    }

    if (Files.isSymbolicLink(source)) {
      throw new IOException("Source directory must not be a symbolic link: " + source);
    }

    if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Source path is not a directory: " + source);
    }

    if (!Files.isReadable(source)) {
      throw new IOException("Source directory is not readable: " + source);
    }
  }

  private static void validateOutputDirectory(@NonNull Path outputDirectory) throws IOException {

    Files.createDirectories(outputDirectory);
    if (Files.isSymbolicLink(outputDirectory)) {
      throw new IOException("Output directory must not be a symbolic link: " + outputDirectory);
    }

    if (!Files.isDirectory(outputDirectory, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Output path is not a directory: " + outputDirectory);
    }

    if (!Files.isWritable(outputDirectory)) {
      throw new IOException("Output directory is not writable: " + outputDirectory);
    }
  }

  private static void validateArchive(@NonNull Path archive) throws IOException {
    if (Files.isSymbolicLink(archive)) {
      throw new IOException("Input archive must not be a symbolic link: " + archive);
    }

    if (!Files.isRegularFile(archive, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Input archive does not exist or is not a regular file: " + archive);
    }

    if (!Files.isReadable(archive)) {
      throw new IOException("Input archive is not readable: " + archive);
    }
  }

  private static void prepareEmptyOutputDirectory(@NonNull Path outputDirectory)
      throws IOException {

    if (Files.exists(outputDirectory, LinkOption.NOFOLLOW_LINKS)) {
      if (Files.isSymbolicLink(outputDirectory)) {
        throw new IOException(
            "Extraction directory must not be a symbolic link: " + outputDirectory);
      }

      if (!Files.isDirectory(outputDirectory, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException(
            "Extraction path already exists and is not a directory: " + outputDirectory);
      }

      try (var entries = Files.list(outputDirectory)) {
        if (entries.findAny().isPresent()) {
          throw new IOException("Extraction directory must be empty: " + outputDirectory);
        }
      }
    } else {
      Files.createDirectories(outputDirectory);
    }
  }

  private static Path getArchivePath(@NonNull Path sourceDirectory, @NonNull Path outputDirectory)
      throws IOException {
    final var fileName = sourceDirectory.getFileName();
    if (fileName == null || fileName.toString().isBlank()) {
      throw new IOException(
          "Cannot determine an archive name from source directory: " + sourceDirectory);
    }

    final var safeFileName = fileName.toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    if (safeFileName.isBlank() || ".".equals(safeFileName) || "..".equals(safeFileName)) {
      throw new IOException(
          "Cannot create a safe archive name from source directory: " + sourceDirectory);
    }

    final var archive = outputDirectory.resolve(safeFileName + TGZ_EXTENSION).normalize();

    if (!archive.startsWith(outputDirectory)) {
      throw new IOException("Generated archive path escapes the output directory: " + archive);
    }

    return archive;
  }

  private static void publishArchive(@NonNull Path temporaryArchive, @NonNull Path finalArchive)
      throws IOException {

    try {
      Files.move(
          temporaryArchive,
          finalArchive,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException _) {
      log.debug(
          "Atomic archive publication is not supported for {}. "
              + "Falling back to a regular replacement move.",
          finalArchive);
      Files.move(temporaryArchive, finalArchive, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static void cleanupPartialExtraction(@NonNull Path extractionRoot) {
    if (!Files.exists(extractionRoot, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    try {
      FileUtils.deleteDirectory(extractionRoot.toFile());
    } catch (IOException e) {
      log.warn("Failed to clean partial extraction directory {}", extractionRoot, e);
    }
  }
}
