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
package de.gematik.refv.lib.fhir_context.entity;

import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.valmodule.api.entity.ProfileFamily;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Module profile support and unknown-profile behavior projected into validation policy. */
public record ModuleProfilePolicy(
    boolean errorOnUnknownProfile, @NonNull List<ProfileFamilySupport> profileFamilies) {

  public ModuleProfilePolicy {
    profileFamilies = List.copyOf(Objects.requireNonNull(profileFamilies));
  }

  /**
   * Projects only profile support and unknown-profile behavior from the module manifest.
   *
   * @param manifest loaded module manifest
   * @return immutable profile policy for validation contexts
   */
  public static @NonNull ModuleProfilePolicy fromManifest(
      @NonNull ValidationModuleManifest manifest) {
    Objects.requireNonNull(manifest);
    return new ModuleProfilePolicy(
        manifest.errorOnUnknownProfile(),
        manifest.profileFamilies().values().stream().map(ModuleProfilePolicy::supportOf).toList());
  }

  /**
   * Returns whether the module supports the canonical and version, including short patch aliases.
   */
  public boolean supports(@NonNull ProfileCanonical profile) {
    return resolveProfileVersion(profile).isPresent();
  }

  /**
   * Resolves a canonical using all profile versions declared by the module. An exact supported
   * version wins. Otherwise, a numeric {@code X.Y} may resolve to the highest declared {@code
   * X.Y.Z} patch version. A versionless canonical uses the declared default version.
   *
   * @param profile canonical and requested version
   * @return the same canonical with its resolved version, or empty when unsupported
   */
  public @NonNull Optional<ProfileCanonical> resolveProfileVersion(
      @NonNull ProfileCanonical profile) {
    return resolveProfileVersion(profile, null);
  }

  /**
   * Resolves a canonical, optionally restricting candidates to versions loaded in a context. When
   * {@code loadedVersions} is supplied, the exact-first and highest-patch rules apply only to
   * versions in that collection.
   *
   * @param profile canonical and requested version
   * @param loadedVersions available versions, or null to use all module-declared versions
   * @return the same canonical with its resolved version, or empty when unsupported or unavailable
   */
  public @NonNull Optional<ProfileCanonical> resolveProfileVersion(
      @NonNull ProfileCanonical profile, @Nullable Collection<String> loadedVersions) {
    Objects.requireNonNull(profile);
    var profileUrl = profile.canonical().toString();
    ProfileFamilySupport prefixFallback = null;
    for (var family : profileFamilies) {
      if (!profileUrl.startsWith(family.urlPrefix())) {
        continue;
      }
      if (prefixFallback == null) {
        prefixFallback = family;
      }
      var profileName = profileUrl.substring(family.urlPrefix().length());
      if (family.profiles().contains(profileName)) {
        return family
            .resolveVersion(profile.version(), loadedVersions)
            .map(version -> new ProfileCanonical(profile.canonical(), version));
      }
    }
    return prefixFallback == null
        ? Optional.empty()
        : prefixFallback
            .resolveVersion(profile.version(), loadedVersions)
            .map(version -> new ProfileCanonical(profile.canonical(), version));
  }

  /**
   * Resolves a requested version against versions available to the caller. An exact match wins. If
   * the request is numeric {@code X.Y}, the highest numeric {@code X.Y.Z} candidate is returned;
   * other abbreviated or non-numeric versions are not expanded.
   *
   * @param requestedVersion requested version, possibly abbreviated as {@code X.Y}
   * @param availableVersions full or exact versions available for resolution
   * @return the exact or resolved version, or empty when no candidate matches
   */
  public static @NonNull Optional<String> resolveLoadedVersion(
      @Nullable String requestedVersion, @NonNull Collection<String> availableVersions) {
    Objects.requireNonNull(availableVersions);
    if (requestedVersion == null || requestedVersion.isBlank()) {
      return Optional.empty();
    }
    if (availableVersions.contains(requestedVersion)) {
      return Optional.of(requestedVersion);
    }
    if (!requestedVersion.matches("\\d+\\.\\d+")) {
      return Optional.empty();
    }
    var majorMinorPrefix = requestedVersion + ".";
    return availableVersions.stream()
        .filter(version -> version.startsWith(majorMinorPrefix))
        .filter(version -> version.matches("\\d+\\.\\d+\\.\\d+"))
        .max(ModuleProfilePolicy::comparePatchVersions);
  }

  /** Compares the three numeric components of already-validated patch versions. */
  private static int comparePatchVersions(String left, String right) {
    var leftParts = left.split("\\.");
    var rightParts = right.split("\\.");
    for (var index = 0; index < leftParts.length; index++) {
      var comparison =
          new BigInteger(leftParts[index]).compareTo(new BigInteger(rightParts[index]));
      if (comparison != 0) {
        return comparison;
      }
    }
    return 0;
  }

  /** Copies the profile lookup data needed from one manifest family. */
  private static ProfileFamilySupport supportOf(ProfileFamily family) {
    return new ProfileFamilySupport(
        family.canonicalBase(),
        family.defaultVersion(),
        family.versions().keySet(),
        family.profiles().keySet());
  }

  public record ProfileFamilySupport(
      @NonNull String canonicalBase,
      @Nullable String defaultVersion,
      @NonNull Set<String> versions,
      @NonNull Set<String> profiles) {

    public ProfileFamilySupport {
      Objects.requireNonNull(canonicalBase);
      versions = Set.copyOf(Objects.requireNonNull(versions));
      profiles = Set.copyOf(Objects.requireNonNull(profiles));
    }

    /** Normalizes this family's canonical base for profile-name matching with a trailing slash. */
    private String urlPrefix() {
      return canonicalBase.endsWith("/") ? canonicalBase : canonicalBase + "/";
    }

    /**
     * Resolves an exact version, family default, or highest matching patch in the available set.
     */
    private Optional<String> resolveVersion(
        @Nullable String profileVersion, @Nullable Collection<String> loadedVersions) {
      var availableVersions =
          loadedVersions == null
              ? versions
              : versions.stream().filter(loadedVersions::contains).toList();
      if (profileVersion == null || profileVersion.isBlank()) {
        return Optional.ofNullable(defaultVersion).filter(availableVersions::contains);
      }
      return ModuleProfilePolicy.resolveLoadedVersion(profileVersion, availableVersions);
    }
  }
}
