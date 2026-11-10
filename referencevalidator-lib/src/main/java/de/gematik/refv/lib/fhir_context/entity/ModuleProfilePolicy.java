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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Module profile support and unknown-profile behavior projected into validation policy. */
public record ModuleProfilePolicy(
    boolean errorOnUnknownProfile, @NonNull List<ProfileFamilySupport> profileFamilies) {

  public ModuleProfilePolicy {
    profileFamilies = List.copyOf(Objects.requireNonNull(profileFamilies));
  }

  public static @NonNull ModuleProfilePolicy fromManifest(
      @NonNull ValidationModuleManifest manifest) {
    Objects.requireNonNull(manifest);
    return new ModuleProfilePolicy(
        manifest.errorOnUnknownProfile(),
        manifest.profileFamilies().values().stream().map(ModuleProfilePolicy::supportOf).toList());
  }

  public boolean supports(@NonNull ProfileCanonical profile) {
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
        return family.supportsVersion(profile.version());
      }
    }
    return prefixFallback != null && prefixFallback.supportsVersion(profile.version());
  }

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

    private String urlPrefix() {
      return canonicalBase.endsWith("/") ? canonicalBase : canonicalBase + "/";
    }

    private boolean supportsVersion(@Nullable String profileVersion) {
      var effectiveVersion =
          profileVersion == null || profileVersion.isBlank() ? defaultVersion : profileVersion;
      return effectiveVersion != null && versions.contains(effectiveVersion);
    }
  }
}
