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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import org.jspecify.annotations.NonNull;

/** Limits the maximum number of parallel validation executions. */
final class BoundedValidationExecutor {
  static final int MAX_IN_FLIGHT_VALIDATIONS = 32;

  private BoundedValidationExecutor() {}

  static <T> @NonNull List<T> runAll(
      @NonNull List<? extends Callable<T>> tasks, int maximumInFlight)
      throws InterruptedException, ExecutionException {
    if (maximumInFlight < 1) {
      throw new IllegalArgumentException("Maximum in-flight validations must be at least one");
    }
    if (maximumInFlight > MAX_IN_FLIGHT_VALIDATIONS) {
      throw new IllegalArgumentException(
          "Maximum in-flight validations cannot exceed " + MAX_IN_FLIGHT_VALIDATIONS);
    }

    var results = new ArrayList<T>(java.util.Collections.nCopies(tasks.size(), null));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      CompletionService<IndexedResult<T>> completions = new ExecutorCompletionService<>(executor);
      int nextTask = 0;
      int inFlight = 0;
      while (nextTask < tasks.size() || inFlight > 0) {
        while (nextTask < tasks.size() && inFlight < maximumInFlight) {
          int index = nextTask++;
          completions.submit(() -> new IndexedResult<>(index, tasks.get(index).call()));
          inFlight++;
        }
        var completed = completions.take().get();
        results.set(completed.index(), completed.value());
        inFlight--;
      }
    }
    return List.copyOf(results);
  }

  private record IndexedResult<T>(int index, T value) {}
}
