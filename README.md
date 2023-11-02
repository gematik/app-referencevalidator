![gematik GmbH](docs/img/Gematik_Logo_Flag.png)

# Gematik Referenzvalidator

![GitHub Latest Release)](https://img.shields.io/github/v/release/gematik/app-referencevalidator?label=release&logo=github) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.gematik.refv/referencevalidator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.gematik.refv/referencevalidator) [![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)


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
> Die Verbindlichkeit des E-Rezept-Moduls vom Referenzvalidator in Abrechnungsprozessen wird in der [Technische Anlage 7, Anhang 2, zur Arzneimittelabrechnungsvereinbarung gemäß § 300 Absatz 3 SGB V](https://www.gkv-datenaustausch.de/leistungserbringer/apotheken/apotheken.jsp) festgelegt. Siehe auch [E-Rezept: Technische Verarbeitbarkeit, Schiedsrichter-Rolle und Problemlösungsverfahren](docs/concept/concept.md#e-rezept-technische-verarbeitbarkeit-schiedsrichter-rolle-und-probleml%C3%B6sungsverfahren).
> Die Verbindlichkeit anderer Validierungsmodule wurde bisher nicht festgelegt. Es besteht lediglich eine Nutzungsempfehlung seitens der gematik.

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
- Prüfung der Gültigkeitszeiträume der in den Instanzen referenzierten Profile
- Nutzung der FHIR-Package-Abhängigkeiten in Abhängigkeit von dem Instanz-Erstellungszeitpunkt (bspw. der datumsabhängigen [KBV-Schlüsseltabellen](https://applications.kbv.de/overview.xhtml))

### Unterstützte Validierungsmodule

| **Modul**                                                       | **Version** |
|-----------------------------------------------------------------|-------------|
| E-Rezept                                                        | 2.0         |
| Elektronische Arbeitsunfähigkeitsbescheinigung                  | 0.9         |
| FHIR Core                                                       | 1.0         |
| Informationstechnische Systeme in Krankenhäusern (ISIK) Stufe 1 | 1.0         |
| Informationstechnische Systeme in Krankenhäusern (ISIK) Stufe 2 | 1.0         |
| Informationstechnische Systeme in Krankenhäusern (ISIK) Stufe 3 | 0.1         |
| Informationstechnische Systeme in der Pflege (ISIP) Stufe 1     | 1.0         |
| DiGA Toolkit                                                    | 0.9         |


### E-Rezept-Modul

Abweichend vom allgemeinen Prüfumfang verhält sich das E-Rezept-Modul wie folgt:
- Codes aus den CodeSystemen `http://fhir.de/CodeSystem/ifa/pzn` und `http://fhir.de/CodeSystem/ask` werden nicht validiert
- Fehler, die bei Validierung von `http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-AbgabedatenBundle|1.0.3` im Zusammenhang mit falschen Angaben bei `http://fhir.abda.de/Identifier/DAV-Herstellerschluessel` stehen, werden ignoriert und führen zum **validen** Ergebnis
- Instanzen mit unbekannten Profilen führen zum invaliden Ergebnis
- Instanzen mit unbekannten Extensions führen zum invaliden Ergebnis

#### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

#### Anpassungen der Profile
- de.gematik.erezept-workflow.r4-1.0.3-1.tgz
  - ErxChargeItem.json
    - BugFix: Korrektur der supportiveInformation-Slices (Keine Snapshot-Generierung sonst möglich)
- de.gematik.erezept-workflow.r4-1.1.1.tgz
  - ErxCommunicationReply.json
    - Communication.about targetProfile: typos: KBV_PR_ERP_Medikament_Freitext, KBV_PR_ERP_Medikament_PZN, KBV_PR_ERP_Medikament_Rezeptur, erxTask. Korrigiert in KBV_PR_ERP_Medication_FreeText, KBV_PR_ERP_Medication_PZN, KBV_PR_ERP_Medication_Compounding, ErxTask
- de.abda.erezeptabgabedatenbasis-1.1.0.tgz
  - Extension-DAV-EX-ERP-Rezeptaenderung.json
  - Extension-DAV-EX-ERP-Zusatzattribute.json
  - Extension-DAV-EX-ERP-ZusatzdatenHerstellung.json
    - Änderungen siehe Version 0.9.6 im [ChangeLog.md des ABDA Referenzvalidators](https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/main/CHANGELOG.md)

### EAU-Modul

Abweichend vom allgemeinen Prüfumfang verhält sich das eAU-Modul wie folgt:
- ICD-10-Codes (CodeSysteme `http://fhir.de/CodeSystem/dimdi/icd-10-gm` und `http://fhir.de/CodeSystem/bfarm/icd-10-gm`) werden nicht validiert
- Instanzen mit unbekannten Profilen führen zum invaliden Ergebnis
- Instanzen mit unbekannten Extensions führen zum invaliden Ergebnis 

#### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

#### Anpassungen der Profile
- de.basisprofil.r4-0.9.13.tgz
  - Extension-seitenlokalisation.json
    - BugFix: Version 0.9.12 auf 0.9.13 korrigiert

### ISIP1-Modul

#### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

### ISIK3-Modul

Abweichend vom allgemeinen Prüfumfang verhält sich das ISIK3-Modul wie folgt:
- Codes aus den CodeSystemen `http://snomed.info/sct`, `http://fhir.de/CodeSystem/bfarm/icd-10-gm`, `http://fhir.de/CodeSystem/bfarm/atc` und `http://fhir.de/CodeSystem/bfarm/ops` werden nicht validiert
- Folgende ValueSets werden nicht validiert: `https://gematik.de/fhir/isik/v3/Basismodul/ValueSet/ProzedurenCodesSCT`, `https://gematik.de/fhir/isik/v3/Basismodul/ValueSet/DiagnosesSCT`, `https://gematik.de/fhir/isik/v3/Basismodul/ValueSet/ProzedurenKategorieSCT`, `https://gematik.de/fhir/isik/v3/Terminplanung/ValueSet/ISiKTerminPriority`, `https://gematik.de/fhir/isik/v3/Medikation/ValueSet/SctRouteOfAdministration` und `http://fhir.de/ValueSet/bfarm/ops`


#### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

### ISIK2-Modul

Abweichend vom allgemeinen Prüfumfang verhält sich das ISIK2-Modul wie folgt:
- Codes aus den CodeSystemen `http://snomed.info/sct`, `http://fhir.de/CodeSystem/bfarm/icd-10-gm`, `http://fhir.de/CodeSystem/bfarm/atc` und `http://fhir.de/CodeSystem/bfarm/ops` werden nicht validiert
- Folgende ValueSets werden nicht validiert: `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/ProzedurenCodesSCT`, `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/DiagnosesSCT`, `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/ProzedurenKategorieSCT`, `https://gematik.de/fhir/isik/v2/Terminplanung/ValueSet/ISiKTerminPriority`, `https://gematik.de/fhir/isik/v2/Medikation/ValueSet/SctRouteOfAdministration` und `http://fhir.de/ValueSet/bfarm/ops`

#### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

### ISIK1-Modul

Abweichend vom allgemeinen Prüfumfang verhält sich das ISIK1-Modul wie folgt:
- Codes aus den CodeSystemen `http://snomed.info/sct`, `http://fhir.de/CodeSystem/bfarm/icd-10-gm` und `http://fhir.de/CodeSystem/bfarm/ops` werden nicht validiert
- Folgende ValueSets werden nicht validiert: `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/ProzedurenCodesSCT`, `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/DiagnosesSCT`, `https://gematik.de/fhir/isik/v2/Basismodul/ValueSet/ProzedurenKategorieSCT` und `http://fhir.de/ValueSet/bfarm/ops`

#### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

### DIGA-Modul

Abweichend vom allgemeinen Prüfumfang verhält sich das DIGA-Modul wie folgt:
- Codes aus den CodeSystemen `http://fhir.de/CodeSystem/ifa/pzn` und `http://fhir.de/CodeSystem/dimdi/atc` werden nicht validiert
- Instanzen mit unbekannten Profilen führen zum invaliden Ergebnis
- Instanzen mit unbekannten Extensions führen zum invaliden Ergebnis

#### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

### CORE-Modul

Der Referenzvalidator bietet die Möglichkeit, FHIR Core-Ressourcen zu validieren. Beim Aufruf des core-Validierungsmoduls ist die Angabe einer Profil-Canonical-URL zur Validierung erforderlich (siehe [Optionen der Konsolenanwendung](#konsolenanwendung-1)).

## Erste Schritte

### Voraussetzungen

Der Referenzvalidator wird als Java-Bibliothek und als Konsolenanwendung verteilt. Für die Verwendung ist JDK 11 erforderlich (z.B. [AdoptOpenJDK](https://adoptopenjdk.net/)).  

### Installation

#### Konsolenanwendung

Für die Verwendung der Konsolenanwendung soll die Datei `referencevalidator-cli-X.Y.Z.jar` in einem beliebigen Ordner im Dateisystem abgelegt werden (siehe [Releases](https://github.com/gematik/app-referencevalidator/releases)).  

#### Java-Bibliothek

Der Referenzvalidator wird zur Einbindung in andere Projekte auf [Maven Central](https://search.maven.org/artifact/de.gematik.refv/referencevalidator) veröffentlicht. Bei der Einbindung ist darauf zu achten, dass genau die angegebenen Versionen der Abhängigkeiten, insbesondere der HAPI-Bibliothekten (`ca.uhn.hapi.fhir`), zur Laufzeit eingebunden sind.

Beispiel zur Einbindung des Referenzvalidator:

``` XML
    <dependency>
          <groupId>de.gematik.refv</groupId>
          <artifactId>referencevalidator-lib</artifactId>
          <version>${version.referencevalidator}</version>
    </dependency>
```    

Die Versionsangabe `${version.referencevalidator}` soll mit der gewünschten einzubindenden Referenzvalidator-Version ersetzt werden. 

## Verwendung

### Konsolenanwendung

Der Referenzvalidator erfordert als Eingabe einen Modulnamen und einen gültigen Pfad zur Datei, die eine FHIR-Ressource enthält:

    java -jar referencevalidator-cli-X.Y.Z.jar erp c:\temp\example.xml

Unterstützte Modulnamen:
- `erp` (E-Rezept)
- `eau` (Elektronische Arbeitsunfähigkeitsbescheinigung)
- `isip1` (Informationstechnische Systeme in der Pflege Stufe 1)
- `isik3` (Informationstechnische Systeme in Krankenhäusern Stufe 3)
- `isik2` (Informationstechnische Systeme in Krankenhäusern Stufe 2)
- `isik1` (Informationstechnische Systeme in Krankenhäusern Stufe 1)
- `diga` (DiGA Toolkit)
- `core` (FHIR Core)

Optionen:
- `--verbose` - Verbode-Modus mit Debug-Protokoll-Ausgaben und INFORMATION- bzw. WARNING-Validierungsnachrichten 
- `--no-profile-validity-period-check` - Deaktivierung der Zeitraumgültigkeitsprüfung der Profilversionen
- `--profile` - Angabe einer Profil-Canonical-URL zur Validierung. Falls angegeben, wird die Angabe der Instanz unter meta.profile ignoriert
- `--module-info` - Ausgabe der unterstützten Profile, Profilversionen und FHIR-Packages zu einem Validierungsmodul
- `--accepted-encodings` - Komma-separierte Liste mit den zu akzeptierenden Serialisierungsformaten der FHIR-Ressourcen. Unterstützte Werte: `xml`,`json`. Überschreibt die Modul-eigene Konfiguration.

### Java-Bibliothek

Folgende Beispiele veranschaulichen die Verwendung vom Referenzvalidator in einer Java-Anwendung.

Validierung einer FHIR-Ressource aus einer Datei:

``` Java
        ValidationModule erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERP);
        ValidationResult result = erpModule.validateFile("c:/temp/KBV_PR_ERP_Bundle.xml");
        System.out.println(result.isValid());
        System.out.println(result.getValidationMessages());
``` 

Validierung einer FHIR-Ressource als String:

``` Java
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
``` 

Die Validierungseinstellungen auch angepasst werden: 

``` Java
        ValidationModule erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERP);
        ValidationOptions options = ValidationOptions.getDefaults();
        options.setProfileValidityPeriodCheckStrategy(ProfileValidityPeriodCheckStrategy.IGNORE);
        options.setProfiles(List.of("https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle|1.0.1"));
        options.setAcceptedEncodings(List.of("xml", "json"));
        options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
        ValidationResult result = erpModule.validateFile("c:/temp/KBV_PR_ERP_Bundle.xml", options);
        System.out.println(result.isValid());
        System.out.println(result.getValidationMessages());
``` 
Die Anpassung der Validierungseinstellungen soll allerdings nur für Testzwecke erfolgen, da damit die Bewertung der eingegebenen Instanz gegenüber Standardeinstellungen verfälscht wird.  

## Lizenz

Siehe [Apache License, Version 2.0](LICENSE)

## Beiträge zum Projekt und Danksagung

Teile des Projekts basieren auf dem [ABDA E-Rezept-Referenzvalidator (Copyright 2022 Deutscher Apothekerverband (DAV))](https://github.com/DAV-ABDA/eRezept-Referenzvalidator/), der unter [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) steht. Modifizierte Quellcodedateien sind im Quellcode als solche gekennzeichnet.

Die Snapshots für die FHIR-Packages wurden auf Basis vom [Pull Request](https://github.com/gematik/app-referencevalidator/pull/1) von [spectrumK](https://www.spectrumk.de/) und [bakdata](https://bakdata.com/) vorgeneriert und in den Referenzvalidator eingebunden.

## Kontakt

Fragen, Anregungen, Bug Reports und Feature requests sind willkommen und können gerne über die [GitHub Issues](https://github.com/gematik/app-referencevalidator/issues) oder über [referenzvalidator&commat;gematik.de](mailto:referenzvalidator&commat;gematik.de) eingereicht werden.
