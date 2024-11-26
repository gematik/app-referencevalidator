/*
Copyright (c) 2022-2024 gematik GmbH

Licensed under the Apache License, Version 2.0 (the License);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package de.gematik.refv.commons.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.validation.ValidationContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.BaseDateTimeType;

@AllArgsConstructor
class ResourceCreationDateLocator {

  private FhirContext fhirContext;

  /**
   * Ermittelt das Erstellungsdatum der Instanz anhand des übergebenen FHIRPath-Ausdrucks
   * entsprechend der Zeitzone Europe/Berlin. Falls im Erstellungszeitpunkt der Instanz keine
   * Zeitzoneninformationen enthalten sind (z.B. bei Verwendung des Formats YYYY-mm-dd oder
   * YYYY-mm-ddThh:mm:ss), wird die Zeitzone Europe/Berlin angenommen. Andernfalls wird die
   * Datumszeitangabe in die Zeitzone Europe/Berlin zuerst konvertiert und die resultierende
   * Datumsangabe als Ergebnis zurückgegeben
   *
   * @param resourceBody FHIR-Ressource, deren Erstellungsdatum ermittelt werden soll
   * @param creationDateLocator FHIRPath-Ausdruck zur Ermittlung des Erstellungsdatums
   * @return Optional.empty() falls mit dem FHIRPath-Ausdruck kein Erstellungsdatum lokalisiert werden konnte und das ermittelte Erstellungsdatum andernfalls
   */
  public Optional<LocalDate> findCreationDateIn(String resourceBody, String creationDateLocator) {
    var context = ValidationContext.forText(fhirContext, resourceBody, null);

    var fhirPathEngine = fhirContext.newFhirPath();
    var creationDateList =
        fhirPathEngine.evaluate(context.getResource(), creationDateLocator, BaseDateTimeType.class);
    if (creationDateList == null || creationDateList.isEmpty()) return Optional.empty();

    if (creationDateList.size() > 1)
      throw new IllegalArgumentException(
          String.format(
              "Multiple values for creationDate found using expression: '%s'",
              creationDateLocator));

    var creationDate = creationDateList.get(0);
    if (creationDate == null) // Date could not be parsed
      throw new DataFormatException(
          String.format("Could not parse date %s", creationDateList.get(0).toString()));

    if (creationDate.getValue() == null)
      throw new DataFormatException(
          "Could not parse creationDate: " + creationDate.asStringValue());

    // Not timezone information supplied --> assume Europe/Berlin
    if(creationDate.getTimeZone() == null)
      return Optional.of(LocalDate.of(creationDate.getYear(), creationDate.getMonth()+1, creationDate.getDay()));

    // TimeZone information supplied
    return Optional.of(
        LocalDate.ofInstant(creationDate.getValue().toInstant(), ZoneId.of("Europe/Berlin")));
  }
}
