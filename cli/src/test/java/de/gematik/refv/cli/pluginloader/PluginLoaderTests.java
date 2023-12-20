package de.gematik.refv.cli.pluginloader;

import de.gematik.refv.cli.PluginLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginLoaderTests {

    private static final String PLUGIN_PATH = "src/test/resources/pluginloader-integration-test/plugins";

    @Test
    void testLoadPlugins() throws IOException {
        PluginLoader pluginLoader = new PluginLoader();

        var plugins = pluginLoader.loadPlugins(PLUGIN_PATH);

        assertFalse(plugins.isEmpty());
        assertTrue(plugins.containsKey("minimal"));
        assertEquals(1, plugins.keySet().size(),"More plugins were loaded than expected");
    }
}
