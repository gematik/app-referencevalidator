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
import de.gematik.refv.lib.valmodule.entity.ValidationModule;
import de.gematik.refv.valmodule.api.entity.MessageTransformation;
import de.gematik.refv.valmodule.api.entity.PackageGroup;
import de.gematik.refv.valmodule.api.entity.ProfileDefinition;
import de.gematik.refv.valmodule.api.entity.ProfileFamily;
import de.gematik.refv.valmodule.api.entity.ProfileVersion;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Pre-computed lookup index for loading dependencies efficiently.
 *
 * <p>Every supported profile canonical ({@code url|version}) is resolved once at load time into a
 * {@link ResolvedVersion}.
 *
 * <p>Resolution rules (mirroring the configuration specification):
 *
 * <ol>
 *   <li>Canonicals without a version are served by the family's explicitly declared {@code
 *       defaultVersion} (registered under {@link ProfileCanonical#DEFAULT_VERSION} at build time).
 *   <li>With a reference date, the first period (declaration order) containing the date wins.
 *       Overlapping periods are allowed, declaration order is the priority.
 *   <li>Without a reference date, the single open-ended period is used and if every period is
 *       closed, resolution fails by design.
 *   <li>Profiles not explicitly declared by a family are served by a canonical-base prefix
 *       fallback: if the profile URL starts with a family's {@code canonicalBase + "/"}, the
 *       family's versions apply. This allows modules to support a whole specification by declaring
 *       only the canonical base, without enumerating every profile.
 * </ol>
 */
public final class ValidationModuleIndex {
  /**
   * A resolved validity period: a package group name and its complete, flattened package set.
   *
   * @param packageGroup the name of the package group active in this period
   * @param validFrom first day (inclusive) of the period
   * @param validTill last day (inclusive) of the period, or null for the open-ended period
   * @param packages the complete set of dependencies to load, in declaration order
   */
  private record ResolvedPeriod(
      @NonNull String packageGroup,
      @NonNull LocalDate validFrom,
      @Nullable LocalDate validTill,
      @NonNull List<String> packages) {

    public boolean isOpenEnded() {
      return validTill == null;
    }

    public boolean contains(@NonNull LocalDate date) {
      return !date.isBefore(validFrom) && (validTill == null || !date.isAfter(validTill));
    }
  }

  /**
   * Everything the validator needs for one profile canonical ({@code url|version}).
   *
   * @param validityPeriods ordered timeline of package groups; first match wins
   * @param openEndedPeriod the currently active period, used when no reference date is available;
   *     null if all periods are closed
   * @param messageTransformations aggregated, duplicate-free transformations of all package groups
   *     bound to this version
   * @param validityDateSource the FHIRPath expression extracting the reference date, or null
   */
  private record ResolvedVersion(
      @NonNull List<ResolvedPeriod> validityPeriods,
      @Nullable ResolvedPeriod openEndedPeriod,
      @NonNull List<MessageTransformation> messageTransformations,
      @Nullable String validityDateSource) {

    /** Returns the first period (declaration order) containing the reference date. */
    public Optional<ResolvedPeriod> periodFor(@NonNull LocalDate referenceDate) {
      return validityPeriods.stream().filter(period -> period.contains(referenceDate)).findFirst();
    }

    /** Returns the open-ended (currently active) period, if any. */
    public Optional<ResolvedPeriod> fallbackPeriod() {
      return Optional.ofNullable(openEndedPeriod);
    }
  }

  /**
   * Internal record that keeps track of the association between Profile Prefix and Family of
   * Packages.
   */
  private record ProfileFamilyByCanonical(
      @NonNull String prefix, @NonNull ProfileFamily profileFamily) {}

  private static final String INVALID_PROFILE_URL = "Invalid profile url specified";

  /// Contains the Runtime Information of a module, including the parsed configuration itself
  private final ValidationModule validationModule;

  /// The single lookup map: profile canonical ("url|version") -> resolved version
  private final Map<String, ResolvedVersion> resolvedVersionsByCanonical;

  /// All known profile URLs (without version), for fast profile-support checks
  private final Set<String> knownProfileUrls;

  /// Families by their canonical base prefix (with trailing '/'), for the prefix fallback
  private final List<ProfileFamilyByCanonical> familiesByPrefix;

  /**
   * Creates a new Index based on the module configuration {@link ValidationModuleManifest}.
   *
   * @param validationModule the {@link ValidationModule} definition loaded.
   */
  public ValidationModuleIndex(@NonNull ValidationModule validationModule) {
    this.validationModule = Objects.requireNonNull(validationModule);
    this.resolvedVersionsByCanonical = buildIndex(validationModule.configuration());
    this.knownProfileUrls = collectProfileUrls(validationModule.configuration());
    this.familiesByPrefix = collectFamiliesByPrefix(validationModule.configuration());
  }

  /** Returns the validation module path. */
  public @NonNull Path getValidationModulePath() {
    return validationModule.modulePath();
  }

  /** Returns the validation module path. */
  public @NonNull ValidationModuleManifest getValidationModuleConfiguration() {
    return validationModule.configuration();
  }

  // ---------------------------------------------
  // Profile lookups (single-map model + prefix fallback)
  // ---------------------------------------------

  /**
   * Returns true if the profile URL is declared by any profile family of this module, or falls
   * under the canonical base of a family (prefix fallback).
   */
  public boolean isProfileDefined(@NonNull String profileUrl) {
    Objects.requireNonNull(profileUrl, INVALID_PROFILE_URL);
    return knownProfileUrls.contains(profileUrl) || matchingFamily(profileUrl).isPresent();
  }

  /**
   * Resolves a profile canonical. Explicitly declared profiles are served by a single map lookup; a
   * null/blank version is served by the family's declared {@code defaultVersion}. If no exact
   * canonical matches, the canonical-base prefix fallback applies: the URL must start with a
   * family's {@code canonicalBase + "/"} and the (normalized) version must be declared by that
   * family.
   */
  public Optional<ResolvedVersion> findProfileVersion(
      @NonNull String profileUrl, @Nullable String version) {
    Objects.requireNonNull(profileUrl, INVALID_PROFILE_URL);
    final ResolvedVersion exact =
        resolvedVersionsByCanonical.get(canonicalKey(profileUrl, normalizeVersion(version)));
    if (exact != null) {
      return Optional.of(exact);
    }
    return resolveByFamilyPrefix(profileUrl, version);
  }

  /**
   * Returns the dependencies of the open-ended (currently active) period, used when no reference
   * date can be determined. Empty if every period of the version is closed — by design, a version
   * that is only valid within known time windows cannot be resolved without a date.
   */
  public List<String> getFallbackPackages(@NonNull String profileUrl, @Nullable String version) {
    return findProfileVersion(profileUrl, version)
        .flatMap(ResolvedVersion::fallbackPeriod)
        .map(ResolvedPeriod::packages)
        .orElse(List.of());
  }

  /**
   * Returns the dependencies of the first validity period containing the reference date, or an
   * empty set if the profile version is unknown or no period matches.
   */
  public List<String> getPackagesForDate(
      @NonNull String profileUrl, @Nullable String version, @NonNull LocalDate referenceDate) {

    return findProfileVersion(profileUrl, version)
        .flatMap(resolved -> resolved.periodFor(referenceDate))
        .map(ResolvedPeriod::packages)
        .orElse(List.of());
  }

  /** Aggregated message transformations of all package groups bound to the profile version. */
  public List<MessageTransformation> getMessageTransformations(
      @NonNull String profileUrl, @Nullable String version) {
    return findProfileVersion(profileUrl, version)
        .map(ResolvedVersion::messageTransformations)
        .orElse(List.of());
  }

  /** The FHIRPath expression used to extract the reference date for the profile version. */
  public Optional<String> getValidityDateSource(
      @NonNull String profileUrl, @Nullable String version) {
    return findProfileVersion(profileUrl, version).map(ResolvedVersion::validityDateSource);
  }

  // ---------------------------------------------
  // Prefix fallback
  // ---------------------------------------------

  /**
   * Returns the family whose canonical base prefix ({@code canonicalBase + "/"}) the profile URL
   * starts with, if any. Nested canonical bases are not supported, so at most one family can match.
   */
  private Optional<ProfileFamily> matchingFamily(@NonNull String profileUrl) {
    for (var family : familiesByPrefix) {
      if (profileUrl.startsWith(family.prefix())) {
        if (Objects.nonNull(family.profileFamily().profiles())) {
          for (var profileEntry : family.profileFamily().profiles().keySet()) {
            if (profileUrl.contains(profileEntry)) {
              return Optional.of(family.profileFamily());
            }
          }
        }
        return Optional.of(family.profileFamily());
      }
    }
    return Optional.empty();
  }

  /**
   * Resolves an undeclared profile via the canonical-base prefix fallback. The version is
   * normalized as usual (null/blank → the family's {@code defaultVersion}); resolution fails if the
   * family declares no default version or the requested version is unknown. Undeclared profiles
   * carry no validity date source.
   */
  private Optional<ResolvedVersion> resolveByFamilyPrefix(
      @NonNull String profileUrl, @Nullable String version) {
    return matchingFamily(profileUrl)
        .flatMap(
            family -> {
              final String requested = normalizeVersion(version);
              final String effective =
                  ProfileCanonical.DEFAULT_VERSION.equals(requested)
                      ? family.defaultVersion()
                      : requested;
              if (effective == null) {
                return Optional.empty();
              }
              final ProfileVersion profileVersion =
                  Objects.requireNonNull(family.versions()).get(effective);
              if (profileVersion == null) {
                return Optional.empty();
              }
              return Optional.of(
                  resolveVersion(
                      validationModule.configuration(),
                      profileVersion,
                      new ProfileDefinition(null, null),
                      effective));
            });
  }

  // ---------------------------------------------
  // Index construction (load time, fail fast)
  // ---------------------------------------------

  private static String canonicalKey(String profileUrl, String version) {
    return profileUrl + "|" + version;
  }

  private static String normalizeVersion(@Nullable String version) {
    return version == null || version.isBlank() ? ProfileCanonical.DEFAULT_VERSION : version;
  }

  private static String urlPrefix(String canonicalBase) {
    return canonicalBase.endsWith("/") ? canonicalBase : canonicalBase + "/";
  }

  private static Set<String> collectProfileUrls(@NonNull ValidationModuleManifest configuration) {
    final Set<String> urls = new LinkedHashSet<>();
    for (ProfileFamily family : configuration.profileFamilies().values()) {
      final String prefix = urlPrefix(family.canonicalBase());
      Objects.requireNonNull(family.profiles())
          .keySet()
          .forEach(profileName -> urls.add(prefix + profileName));
    }
    return Collections.unmodifiableSet(urls);
  }

  // There could be more Profile Families under the same profile prefix (e.g. eRezept)
  private static List<ProfileFamilyByCanonical> collectFamiliesByPrefix(
      @NonNull ValidationModuleManifest configuration) {
    final List<ProfileFamilyByCanonical> byPrefix = new ArrayList<>();
    configuration
        .profileFamilies()
        .forEach(
            (familyName, family) -> {
              final String prefix = urlPrefix(family.canonicalBase());
              byPrefix.add(new ProfileFamilyByCanonical(prefix, family));
            });
    return byPrefix;
  }

  private static Map<String, ResolvedVersion> buildIndex(
      @NonNull ValidationModuleManifest configuration) {
    final Map<String, ResolvedVersion> index = new HashMap<>();

    configuration
        .profileFamilies()
        .forEach(
            (familyName, family) -> {
              final String prefix = urlPrefix(family.canonicalBase());
              Objects.requireNonNull(family.profiles())
                  .forEach(
                      (profileName, profileDefinition) ->
                          registerProfile(
                              index,
                              configuration,
                              familyName,
                              family,
                              prefix + profileName,
                              profileDefinition));
            });
    return Collections.unmodifiableMap(index);
  }

  private static void registerProfile(
      Map<String, ResolvedVersion> index,
      ValidationModuleManifest configuration,
      String familyName,
      ProfileFamily family,
      String profileUrl,
      ProfileDefinition profileDefinition) {

    Objects.requireNonNull(family.versions())
        .forEach(
            (version, profileVersion) -> {
              final String canonical = canonicalKey(profileUrl, version);
              final ResolvedVersion resolved =
                  resolveVersion(configuration, profileVersion, profileDefinition, version);
              if (index.putIfAbsent(canonical, resolved) != null) {
                throw new IllegalStateException(
                    "Duplicate profile canonical '%s' introduced by profile family '%s'"
                        .formatted(canonical, familyName));
              }
            });

    // Alias for versionless canonicals: url|DEFAULT_VERSION -> declared defaultVersion.
    final String defaultVersion = family.defaultVersion();
    if (Objects.nonNull(defaultVersion)
        && !family.versions().containsKey(ProfileCanonical.DEFAULT_VERSION)) {
      final ProfileVersion defaultProfileVersion = family.versions().get(defaultVersion);
      if (Objects.isNull(defaultProfileVersion)) {
        throw new IllegalStateException(
            "Default version '%s' of profile family '%s' is not declared among its versions %s"
                .formatted(defaultVersion, familyName, family.versions().keySet()));
      }
      index.putIfAbsent(
          canonicalKey(profileUrl, ProfileCanonical.DEFAULT_VERSION),
          resolveVersion(configuration, defaultProfileVersion, profileDefinition, defaultVersion));
    }
  }

  private static ResolvedVersion resolveVersion(
      ValidationModuleManifest configuration,
      ProfileVersion profileVersion,
      ProfileDefinition profileDefinition,
      String version) {

    final List<ResolvedPeriod> periods =
        profileVersion.groups().stream()
            .map(
                period ->
                    new ResolvedPeriod(
                        period.packageGroupName(),
                        period.validFrom(),
                        period.validTill(),
                        packagesOf(configuration, period.packageGroupName())))
            .toList();

    final ResolvedPeriod openEnded =
        periods.stream().filter(ResolvedPeriod::isOpenEnded).findFirst().orElse(null);

    return new ResolvedVersion(
        periods,
        openEnded,
        aggregateMessageTransformations(configuration, profileVersion),
        profileDefinition.dateSourceFor(version).orElse(null));
  }

  private static List<String> packagesOf(
      @NonNull ValidationModuleManifest configuration, @NonNull String groupName) {
    // Reference integrity is guaranteed by the ValidationModuleConfig constructor.
    if (configuration.packageGroups().isEmpty()) {
      return List.of();
    }
    final PackageGroup group = configuration.packageGroups().get(groupName);
    return group.packages();
  }

  /**
   * Collects the distinct message-transformation templates referenced by all package groups of the
   * version, preserving declaration order.
   */
  private static List<MessageTransformation> aggregateMessageTransformations(
      ValidationModuleManifest configuration, ProfileVersion profileVersion) {
    final Set<String> transformationNames = new LinkedHashSet<>();
    profileVersion
        .groups()
        .forEach(
            period -> {
              if (!configuration.packageGroups().isEmpty()) {
                transformationNames.addAll(
                    Objects.requireNonNull(
                        configuration
                            .packageGroups()
                            .get(period.packageGroupName())
                            .messageTransformations()));
              }
            });

    if (transformationNames.isEmpty()) {
      return List.of();
    }

    final List<MessageTransformation> transformations = new ArrayList<>();
    transformationNames.forEach(
        name ->
            transformations.addAll(
                Objects.requireNonNull(configuration.messageTransformations())
                    .getOrDefault(name, List.of())));
    return Collections.unmodifiableList(transformations);
  }
}
