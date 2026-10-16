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
package de.gematik.refv.lib.package_resolver.entity;

import de.gematik.refv.lib.exceptions.CyclicDependencyException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Defines the dependency graph across FHIR packages.
 *
 * <p>An edge {@code dependency -> dependent} means that {@code dependency} must appear before
 * {@code dependent} in the resulting order.
 */
public final class DependencyGraph {

  private final Map<PackageId, Set<PackageId>> out = new LinkedHashMap<>();
  private final Map<PackageId, Integer> indegree = new LinkedHashMap<>();

  /**
   * Adds a node to the graph if it does not already exist.
   *
   * @param node the node to add
   * @throws NullPointerException if {@code node} is {@code null}
   */
  public void addNode(@NonNull PackageId node) {
    Objects.requireNonNull(node, "node must not be null");

    out.computeIfAbsent(node, ignored -> new LinkedHashSet<>());
    indegree.putIfAbsent(node, 0);
  }

  /**
   * Adds a dependency relation to the graph.
   *
   * <p>The resulting edge points from {@code dependency} to {@code dependent}, meaning that {@code
   * dependency} must occur before {@code dependent}.
   *
   * @param dependency the dependency
   * @param dependent the package depending on it
   */
  public void addDependency(@NonNull PackageId dependency, @NonNull PackageId dependent) {
    Objects.requireNonNull(dependency, "dependency must not be null");
    Objects.requireNonNull(dependent, "dependent must not be null");

    addNode(dependency);
    addNode(dependent);

    Set<PackageId> dependents = out.get(dependency);

    if (dependents.add(dependent)) {
      indegree.compute(dependent, (ignored, degree) -> Objects.requireNonNullElse(degree, 0) + 1);
    }
  }

  /**
   * Returns the nodes in dependency-first topological order.
   *
   * <p>If {@code A} is a dependency of {@code B}, {@code A} appears before {@code B} in the
   * returned list.
   *
   * @return immutable dependency-first ordering
   * @throws CyclicDependencyException if the graph contains a cycle
   * @throws IllegalStateException if the graph contains an inconsistent state
   */
  public @NonNull List<PackageId> dependencyFirstOrder() {
    Map<PackageId, Integer> counts = new LinkedHashMap<>(indegree);
    ArrayDeque<PackageId> queue = new ArrayDeque<>();

    counts.forEach(
        (node, degree) -> {
          if (degree == 0) {
            queue.addLast(node);
          }
        });

    List<PackageId> result = new ArrayList<>(counts.size());

    while (!queue.isEmpty()) {
      PackageId node = queue.removeFirst();
      result.add(node);

      for (PackageId dependent : out.getOrDefault(node, Collections.emptySet())) {
        Integer degree = counts.get(dependent);

        if (degree == null) {
          throw new IllegalStateException(
              "Dependency graph is inconsistent: node is present in the "
                  + "adjacency graph but missing from the indegree graph: "
                  + dependent);
        }

        int newDegree = degree - 1;

        if (newDegree < 0) {
          throw new IllegalStateException(
              "Dependency graph is inconsistent: negative indegree for node: " + dependent);
        }

        counts.put(dependent, newDegree);

        if (newDegree == 0) {
          queue.addLast(dependent);
        }
      }
    }

    if (result.size() != counts.size()) {
      throw new CyclicDependencyException("Cyclic package dependency detected");
    }

    return List.copyOf(result);
  }
}
