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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SnapshotArchiveCacheTest {
  @TempDir Path tempDir;

  @DisplayName("Given a missing cache directory, when looked up, then it is an empty cache")
  @Test
  void missingCacheDirectoryIsTreatedAsEmpty() {
    var cache = cache(10, 1024, Duration.ofDays(1));

    Assertions.assertTrue(cache.find(key(0)).isEmpty());
    Assertions.assertFalse(Files.exists(tempDir.resolve("cache")));
  }

  @DisplayName("Given an expired snapshot archive, when looked up, then it is removed")
  @Test
  void expiresOldArchive() throws Exception {
    var cache = cache(10, 1024, Duration.ofDays(1));
    Path source = archive("source.tgz", "archive");
    String key = key(1);
    cache.publish(key, source);
    Path entry = cachePath(key);
    Files.setLastModifiedTime(entry, FileTime.from(Instant.now().minus(Duration.ofDays(2))));

    Assertions.assertTrue(cache.find(key).isEmpty());
    Assertions.assertFalse(Files.exists(entry));
  }

  @DisplayName(
      "Given more entries than the cache limit, when publishing, then least-recent entries are evicted")
  @Test
  void evictsLeastRecentlyUsedEntriesAtEntryLimit() throws Exception {
    var cache = cache(2, 1024, Duration.ofDays(30));
    var first = key(1);
    var second = key(2);
    var third = key(3);
    Path source = archive("source.tgz", "archive");
    cache.publish(first, source);
    Files.setLastModifiedTime(
        cachePath(first), FileTime.from(Instant.now().minus(Duration.ofDays(1))));
    cache.publish(second, source);
    Assertions.assertTrue(cache.find(second).isPresent());

    cache.publish(third, source);

    Assertions.assertFalse(Files.exists(cachePath(first)));
    Assertions.assertTrue(Files.exists(cachePath(second)));
    Assertions.assertTrue(Files.exists(cachePath(third)));
  }

  @DisplayName(
      "Given archive bytes exceed the cache budget, when published, then they are not retained")
  @Test
  void skipsArchiveLargerThanCacheBudget() throws Exception {
    var cache = cache(10, 3, Duration.ofDays(30));
    String key = key(4);

    cache.publish(key, archive("large.tgz", "four"));

    Assertions.assertFalse(Files.exists(cachePath(key)));

    var aggregateCache = cache(10, 7, Duration.ofDays(30));
    String olderKey = key(5);
    String newestKey = key(6);
    Path smallArchive = archive("small.tgz", "four");
    aggregateCache.publish(olderKey, smallArchive);
    aggregateCache.publish(newestKey, smallArchive);

    Assertions.assertFalse(Files.exists(cachePath(olderKey)));
    Assertions.assertTrue(Files.exists(cachePath(newestKey)));
  }

  SnapshotArchiveCache cache(int maxEntries, long maxBytes, Duration maxAge) {
    return new SnapshotArchiveCache(
        tempDir.resolve("cache"),
        new SnapshotArchiveCache.RetentionPolicy(maxEntries, maxBytes, maxAge));
  }

  Path archive(String name, String content) throws Exception {
    Path source = tempDir.resolve(name);
    Files.writeString(source, content);
    return source;
  }

  Path cachePath(String key) {
    return tempDir.resolve("cache").resolve(key + ".tgz");
  }

  String key(int value) {
    return "%064x".formatted(value);
  }
}
