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
package de.gematik.refv.lib.package_resolver.control;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Utility class to perform the comparison of Semantic Version between different strings/packages.
 */
public final class SemanticVersion {
  private SemanticVersion() {}

  /**
   * Compares two strings, detecting if it is a semantic version.
   *
   * @param first first string to compare
   * @param second second string to compare
   * @return an integer representing the order between two compared strings
   */
  public static int compare(@NonNull String first, @NonNull String second) {
    Parsed a = Parsed.of(Objects.requireNonNull(first, "The first string must be defined"));
    Parsed b = Parsed.of(Objects.requireNonNull(second, "The second string must be defined"));
    int size = Math.max(a.numbers.size(), b.numbers.size());
    for (int i = 0; i < size; i++) {
      BigInteger x = i < a.numbers.size() ? a.numbers.get(i) : BigInteger.ZERO;
      BigInteger y = i < b.numbers.size() ? b.numbers.get(i) : BigInteger.ZERO;
      int result = x.compareTo(y);
      if (result != 0) return result;
    }
    if (a.qualifier.isEmpty() && !b.qualifier.isEmpty()) return 1;
    if (!a.qualifier.isEmpty() && b.qualifier.isEmpty()) return -1;
    return a.qualifier.compareTo(b.qualifier);
  }

  private record Parsed(@NonNull List<BigInteger> numbers, @NonNull String qualifier) {
    static Parsed of(String value) {
      String[] split = value.split("-", 2);
      String[] parts = split[0].split("\\.");
      List<BigInteger> numbers = new ArrayList<>();
      for (String part : parts) {
        if (!part.matches("\\d+")) return new Parsed(List.of(), value);
        numbers.add(new BigInteger(part));
      }
      return new Parsed(List.copyOf(numbers), split.length == 2 ? split[1] : "");
    }
  }
}
