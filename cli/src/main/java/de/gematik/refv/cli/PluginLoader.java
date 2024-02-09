/*
 * Copyright (c) 2024 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.refv.cli;

import de.gematik.refv.Plugin;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

@NoArgsConstructor
@Slf4j
public class PluginLoader {
    private final Map<String, Plugin> plugins = new HashMap<>();

    public Map<String, Plugin> loadPlugins(String pluginPath) throws IOException, IllegalArgumentException {
        plugins.clear();

        final File pluginFolder = new File(pluginPath);

        if (!pluginFolder.exists()) {
            throw new IllegalArgumentException(String.format("Plugins directory is missing. No such file or directory: %s", pluginFolder.getPath()));
        }

        var zipFiles = Arrays.stream(Objects.requireNonNull(pluginFolder.list((dir, name) -> name.endsWith(".zip")))).map(fileName ->
                getZipFile(pluginPath, fileName)).collect(Collectors.toList());

        if (zipFiles.isEmpty()) {
            log.info("No plugins found in: {}", pluginFolder.getPath());
            return new HashMap<>();
        }

        for (var zipFile : zipFiles) {
            var configFile = zipFile.stream().filter(e -> e.getName().endsWith("config.yaml")).findFirst();
            if (configFile.isEmpty())
                throw new IllegalArgumentException("No config file found for plugin " + zipFile.getName());

            Plugin plugin = Plugin.createFromZipFile(zipFile);
            if(plugins.containsKey(plugin.getId())) {
                log.warn("Duplicate plugin id found: '{}'. Change the id of the plugin in the plugin configuration file and try again", plugin.getId());
            }
            else
                plugins.put(plugin.getId(), plugin);
        }
        return plugins;
    }

    @SneakyThrows
    private static ZipFile getZipFile(String pluginPath, String fileName) {
        return new ZipFile(pluginPath + File.separator + fileName);
    }
}
