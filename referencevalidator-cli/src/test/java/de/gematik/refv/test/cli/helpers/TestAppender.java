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
package de.gematik.refv.test.cli.helpers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "TestAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class TestAppender extends AbstractAppender {
  private static final AtomicLong NEXT_ID = new AtomicLong();
  private final List<LogEvent> logEvents = new CopyOnWriteArrayList<>();

  @PluginFactory
  public static TestAppender createAppender(
      @PluginAttribute(value = "name", defaultString = "TestAppender") final String name) {
    return new TestAppender(name + "-" + NEXT_ID.incrementAndGet());
  }

  private TestAppender(final String name) {
    super(name, null, null, false, Property.EMPTY_ARRAY);
  }

  public List<LogEvent> getLogsFromCurrentThread() {
    final var currentThreadName = Thread.currentThread().getName();
    return logEvents.stream()
        .filter(event -> currentThreadName.equals(event.getThreadName()))
        .toList();
  }

  @Override
  public void append(final LogEvent event) {
    logEvents.add(event.toImmutable());
  }
}
