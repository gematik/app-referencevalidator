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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Utility Class that sets limits for avoiding the loading of big resources which might cause
 * resource starvation.
 */
final class ValidationInputCollector {
  static final int MAX_INPUT_FILES = 10_000;
  static final long MAX_FILE_BYTES = 100L * 1024 * 1024;
  static final long MAX_TOTAL_BYTES = 1024L * 1024 * 1024;
  static final int MAX_DIRECTORY_DEPTH = 64;

  static final Limits DEFAULT_LIMITS = new Limits(1_000, 50L * 1024 * 1024, 500L * 1024 * 1024, 32);

  private ValidationInputCollector() {}

  static @NonNull List<ValidationInput> collect(@NonNull List<String> sources) throws IOException {
    return collect(sources, DEFAULT_LIMITS);
  }

  static @NonNull List<ValidationInput> collect(
      @NonNull List<String> sources, @NonNull Limits limits) throws IOException {
    Objects.requireNonNull(sources, "The validation sources cannot be null");
    Objects.requireNonNull(limits, "The validation input limits cannot be null");

    var collector = new Collector(limits);
    for (var source : sources) {
      collector.visit(Path.of(source));
    }
    return List.copyOf(collector.inputs);
  }

  static byte @NonNull [] readResourceBytes(@NonNull ValidationInput input, long maximumFileBytes)
      throws IOException {
    Objects.requireNonNull(input, "The validation input cannot be null");
    if (maximumFileBytes < 0 || maximumFileBytes >= Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "The maximum resource size is outside the supported range");
    }
    if (input.size() > maximumFileBytes) {
      throw new IOException(
          "FHIR resource exceeds the maximum file size of "
              + maximumFileBytes
              + " bytes: "
              + input.path());
    }
    var attributes =
        Files.readAttributes(input.path(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    if (!attributes.isRegularFile() || attributes.size() != input.size()) {
      throw new IOException("FHIR resource changed during validation: " + input.path());
    }

    final byte[] content;
    try (var inputStream = Files.newInputStream(input.path(), LinkOption.NOFOLLOW_LINKS)) {
      content = inputStream.readNBytes(Math.toIntExact(maximumFileBytes + 1));
    }
    if (content.length > maximumFileBytes) {
      throw new IOException(
          "FHIR resource exceeds the maximum file size of "
              + maximumFileBytes
              + " bytes: "
              + input.path());
    }
    if (content.length != input.size()) {
      throw new IOException("FHIR resource size changed during validation: " + input.path());
    }
    return content;
  }

  record Limits(
      int maximumFiles, long maximumFileBytes, long maximumTotalBytes, int maximumDirectoryDepth) {
    Limits {
      if (maximumFiles < 1
          || maximumFiles > MAX_INPUT_FILES
          || maximumFileBytes < 1
          || maximumFileBytes > MAX_FILE_BYTES
          || maximumTotalBytes < 1
          || maximumTotalBytes > MAX_TOTAL_BYTES
          || maximumDirectoryDepth < 0
          || maximumDirectoryDepth > MAX_DIRECTORY_DEPTH) {
        throw new IllegalArgumentException("Validation input limits exceed the supported range");
      }
    }
  }

  record ValidationInput(@NonNull Path path, long size) {
    ValidationInput {
      Objects.requireNonNull(path, "The validation input path cannot be null");
    }
  }

  private static final class Collector {
    private final Limits limits;
    private final List<ValidationInput> inputs = new ArrayList<>();
    private long totalBytes;

    private Collector(Limits limits) {
      this.limits = limits;
    }

    private void visit(Path source) throws IOException {
      var attributes =
          Files.readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (attributes.isSymbolicLink()) {
        throw new IOException("Symbolic links are not accepted as validation inputs: " + source);
      }
      if (attributes.isDirectory()) {
        visitDirectory(source);
        return;
      }
      addFile(source, attributes);
    }

    private void visitDirectory(Path root) throws IOException {
      Files.walkFileTree(
          root,
          java.util.Set.of(),
          limits.maximumDirectoryDepth() + 1,
          new SimpleFileVisitor<>() {
            @Override
            public @NonNull FileVisitResult preVisitDirectory(
                @NonNull Path directory, @NonNull BasicFileAttributes attributes)
                throws IOException {
              int depth = root.equals(directory) ? 0 : root.relativize(directory).getNameCount();
              if (depth > limits.maximumDirectoryDepth()) {
                throw new IOException(
                    "Validation directory depth exceeds "
                        + limits.maximumDirectoryDepth()
                        + ": "
                        + directory);
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public @NonNull FileVisitResult visitFile(
                @NonNull Path file, @NonNull BasicFileAttributes attributes) throws IOException {
              if (attributes.isSymbolicLink()) {
                throw new IOException(
                    "Symbolic links are not accepted as validation inputs: " + file);
              }
              if (attributes.isDirectory()) {
                throw new IOException(
                    "Validation directory depth exceeds "
                        + limits.maximumDirectoryDepth()
                        + ": "
                        + file);
              }
              addFile(file, attributes);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public @NonNull FileVisitResult visitFileFailed(
                @NonNull Path file, @NonNull IOException error) throws IOException {
              throw error;
            }

            @Override
            public @NonNull FileVisitResult postVisitDirectory(
                @NonNull Path directory, IOException error) throws IOException {
              if (error != null) {
                throw error;
              }
              return FileVisitResult.CONTINUE;
            }
          });
    }

    private void addFile(Path file, BasicFileAttributes attributes) throws IOException {
      if (!attributes.isRegularFile()) {
        throw new IOException("Validation input is not a regular file: " + file);
      }
      if (inputs.size() >= limits.maximumFiles()) {
        throw new IOException(
            "Validation input count exceeds the maximum of " + limits.maximumFiles());
      }

      long size = attributes.size();
      if (size > limits.maximumFileBytes()) {
        throw new IOException(
            "FHIR resource exceeds the maximum file size of "
                + limits.maximumFileBytes()
                + " bytes: "
                + file);
      }
      if (size > limits.maximumTotalBytes() - totalBytes) {
        throw new IOException(
            "Validation inputs exceed the maximum total size of "
                + limits.maximumTotalBytes()
                + " bytes");
      }

      totalBytes += size;
      inputs.add(new ValidationInput(file, size));
    }
  }
}
