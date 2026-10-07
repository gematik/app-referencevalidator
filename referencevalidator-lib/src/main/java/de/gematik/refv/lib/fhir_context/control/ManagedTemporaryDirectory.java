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
package de.gematik.refv.lib.fhir_context.control;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Watches the temporary directory and manages its lifecycle. */
final class ManagedTemporaryDirectory implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(ManagedTemporaryDirectory.class);
  private final SharedDirectory shared;
  private boolean closed;

  private ManagedTemporaryDirectory(@NonNull SharedDirectory shared) {
    this.shared = shared;
  }

  static ManagedTemporaryDirectory owned(@NonNull Path path) {
    return new ManagedTemporaryDirectory(new SharedDirectory(path));
  }

  synchronized ManagedTemporaryDirectory retain() {
    if (closed) {
      throw new IllegalStateException("Temporary directory lease is closed");
    }
    shared.references.incrementAndGet();
    return new ManagedTemporaryDirectory(shared);
  }

  @Override
  public synchronized void close() {
    if (!closed) {
      closed = true;
      shared.release();
    }
  }

  private static final class SharedDirectory {
    private final Path path;
    private final AtomicInteger references = new AtomicInteger(1);

    private SharedDirectory(Path path) {
      this.path = path.toAbsolutePath().normalize();
    }

    private void release() {
      int remaining = references.decrementAndGet();
      if (remaining < 0) {
        throw new IllegalStateException("Temporary directory lease count became negative");
      }
      if (remaining == 0) {
        deleteDirectory(path);
      }
    }

    private static void deleteDirectory(Path directory) {
      if (Files.notExists(directory)) {
        return;
      }
      try (Stream<Path> paths = Files.walk(directory)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
          Files.deleteIfExists(path);
        }
      } catch (IOException | UncheckedIOException e) {
        log.warn("Could not clean up temporary FHIR cache directory {}", directory, e);
      }
    }
  }
}
