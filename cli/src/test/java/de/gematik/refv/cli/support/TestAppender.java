/*-
 * #%L
 * referencevalidator-cli
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
package de.gematik.refv.cli.support;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class TestAppender extends AbstractAppender {
  private final List<LogEvent> log = new CopyOnWriteArrayList<>();

  public TestAppender() {
    super(
        "TestAppender-" + UUID.randomUUID(),
        null,
        PatternLayout.createDefaultLayout(),
        false,
        Property.EMPTY_ARRAY);
  }

  @Override
  public void append(final LogEvent loggingEvent) {
    log.add(loggingEvent.toImmutable());
  }

  public int size() {
    return log.size();
  }

  public List<LogEvent> getLogsFrom(int startIndex) {
    int safeStart = Math.max(0, startIndex);
    if (safeStart >= log.size()) {
      return new ArrayList<>();
    }
    return new ArrayList<>(log.subList(safeStart, log.size()));
  }

  public List<LogEvent> getLogs() {
    return new ArrayList<>(log);
  }
}
