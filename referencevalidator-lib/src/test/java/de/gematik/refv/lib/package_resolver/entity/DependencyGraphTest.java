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
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DependencyGraphTest {
  @Test
  void ordersDiamondAndDisconnectedRootsWithoutDuplicatingEdges() {
    var graph = new DependencyGraph();
    var root = new PackageId("root", "1.0.0");
    var left = new PackageId("left", "1.0.0");
    var right = new PackageId("right", "1.0.0");
    var shared = new PackageId("shared", "1.0.0");
    var independent = new PackageId("independent", "1.0.0");
    graph.addNode(root);
    graph.addDependency(left, root);
    graph.addDependency(right, root);
    graph.addDependency(shared, left);
    graph.addDependency(shared, right);
    graph.addDependency(shared, right);
    graph.addNode(independent);

    var order = graph.dependencyFirstOrder();

    Assertions.assertEquals(5, order.size());
    Assertions.assertEquals(5L, order.stream().distinct().count());
    Assertions.assertTrue(order.indexOf(shared) < order.indexOf(left));
    Assertions.assertTrue(order.indexOf(shared) < order.indexOf(right));
    Assertions.assertTrue(order.indexOf(left) < order.indexOf(root));
    Assertions.assertTrue(order.indexOf(right) < order.indexOf(root));
    Assertions.assertEquals(order, graph.dependencyFirstOrder());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void rejectsCyclesEvenWithAnIndependentReadyNode(int length) {
    var graph = new DependencyGraph();
    graph.addNode(new PackageId("independent", "1.0.0"));
    for (int index = 0; index < length; index++) {
      graph.addDependency(
          new PackageId("node-" + index, "1.0.0"),
          new PackageId("node-" + ((index + 1) % length), "1.0.0"));
    }
    Assertions.assertThrows(CyclicDependencyException.class, graph::dependencyFirstOrder);
  }

  @Test
  void handlesEmptyGraphsAndDistinctPackageVersions() {
    var graph = new DependencyGraph();
    Assertions.assertEquals(List.of(), graph.dependencyFirstOrder());
    var earlier = new PackageId("profile", "1.0.0");
    var later = new PackageId("profile", "2.0.0");
    graph.addDependency(earlier, later);
    Assertions.assertEquals(List.of(earlier, later), graph.dependencyFirstOrder());
  }
}
