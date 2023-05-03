![gematik GmbH](docs/img/Gematik_Logo_Flag.png)

# Gematik Referenzvalidator

<details>
  <summary>Inhaltsverzeichnis</summary>
  <ol>
    <li>
      <a href="#über-das-projekt">Über das Projekt</a>
       <ul>
        <li><a href="#releasenotes">Release Notes</a></li>
      </ul>     
    </li>
    <li>
      <a href="#funktionsumfang">Funktionsumfang</a>
      <ul>
        <li><a href="#e-rezept-modul">E-Rezept-Modul</a></li>
      </ul>
    </li>
    <li>
      <a href="#erste-schritte">Erste Schritte</a>
      <ul>
        <li><a href="#voraussetzungen">Voraussetzungen</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#verwendung">Verwendung</a></li>
    <li><a href="#lizenz">Lizenz</a></li>
    <li><a href="#kontakt">Kontakt</a></li>
  </ol>
</details>

## Über das Projekt

Der Referenzvalidator ermöglicht eine erweiterte Validierung von FHIR-Ressourcen, die in den Anwendungen der Telematikinfrastruktur (TI) verwendet werden. Der Referenzvalidator liefert autoritative Antworten zur Validität von übertragenen Datensätzen und ist somit eine Referenz für eventuell sonst im Rahmen einer TI-Anwendung eingesetzte FHIR-Validatoren. 

Siehe [Use Cases, Anforderungen, Architektur, Entwicklungsprozess](docs/concept/concept.md) für weitere Informationen.

> **Warning**
> Die Verbindlichkeit vom Referenzvalidator wurde für die Telematikinfrastruktur und ihre Anwendungen noch nicht festgelegt. Für die Validierung der Daten in Abrechnungsprozessen der E-Rezept-Anwendung soll [ABDA Referenzvalidator](https://github.com/DAV-ABDA/eRezept-Referenzvalidator/) eingesetzt werden

### Release Notes

Siehe [Release Notes](ReleaseNotes.md)

## Funktionsumfang

- Validierung von FHIR-Ressourcen anhand der referenzierten [Profile](#unterstützte-profile-und-versionen)
- Der Prüfumfang entspricht dem Umfang des [HL7 Java Validators](https://www.hl7.org/fhir/validation.html):
  - Struktur: Alle Elemente einer Instanz MÜSSEN in dem referenzierten Profil definiert sein
  - Kardinalität: Die Min/Max-Angaben aller Eigenschaften sind berücksichtigt
  - Wertebereiche: Die Wertebereiche von Eigenschaften werden berücksichtigt (einschließlich aufgelistete Codes)
  - Coding/CodeableConcept bindings: Die Code-Angaben in einer Instanz entsprechen der Definition des Kodierungssystems aus dem Profil
  - Constraints/Invariants: Die für die Eigenschaften im Profil definierten Regeln sind eingehalten

Ergänzungen zum Standardverhalten des HL7 Java Validators:
- Instanzen mit unbekannten Profilen führen zum invaliden Ergebnis
- Instanzen mit unbekannten Extensions führen zum invaliden Ergebnis
- Nicht auflösbare relative Referenzen in Bundles führen zum invaliden Ergebnis 

### E-Rezept-Modul

Abweichend vom allgemeinen Prüfumfang verhält sich das E-Rezept-Modul wie folgt:
- Codes aus den CodeSystemen `http://fhir.de/CodeSystem/ifa/pzn` und `http://fhir.de/CodeSystem/ask` werden nicht validiert
- Fehler, die bei Validierung von `http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-AbgabedatenBundle|1.0.3` im Zusammenhang mit falschen Angaben bei `http://fhir.abda.de/Identifier/DAV-Herstellerschluessel` stehen, werden ignoriert und führen zum **validen** Ergebnis

Die eingebundenen Packages, unterstützte Profile und Versionen findet man [hier](supported-profiles.md).

### EAU-Modul

Abweichend vom allgemeinen Prüfumfang verhält sich das eAU-Modul wie folgt:
- ICD-10-Codes (CodeSysteme `http://fhir.de/CodeSystem/dimdi/icd-10-gm` und `http://fhir.de/CodeSystem/bfarm/icd-10-gm`) werden nicht validiert

Die eingebundenen Packages, unterstützte Profile und Versionen findet man [hier](supported-profiles.md).

### ISIP1-Modul

Die eingebundenen Packages, unterstützte Profile und Versionen findet man [hier](supported-profiles.md).

### ISIK2-Modul

Abweichend vom allgemeinen Prüfumfang verhält sich das ISIK2-Modul wie folgt:
- Codes aus den CodeSystemen `http://snomed.info/sct`, `http://fhir.de/CodeSystem/bfarm/icd-10-gm`, `http://fhir.de/CodeSystem/bfarm/atc` und `http://fhir.de/CodeSystem/bfarm/ops` werden nicht validiert
- Folgende ValueSets werden nicht validiert: `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/ProzedurenCodesSCT`, `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/DiagnosesSCT`, `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/ProzedurenKategorieSCT`, `https://gematik.de/fhir/isik/v2/Terminplanung/ValueSet/ISiKTerminPriority`, `https://gematik.de/fhir/isik/v2/Medikation/ValueSet/SctRouteOfAdministration` und `http://fhir.de/ValueSet/bfarm/ops`

### ISIK1-Modul

Abweichend vom allgemeinen Prüfumfang verhält sich das ISIK1-Modul wie folgt:
- Codes aus den CodeSystemen `http://snomed.info/sct`, `http://fhir.de/CodeSystem/bfarm/icd-10-gm` und `http://fhir.de/CodeSystem/bfarm/ops` werden nicht validiert
- Folgende ValueSets werden nicht validiert: `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/ProzedurenCodesSCT`, `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/DiagnosesSCT`, `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/ProzedurenKategorieSCT` und `http://fhir.de/ValueSet/bfarm/ops`

Die eingebundenen Packages, unterstützte Profile und Versionen findet man [hier](supported-profiles.md).

## Erste Schritte

### Voraussetzungen

Der Referenzvalidator wird als Java-Bibliothek und als Konsolenanwendung verteilt. Für die Verwendung ist JDK 11 erforderlich (z.B. [AdoptOpenJDK](https://adoptopenjdk.net/)).  

### Installation

#### Konsolenanwendung

Für die Verwendung der Konsolenanwendung soll die Datei `referencevalidator-cli-X.Y.Z.jar` in einem beliebigen Ordner im Dateisystem abgelegt werden.

#### Java-Bibliothek

Der Referenzvalidator wird zur Einbindung in andere Projekte auf [Maven Central](https://search.maven.org/artifact/de.gematik.refv/referencevalidator) veröffentlicht. Bei der Einbindung ist darauf zu achten, dass genau die angegebenen Versionen der Abhängigkeiten, insbesondere der HAPI-Bibliothekten (`ca.uhn.hapi.fhir`), zur Laufzeit eingebunden sind.

Beispiel zur Einbindung des Referenzvalidator:

    <dependency>
          <groupId>de.gematik.refv</groupId>
          <artifactId>referencevalidator-lib</artifactId>
          <version>${version.referencevalidator}</version>
    </dependency>

Die Versionsangabe `${version.referencevalidator}` soll mit der gewünschten einzubindenden Referenzvalidator-Version ersetzt werden. 

## Verwendung

### Konsolenanwendung

Der Referenzvalidator erfordert als Eingabe einen Modulnamen und einen gültigen Pfad zur Datei, die eine FHIR-Ressource enthält:

    java -jar referencevalidator-cli-X.Y.Z.jar -m erp -i c:\temp\example.xml

Unterstützte Modulnamen:
- `erp` (E-Rezept)
- `eau` (Elektronische Arbeitsunfähigkeitsbescheinigung)
- `isip1` (Informationstechnische Systeme in der Pflege Stufe 1)
- `isik2` (Informationstechnische Systeme in Krankenhäusern Stufe 2)
- `isik1` (Informationstechnische Systeme in Krankenhäusern Stufe 1)

### Java-Bibliothek

Folgende Beispiele veranschaulichen die Verwendung vom Referenzvalidator in einer Java-Anwendung.

Validierung einer FHIR-Ressource aus einer Datei:

    ValidationModule erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERP);
    Path path = Paths.get("c:/temp/KBV_PR_ERP_Bundle.xml");
    ValidationResult result = erpModule.validateFile(path);
    System.out.println(result.isValid());
    System.out.println(result.getValidationMessages());

Validierung einer FHIR-Ressource als String:

    ValidationModule erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERP);
    String fhirRessource = "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
        + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
        + "    <meta>\n"
        + "        <profile value=\"https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle|1.0.1\"/>\n"
        + "        \n"
        + "    </meta>\n"
        + "</Bundle>";
    ValidationResult result = erpModule.validateString(fhirRessource);
    System.out.println(result.isValid());
    System.out.println(result.getValidationMessages());

## Lizenz

Siehe [Apache License, Version 2.0](LICENSE)

## Beiträge zum Projekt und Danksagung

Teile des Projekts basieren auf dem [ABDA E-Rezept-Referenzvalidator (Copyright 2022 Deutscher Apothekerverband (DAV))](https://github.com/DAV-ABDA/eRezept-Referenzvalidator/), der unter [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) steht. Modifizierte Quellcodedateien sind im Quellcode als solche gekennzeichnet.

Die Snapshots für die FHIR-Packages wurden auf Basis vom [Pull Request](https://github.com/gematik/app-referencevalidator/pull/1) von [spectrumK](https://www.spectrumk.de/) und [bakdata](https://bakdata.com/) vorgeneriert und in den Referenzvalidator eingebunden.

## Kontakt

Fragen, Anregungen, Bug Reports und Feature requests sind willkommen und können gerne über die [GitHub Issues](https://github.com/gematik/app-referencevalidator/issues) oder über [referenzvalidator&commat;gematik.de](mailto:referenzvalidator&commat;gematik.de) eingereicht werden.
