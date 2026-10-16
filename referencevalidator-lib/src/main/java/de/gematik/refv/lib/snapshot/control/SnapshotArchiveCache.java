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
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistent filesystem cache for successfully generated FHIR snapshot package archives.
 *
 * <p>This class is package-private because it is an implementation detail of snapshot processing.
 */
final class SnapshotArchiveCache {

  private static final Logger log = LoggerFactory.getLogger(SnapshotArchiveCache.class);
  private static final String CACHE_EXTENSION = LocalArchive.ARCHIVE_PACKAGE_EXTENSION;
  private static final String TEMP_EXTENSION = ".tmp";
  private static final Object CACHE_LOCK = new Object();
  private static final RetentionPolicy DEFAULT_RETENTION =
      new RetentionPolicy(500, 1024L * 1024 * 1024, Duration.ofDays(90));
  private final Path directory;
  private final RetentionPolicy retention;

  SnapshotArchiveCache(Path directory) {
    this(directory, DEFAULT_RETENTION);
  }

  SnapshotArchiveCache(Path directory, RetentionPolicy retention) {
    this.directory = Objects.requireNonNull(directory).toAbsolutePath().normalize();
    this.retention = Objects.requireNonNull(retention);
  }

  Optional<Path> find(String cacheKey) {
    synchronized (CACHE_LOCK) {
      Path candidate = cachePath(cacheKey);
      try {
        prune(Instant.now(), null);
        if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)
            || Files.size(candidate) == 0) {
          return Optional.empty();
        }
        if (isExpired(candidate, Instant.now())) {
          Files.deleteIfExists(candidate);
          return Optional.empty();
        }
        updateLastAccess(candidate);
        return Optional.of(candidate);
      } catch (IOException e) {
        log.warn("Could not inspect snapshot archive cache entry {}", candidate, e);
        return Optional.empty();
      }
    }
  }

  Path materialize(Path cachedArchive, Path outputDirectory, String fileName) throws IOException {
    Files.createDirectories(outputDirectory);
    Path normalizedOutput = outputDirectory.toAbsolutePath().normalize();
    Path target = normalizedOutput.resolve(fileName).normalize();
    if (!target.startsWith(normalizedOutput)) {
      throw new IOException("Archive target escapes output directory: " + target);
    }
    Files.copy(cachedArchive, target, StandardCopyOption.REPLACE_EXISTING);
    return target;
  }

  Optional<Path> materialize(String cacheKey, Path outputDirectory, String fileName)
      throws IOException {
    synchronized (CACHE_LOCK) {
      Optional<Path> cachedArchive = find(cacheKey);
      return cachedArchive.isPresent()
          ? Optional.of(materialize(cachedArchive.orElseThrow(), outputDirectory, fileName))
          : Optional.empty();
    }
  }

  void publish(String cacheKey, Path generatedArchive) throws IOException {
    synchronized (CACHE_LOCK) {
      Files.createDirectories(directory);
      Instant now = Instant.now();
      prune(now, null);
      Path target = cachePath(cacheKey);
      if (Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) && Files.size(target) > 0) {
        Files.setLastModifiedTime(target, FileTime.from(now));
        prune(now, target);
        return;
      }
      if (Files.size(generatedArchive) > retention.maxBytes()) {
        log.debug(
            "Snapshot archive {} exceeds the cache size limit; skipping cache", generatedArchive);
        return;
      }

      Path temporary = Files.createTempFile(directory, cacheKey + "-", TEMP_EXTENSION);
      try {
        Files.copy(generatedArchive, temporary, StandardCopyOption.REPLACE_EXISTING);
        Files.setLastModifiedTime(temporary, FileTime.from(now));
        try {
          Files.move(
              temporary,
              target,
              StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException _) {
          Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
      } finally {
        Files.deleteIfExists(temporary);
      }
      prune(now, target);
    }
  }

  private void prune(Instant now, Path newestEntry) throws IOException {
    if (!Files.isDirectory(directory)) {
      return;
    }
    deleteExpiredTemporaryFiles(now);

    List<CacheEntry> entries = cacheEntries();
    Instant expiresBefore = now.minus(retention.maxAge());
    deleteExpiredEntries(entries, expiresBefore);
    var retained = retainedEntries(entries, expiresBefore);
    evictEntries(retained, newestEntry);
  }

  private void deleteExpiredTemporaryFiles(Instant now) throws IOException {
    try (Stream<Path> paths = Files.list(directory)) {
      for (Path path : paths.toList()) {
        if (path.getFileName().toString().endsWith(TEMP_EXTENSION) && isExpired(path, now)) {
          Files.deleteIfExists(path);
        }
      }
    }
  }

  private List<CacheEntry> cacheEntries() throws IOException {
    try (Stream<Path> paths = Files.list(directory)) {
      return paths
          .filter(this::isCacheArchive)
          .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
          .map(this::cacheEntry)
          .flatMap(Optional::stream)
          .toList();
    }
  }

  private void deleteExpiredEntries(List<CacheEntry> entries, Instant expiresBefore)
      throws IOException {
    for (CacheEntry entry : entries) {
      if (entry.lastAccess().isBefore(expiresBefore)) {
        Files.deleteIfExists(entry.path());
      }
    }
  }

  private List<CacheEntry> retainedEntries(List<CacheEntry> entries, Instant expiresBefore) {
    return entries.stream()
        .filter(entry -> !entry.lastAccess().isBefore(expiresBefore))
        .sorted(
            Comparator.comparing(CacheEntry::lastAccess)
                .thenComparing(entry -> entry.path().toString()))
        .toList();
  }

  private void evictEntries(List<CacheEntry> retained, Path newestEntry) throws IOException {
    long totalBytes =
        retained.stream()
            .map(CacheEntry::size)
            .reduce(
                0L, (total, size) -> Long.MAX_VALUE - total < size ? Long.MAX_VALUE : total + size);
    int entryCount = retained.size();
    for (CacheEntry entry : retained) {
      boolean overLimit = entryCount > retention.maxEntries() || totalBytes > retention.maxBytes();
      boolean isNewestEntry = newestEntry != null && entry.path().equals(newestEntry);
      if (overLimit && !isNewestEntry) {
        Files.deleteIfExists(entry.path());
        entryCount--;
        totalBytes -= entry.size();
      }
    }
  }

  private void updateLastAccess(Path candidate) {
    try {
      Files.setLastModifiedTime(candidate, FileTime.from(Instant.now()));
    } catch (IOException e) {
      log.debug("Could not update snapshot archive cache access time for {}", candidate, e);
    }
  }

  private Optional<CacheEntry> cacheEntry(Path path) {
    try {
      return Optional.of(
          new CacheEntry(path, Files.size(path), Files.getLastModifiedTime(path).toInstant()));
    } catch (IOException e) {
      log.debug("Could not inspect snapshot archive cache entry {}", path, e);
      return Optional.empty();
    }
  }

  private boolean isExpired(Path path, Instant now) throws IOException {
    return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS)
        .toInstant()
        .isBefore(now.minus(retention.maxAge()));
  }

  private boolean isCacheArchive(Path path) {
    String filename = path.getFileName().toString();
    if (!filename.endsWith(CACHE_EXTENSION) || filename.length() != 64 + CACHE_EXTENSION.length()) {
      return false;
    }
    return filename.substring(0, 64).matches("[0-9a-f]{64}");
  }

  private Path cachePath(String cacheKey) {
    if (!cacheKey.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("Invalid snapshot archive cache key");
    }
    return directory.resolve(cacheKey + CACHE_EXTENSION).normalize();
  }

  record RetentionPolicy(int maxEntries, long maxBytes, Duration maxAge) {
    RetentionPolicy {
      Objects.requireNonNull(maxAge);
      if (maxEntries < 1 || maxBytes < 1 || maxAge.isNegative() || maxAge.isZero()) {
        throw new IllegalArgumentException("Snapshot cache retention limits must be positive");
      }
    }
  }

  private record CacheEntry(Path path, long size, Instant lastAccess) {}
}
