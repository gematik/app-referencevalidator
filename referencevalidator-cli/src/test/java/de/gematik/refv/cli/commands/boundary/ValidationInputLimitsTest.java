/*-
 * #%L
 * referencevalidator-cli
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

package de.gematik.refv.cli.commands.boundary;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValidationInputLimitsTest {
  @TempDir Path tempDir;

  @Test
  void acceptsInputCountAtLimitAndRejectsTheNextFile() throws IOException {
    var first = write("first.json", "1");
    var second = write("second.json", "2");
    var third = write("third.json", "3");
    var limits = new ValidationInputCollector.Limits(2, 10, 20, 0);

    assertEquals(
        2,
        ValidationInputCollector.collect(List.of(first.toString(), second.toString()), limits)
            .size());
    assertThrows(
        IOException.class,
        () ->
            ValidationInputCollector.collect(
                List.of(first.toString(), second.toString(), third.toString()), limits));
  }

  @Test
  void acceptsFileSizeAtLimitAndRejectsOneByteAbove() throws IOException {
    var atLimit = write("at-limit.json", "12345");
    var overLimit = write("over-limit.json", "123456");
    var limits = new ValidationInputCollector.Limits(2, 5, 20, 0);

    assertEquals(1, ValidationInputCollector.collect(List.of(atLimit.toString()), limits).size());
    assertThrows(
        IOException.class,
        () -> ValidationInputCollector.collect(List.of(overLimit.toString()), limits));
  }

  @Test
  void readsOnlyWithinFileLimit() throws IOException {
    var file = write("bounded-read.json", "12345");
    var input = new ValidationInputCollector.ValidationInput(file, Files.size(file));

    assertArrayEquals(
        "12345".getBytes(StandardCharsets.UTF_8),
        ValidationInputCollector.readResourceBytes(input, 5));
    assertThrows(IOException.class, () -> ValidationInputCollector.readResourceBytes(input, 4));
  }

  @Test
  void acceptsAggregateBytesAtLimitAndRejectsOneByteAbove() throws IOException {
    var first = write("three-bytes.json", "123");
    var second = write("also-three.json", "456");
    var overByOne = write("four-bytes.json", "7890");
    var limits = new ValidationInputCollector.Limits(3, 4, 6, 0);

    assertEquals(
        2,
        ValidationInputCollector.collect(List.of(first.toString(), second.toString()), limits)
            .size());
    assertThrows(
        IOException.class,
        () ->
            ValidationInputCollector.collect(
                List.of(first.toString(), overByOne.toString()), limits));
  }

  @Test
  void acceptsDirectoryDepthAtLimitAndRejectsOneLevelDeeper() throws IOException {
    var atLimit = Files.createDirectories(tempDir.resolve("at-limit/child"));
    Files.writeString(atLimit.resolve("resource.json"), "{}");
    var overLimit = Files.createDirectories(tempDir.resolve("over-limit/child/grandchild"));
    Files.writeString(overLimit.resolve("resource.json"), "{}");
    var limits = new ValidationInputCollector.Limits(2, 10, 20, 1);

    assertEquals(
        1,
        ValidationInputCollector.collect(List.of(atLimit.getParent().toString()), limits).size());
    assertThrows(
        IOException.class,
        () ->
            ValidationInputCollector.collect(
                List.of(overLimit.getParent().getParent().toString()), limits));
  }

  @Test
  void acceptsAbsoluteLimitCeilingsAndRejectsValuesAboveThem() {
    assertEquals(
        ValidationInputCollector.MAX_INPUT_FILES,
        new ValidationInputCollector.Limits(ValidationInputCollector.MAX_INPUT_FILES, 1, 1, 0)
            .maximumFiles());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ValidationInputCollector.Limits(
                ValidationInputCollector.MAX_INPUT_FILES + 1, 1, 1, 0));

    assertEquals(
        ValidationInputCollector.MAX_FILE_BYTES,
        new ValidationInputCollector.Limits(
                1,
                ValidationInputCollector.MAX_FILE_BYTES,
                ValidationInputCollector.MAX_FILE_BYTES,
                0)
            .maximumFileBytes());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ValidationInputCollector.Limits(
                1,
                ValidationInputCollector.MAX_FILE_BYTES + 1,
                ValidationInputCollector.MAX_FILE_BYTES + 1,
                0));

    assertEquals(
        ValidationInputCollector.MAX_TOTAL_BYTES,
        new ValidationInputCollector.Limits(1, 1, ValidationInputCollector.MAX_TOTAL_BYTES, 0)
            .maximumTotalBytes());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ValidationInputCollector.Limits(
                1, 1, ValidationInputCollector.MAX_TOTAL_BYTES + 1, 0));

    assertEquals(
        ValidationInputCollector.MAX_DIRECTORY_DEPTH,
        new ValidationInputCollector.Limits(1, 1, 1, ValidationInputCollector.MAX_DIRECTORY_DEPTH)
            .maximumDirectoryDepth());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ValidationInputCollector.Limits(
                1, 1, 1, ValidationInputCollector.MAX_DIRECTORY_DEPTH + 1));
  }

  @Test
  void holdsInFlightValidationsAtLimitAndRejectsHigherConfiguredCap() throws Exception {
    int maximumInFlight = 2;
    var active = new AtomicInteger();
    var peak = new AtomicInteger();
    var startedCount = new AtomicInteger();
    var firstBatchStarted = new CountDownLatch(maximumInFlight);
    var releaseFirstBatch = new CountDownLatch(1);
    List<Callable<Integer>> tasks = new ArrayList<>();
    for (int value = 0; value < maximumInFlight + 1; value++) {
      int taskValue = value;
      tasks.add(
          () -> {
            int nowActive = active.incrementAndGet();
            peak.accumulateAndGet(nowActive, Math::max);
            startedCount.incrementAndGet();
            firstBatchStarted.countDown();
            try {
              assertTrue(releaseFirstBatch.await(5, TimeUnit.SECONDS));
              return taskValue;
            } finally {
              active.decrementAndGet();
            }
          });
    }

    try (var coordinator = Executors.newSingleThreadExecutor()) {
      try {
        var results =
            coordinator.submit(() -> BoundedValidationExecutor.runAll(tasks, maximumInFlight));
        assertTrue(firstBatchStarted.await(5, TimeUnit.SECONDS));
        assertEquals(maximumInFlight, active.get());
        assertEquals(maximumInFlight, startedCount.get());
        releaseFirstBatch.countDown();
        assertEquals(List.of(0, 1, 2), results.get(5, TimeUnit.SECONDS));
        assertEquals(3, startedCount.get());
        assertEquals(maximumInFlight, peak.get());
        assertThrows(
            IllegalArgumentException.class,
            () ->
                BoundedValidationExecutor.runAll(
                    tasks, BoundedValidationExecutor.MAX_IN_FLIGHT_VALIDATIONS + 1));
      } finally {
        releaseFirstBatch.countDown();
      }
    }
  }

  private Path write(String name, String content) throws IOException {
    var file = tempDir.resolve(name);
    return Files.writeString(file, content);
  }
}
