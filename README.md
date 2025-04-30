<img align="right" width="250" height="47" src="docs/img/Gematik_Logo_Flag.png"/> <br/>

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
        <li><a href="#unterstützte-validierungsmodule">Unterstützte Validierungsmodule</a></li>
        <li><a href="#e-rezept-modul">E-Rezept-Modul</a></li>
        <li><a href="#eau-modul">EAU-Modul</a></li>
        <li><a href="#core-modul">Code-Modul</a></li>
        <li><a href="#e-rezept-abrechnungsdaten-modul-experimentell">E-Rezept Abrechnungsdaten-Modul (EXPERIMENTELL)</a></li>
        <li><a href="#externe-validierungsmodule-plugins">Externe Validierungsmodule (Plugins)</a></li>
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
    <li><a href="#beiträge-zum-projekt-und-danksagung">Beiträge zum Projekt und Danksagung</a></li>
    <li><a href="#kontakt">Kontakt</a></li>
  </ol>
</details>

## Über das Projekt

Der Referenzvalidator ermöglicht eine erweiterte Validierung von FHIR-Ressourcen, die in den Anwendungen der Telematikinfrastruktur (TI) verwendet werden. Der Referenzvalidator liefert autoritative Antworten zur Validität von übertragenen Datensätzen und ist somit eine Referenz für eventuell sonst im Rahmen einer TI-Anwendung eingesetzte FHIR-Validatoren. 

Siehe [Use Cases, Anforderungen, Architektur, Entwicklungsprozess](docs/concept/concept.md) für weitere Informationen.

> **Warning**
> Die Verbindlichkeit des E-Rezept-Moduls vom Referenzvalidator in Abrechnungsprozessen wird in der [Technische Anlage 7, Anhang 2, zur Arzneimittelabrechnungsvereinbarung gemäß § 300 Absatz 3 SGB V](https://www.gkv-datenaustausch.de/leistungserbringer/apotheken/apotheken.jsp) festgelegt. Siehe auch [E-Rezept: Technische Verarbeitbarkeit, Schiedsrichter-Rolle und Problemlösungsverfahren](docs/concept/concept.md#e-rezept-technische-verarbeitbarkeit-schiedsrichter-rolle-und-probleml%C3%B6sungsverfahren).
> Die Verbindlichkeit anderer Validierungsmodule wurde bisher nicht festgelegt. Es besteht lediglich eine Nutzungsempfehlung seitens der gematik.

> **Warning**
> Der Betrieb des Referenzvalidators in bestehenden Anwendungen oder Anwendungslandschaften obliegt der Verantwortung der Nutzer. Die gematik trifft angemessene Maßnahmen, um die Sicherheit und Performance des Referenzvalidators zu gewährleisten. Sie übernimmt aber keine Verantwortung für etwaige Schäden, die durch die Integration des Referenzvalidators in Produktionssysteme entstehen (siehe auch Haftungsausschuss der [Apache 2.0-Lizenz](https://www.apache.org/licenses/LICENSE-2.0). Insbesondere müssen die Sicherheitsaspekte des Gesamtsystems unter Einbeziehung der technischen und organisatorischen Rahmenbedingungen der jeweiligen Betriebsumgebung durch die Nutzer selbst bewertet werden. Des Weiteren hängen die Performance-Eigenschaften des Validators stark von der jeweiligen Betriebsumgebung ab.

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

| **Modul**                                      | **Version** |
|------------------------------------------------|-------------|
| E-Rezept                                       | 2.10        |
| Elektronische Arbeitsunfähigkeitsbescheinigung | 0.91        |
| FHIR Core                                      | 1.0         |
| E-Rezept Abrechnungsdaten (experimentell)      | 0.3         |



### E-Rezept-Modul

Abweichend vom allgemeinen Prüfumfang verhält sich das E-Rezept-Modul wie folgt:
- Folgende CodeSysteme werden nicht validiert:
  - `http://fhir.de/CodeSystem/ifa/pzn`
  - `http://fhir.de/CodeSystem/ask`
  - `http://fhir.de/CodeSystem/bfarm/atc` (wird in _GEM_ERP_PR_Medication_ verwendet)
  - `http://snomed.info/sct` (wird in _GEM_ERP_PR_Medication_ verwendet)
  - `https://terminologieserver.bfarm.de/fhir/CodeSystem/arzneimittel-referenzdaten-pharmazeutisches-produkt` (wird in _GEM_ERP_PR_Medication_ verwendet)
- Folgende CodeSysteme werden validiert, führt aber zu einer Warnung im Falle der Nicht-Validität:
  - `http://unitsofmeasure.org`
- Folgende ValueSets werden nicht validiert:
  - `http://hl7.org/fhir/uv/ips/ValueSet/medicine-doseform` (wird in _GEM_ERP_PR_Medication_ verwendet)
- Fehler, die bei Validierung von `http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-AbgabedatenBundle|1.0.3` im Zusammenhang mit falschen Angaben bei `http://fhir.abda.de/Identifier/DAV-Herstellerschluessel` stehen, werden ignoriert und führen zum **validen** Ergebnis
- Instanzen mit unbekannten Profilen führen zum invaliden Ergebnis
- Instanzen mit unbekannten Extensions führen zum invaliden Ergebnis
- Alle E-Rezept-Ressourcen (auch verschachtelte) dürfen nur genau eine meta.profile-Angabe enthalten (obwohl manche Profile technisch auch mehrfache meta.profile-Angaben erlauben).  

> **Warning**
> Das E-Rezept-Modul validiert nur FHIR-Instanzen, für die ein Profil im Rahmen der jeweiligen FHIR-Spezifikationen definiert wurde. Manche Ausgaben vom E-Rezept-Fachdienst (z.B. collection-Bundles), für die kein dediziertes Profil definiert wurde, sind nicht Bestandteil des Prüfumfangs. Diese Instanzen sind daher nicht validierbar.

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

Für folgende Profile erfolgen Instanz-datumsbasierte Auswahl der FHIR-Packages zur Validierung sowie Profilgültigkeitsprüfungen:

<table id="creation-date-elements">
<tr>
<th>Profil</th>
<th>Datenelement</th>
<th>Anmerkung</th>
</tr>
<tr>
<td>KBV_PR_EAU_Bundle</td>
<td>Bundle.entry.resource.where(meta.profile.contains('KBV_PR_EAU_Composition')).date</td>
<td>Ausstellungsdatum</td>
</tr>
<tr>
<td>KBV_PR_EAU_Composition</td>
<td>date</td>
<td>Ausstellungsdatum</td>
</tr>
<tr>
<td>KBV_PR_EAU_Storno_Bundle</td>
<td>Bundle.entry.resource.where(meta.profile.contains('KBV_PR_EAU_Composition')).date</td>
<td>Aktuelles Datum der Stornierung</td>
</tr>
<tr>
<td>KBV_PR_EAU_Storno_Composition</td>
<td>date</td>
<td>Aktuelles Datum der Stornierung</td>
</tr>
</table>

#### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

#### Anpassungen der Profile
- de.basisprofil.r4-0.9.13.tgz
  - Extension-seitenlokalisation.json
    - BugFix: Version 0.9.12 auf 0.9.13 korrigiert

### CORE-Modul

Der Referenzvalidator bietet die Möglichkeit, FHIR Core-Ressourcen zu validieren. Beim Aufruf des core-Validierungsmoduls ist die Angabe einer Profil-Canonical-URL zur Validierung erforderlich (siehe [Optionen der Konsolenanwendung](#konsolenanwendung-1)).

### E-Rezept Abrechnungsdaten-Modul _(EXPERIMENTELL)_

Dieses Modul bietet eine experimentelle Umsetzung zur performanten Validierung von großen [TA7-FHIR-Abrechnungsdateien](https://simplifier.net/erezeptabrechnungsdaten). Unterstützt werden nur Resourcen mit dem Profil `https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_Rechnung_Bundle|1.3.0`.

Das Modul mit der id `erpta7` kann auf die gleiche Art und Weise verwendet werden wie das E-Rezept-Modul, z.B.

```
java -jar .\referencevalidator-cli-X.Y.Z.jar erpta7 c:\temp\big-ta7-file.xml
```
oder aus dem Quellcode heraus unter der Verwendung der entsprechenden Kennung `SupportedValidationModule.ERPTA7`:

```Java
ValidationModule erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERPTA7);
```

Für die Validierung der TA7-FHIR-Instanzen teilt das Modul die Eingabe in mehrere `GKVSV_PR_TA7_RezeptBundle`-Ressourcen auf und validiert diese unabhängig voneinander - parallel und unter Verwendung des E.Rezept-Validierungsmoduls. Zusätzlich erfolgt Validierung des umschließenden Bundles, anderer Ressourcen sowie Querverweisprüfungen innerhalb der Eingabe-FHIR-Instanz, um die Gesamtkorrektheit der Validierung zu gewährleisten. Das Modul strebt identische Validierungsergebnisse wie das E-Rezept-Modul an. Die Leistung des Moduls hängt von der Anzahl der verfügbaren CPU-Kerne ab, was der Anzahl der vom Modul gestarteten parallelen Threads entspricht. Wie auch das E-Rezept-Modul validiert das ERPTA7-Modul KEINE  base64-kodierten Daten (eRezepte, Abgabedaten, Belege) innerhalb der TA7-Datei.

### Externe Validierungsmodule (Plugins)

Der Referenzvalidator kann mit weiteren Validierungsmodulen erweitert werden. Die von der gematik bereitgestellten Validierungsmodule 
können aus dem entsprechenden [GitHub-Repository](https://github.com/gematik/app-referencevalidator-plugins/releases) heruntergeladen werden.

#### Nutzung mit der Konsolenanwendung

Die Plugins sollen im Ordner `plugins` auf derselben Ordnerebene wie die `jar`-Referenzvalidator-Datei abgelegt werden.
Beim Aufruf des Referenzvalidators soll die `id` des abgelegten Plugins angegeben werden.

Beispiel der Ordnerstruktur:
```
referencevalidator/
├── plugins/
│   └── isik3-terminplanung.zip
├── referencevalidator-cli-X.Y.Z.jar
└── test-ISIKTermin.json
```

Beispielaufruf:

```
    cd referencevalidator
    java -jar .\referencevalidator-cli-X.Y.Z.jar isik3-terminplanung test-ISIKTermin.json
```

#### Nutzung mit der Java-Bibliothek

``` Java
Plugin plugin = Plugin.createFromZipFile(new ZipFile("src/test/resources/plugins/minimal-plugin.zip"));
var pluginModule = new ValidationModuleFactory().createValidationModuleFromPlugin(plugin);
String fhirRessource = "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
        + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
        + "    <meta>\n"
        + "        <profile value=\"https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle|1.0.1\"/>\n"
        + "        \n"
        + "    </meta>\n"
        + "</Bundle>";
var result = pluginModule.validateString(fhirRessource);
System.out.println(result.isValid());
```

## Erste Schritte

### Voraussetzungen

Der Referenzvalidator wird als Java-Bibliothek und als Konsolenanwendung verteilt. Für die Verwendung ist JDK 11 erforderlich (z.B. [AdoptOpenJDK](https://adoptopenjdk.net/)).

> **Warning**
> Der Referenzvalidator verfolgt das Ziel, die Validierungsergebnisse unabhängig von den lokalen Einstellungen der Zeitzone oder der Zahlendarstellung zu produzieren. Bei den lokalen Spracheinstellungen ist allerdings darauf zu achten, dass Deutsch oder Englisch als Sprache voreingestellt ist (z.B. Locale de_DE oder en_US). Bei anderen Sprachen kann es in Randfällen zur Abweichung bei der Bewertung der Validität der Instanzen kommen.  

### Installation

#### Konsolenanwendung

Für die Verwendung der Konsolenanwendung soll die Datei `referencevalidator-cli-X.Y.Z.jar` in einem beliebigen Ordner im Dateisystem abgelegt werden (siehe [Releases](https://github.com/gematik/app-referencevalidator/releases)).  

#### Java-Bibliothek

Der Referenzvalidator wird zur Einbindung in andere Projekte auf [Maven Central](https://search.maven.org/artifact/de.gematik.refv/referencevalidator) veröffentlicht. 

> **Warning**
> Bei der Einbindung ist darauf zu achten, dass genau die angegebenen Versionen der Abhängigkeiten, insbesondere der HAPI-Bibliothekten (`ca.uhn.hapi.fhir`), zur Laufzeit eingebunden sind. Im Falle der Abweichung können die Validierungsergebnisse anders ausfallen als erwartet.

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

Der Referenzvalidator erfordert als Eingabe einen Modulnamen und einen oder mehrere gültige Pfade zu Dateien oder Verzeichnissen, die FHIR-Ressourcen enthalten (bei mehreren Pfaden müssen diese mit Kommas separiert werden):

    java -jar referencevalidator-cli-X.Y.Z.jar erp c:\temp\example.xml
    java -jar referencevalidator-cli-X.Y.Z.jar erp c:\temp\example.xml,c:\temp\example2.xml,c:\temp\folder-with-fhir-resources

Unterstützte Modulnamen:
- `erp` (E-Rezept)
- `eau` (Elektronische Arbeitsunfähigkeitsbescheinigung)
- `core` (FHIR Core)
- `erpta7` (E-Rezept Abrechnungsdaten)

Optionen:
- `--verbose` - Verbode-Modus mit Debug-Protokoll-Ausgaben und INFORMATION- bzw. WARNING-Validierungsnachrichten 
- `--no-profile-validity-period-check` - Deaktivierung der Zeitraumgültigkeitsprüfung der Profilversionen
- `--profile` - Angabe einer Profil-Canonical-URL zur Validierung. Falls angegeben, wird die Angabe der Instanz unter meta.profile ignoriert
- `--profile-filter` - Angabe eines regulären Ausdrucks zur Prüfung der unter meta.profile referenzierten Profile. Falls kein referenzierstes Profil dem regulären Ausdruck entspricht, wird die Instanz als invalide betrachtet. Beispiele zur Nutzung:
  - Prüfen, dass es sich bei einer Instanz grundsätzlich um eine E-Rezept-Quittung und nicht etwa um eine Verordnung handelt: `--profile-filter "ErxReceipt|GEM_ERP_PR_Bundle"`
  - Prüfen, dass es sich bei einer Instanz um eine E-Rezept-Verordnung (in der Profilversion 1.1) handelt: `--profile-filter "KBV_PR_ERP_Bundle\|1\.1"`
  - Prüfen, dass es sich bei einer Instanz um Abgabedaten (GKV oder PKV) handelt: `--profile-filter "DAV-(PKV-)?PR-ERP-AbgabedatenBundle"`
- `--module-info` - Ausgabe der unterstützten Profile, Profilversionen und FHIR-Packages zu einem Validierungsmodul
- `--accepted-encodings` - Komma-separierte Liste mit den zu akzeptierenden Serialisierungsformaten der FHIR-Ressourcen. Unterstützte Werte: `xml`,`json`. Überschreibt die Modul-eigene Konfiguration.
- `--output` - Angabe eines Dateipfades um das Validierungsergebnis in Form einer FHIR OperationOutcome-Ressource bzw. eines Bundles mit OperationOutcome-Ressourcen in eine Datei zu schreiben. Beispiel: `--output c:\temp\output.xml`. Als Ausgabeformate werden sowohl `xml` als auch `json` unterstützt. Die Struktur der Ausgabedateien ist in der [weiterführenden Dokumentation](docs/profiles/fsh/fsh-generated/resources) zu finden.    

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

Die Validierungseinstellungen können auch angepasst werden: 

``` Java
        ValidationModule erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERP);
        ValidationOptions options = ValidationOptions.getDefaults();
        options.setProfileValidityPeriodCheckStrategy(ProfileValidityPeriodCheckStrategy.IGNORE);
        options.setProfiles(List.of("https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle|1.0.1"));
        options.setAcceptedEncodings(List.of("xml", "json"));
        options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
        options.setProfileFilterRegex("KBV_PR_ERP_Bundle");
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