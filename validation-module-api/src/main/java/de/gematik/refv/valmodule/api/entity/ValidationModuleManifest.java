/*-
 * #%L
 * Validation Module API
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
package de.gematik.refv.valmodule.api.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The complete manifest definition of a Validation Module (spec version 3.0).
 *
 * <p>Structure:
 *
 * <ul>
 *   <li>{@link #packageGroups()} — named, date-free sets of packages ("what gets loaded")
 *   <li>{@link #profileFamilies()} — profiles with versioned package-group timelines ("when does
 *       which group apply")
 *   <li>{@link #messageTransformations()} — reusable severity-rewrite rules referenced by name from
 *       package groups
 * </ul>
 *
 * <p>All cross-references are validated on construction (fail fast at load time): every {@link
 * PackageGroupReference} must reference an existing package group and every package group may only
 * reference existing message transformations.
 */
public record ValidationModuleManifest(
    @NonNull String configSpecVersion, // the specification version of the plugin
    @NonNull String name, // the name of the plugin
    @NonNull String version, // the plugin version
    @NonNull String author, // the plugin author
    @NonNull String description,
    @Nullable String specUrl,
    boolean errorOnUnknownProfile,
    boolean anyExtensionsAllowed,
    boolean requireExpansionBeforeValidation,
    @Nullable List<SuppressionRule> globalSuppressionRules,
    @NonNull Map<String, PackageGroup> packageGroups,
    @Nullable Map<String, List<MessageTransformation>> messageTransformations,
    @NonNull Map<String, ProfileFamily> profileFamilies,
    @Nullable List<String> ignoredCodeSystems,
    @Nullable List<String> ignoredValueSets) {

  public ValidationModuleManifest {
    Objects.requireNonNull(configSpecVersion);
    Objects.requireNonNull(name);
    Objects.requireNonNull(version);
    Objects.requireNonNull(author);
    Objects.requireNonNull(description);

    if (specUrl == null) {
      specUrl = "";
    }

    globalSuppressionRules = copyOrEmpty(globalSuppressionRules);
    packageGroups = copyOrEmpty(Objects.requireNonNull(packageGroups, "No package group defined"));
    if (packageGroups.isEmpty()) {
      throw new IllegalArgumentException(
          "The manifest must contain at least one package group (R1.2)");
    }
    messageTransformations = copyOrEmpty(messageTransformations);
    profileFamilies =
        copyOrEmpty(Objects.requireNonNull(profileFamilies, "No profile families defined"));
    if (profileFamilies.isEmpty()) {
      throw new IllegalArgumentException(
          "The manifest must contain at least one profile family (R1.3)");
    }
    ignoredCodeSystems = copyOrEmpty(ignoredCodeSystems);
    ignoredValueSets = copyOrEmpty(ignoredValueSets);

    requireKnownMessageTransformations(packageGroups, messageTransformations);
    requireKnownPackageGroups(profileFamilies, packageGroups);
  }

  private static @NonNull <T> List<T> copyOrEmpty(@Nullable List<T> list) {
    return list == null || list.isEmpty() ? Collections.emptyList() : List.copyOf(list);
  }

  private static @NonNull <T> Map<String, T> copyOrEmpty(@Nullable Map<String, T> map) {
    return map == null || map.isEmpty() ? Collections.emptyMap() : Map.copyOf(map);
  }

  /** Every message transformation referenced by a package group must be declared. */
  private static void requireKnownMessageTransformations(
      @NonNull Map<String, PackageGroup> packageGroups,
      @Nullable Map<String, List<MessageTransformation>> messageTransformations) {
    if (packageGroups.isEmpty()) {
      return;
    }

    for (var group : packageGroups.entrySet()) {
      final var groupMessageTransformations = group.getValue().messageTransformations();
      if (Objects.isNull(groupMessageTransformations)) {
        return;
      }

      for (var groupMessage : groupMessageTransformations) {
        if (Objects.isNull(messageTransformations)
            || !messageTransformations.containsKey(groupMessage)) {
          throw new IllegalArgumentException(
              "Package group '%s' references unknown message transformation '%s'"
                  .formatted(group.getKey(), groupMessage));
        }
      }
    }
  }

  /** Every validity period of every profile family version must reference a declared group. */
  private static void requireKnownPackageGroups(
      @Nullable Map<String, ProfileFamily> profileFamilies,
      @Nullable Map<String, PackageGroup> packageGroups) {
    if (Objects.isNull(profileFamilies) || profileFamilies.isEmpty()) {
      return;
    }

    for (var profileFamily : profileFamilies.entrySet()) {
      for (var familyEntry :
          Objects.requireNonNull(profileFamily.getValue().versions()).entrySet()) {
        final var validityPeriods = familyEntry.getValue().groups();
        for (var validityPeriod : validityPeriods) {
          if (Objects.isNull(packageGroups)
              || !packageGroups.containsKey(validityPeriod.packageGroupName())) {
            throw new IllegalArgumentException(
                String.format(
                    "Version '%s' of profile family '%s' references unknown"
                        + " package group '%s'",
                    familyEntry.getKey(),
                    profileFamily.getKey(),
                    validityPeriod.packageGroupName()));
          }
        }
      }
    }
  }
}
