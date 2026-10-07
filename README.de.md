<img width="250" alt="gematik GmbH" src="https://raw.githubusercontent.com/gematik/gematik.github.io/master/Gematik_Logo_Flag_With_Background.png" />

# gematik Referenzvalidator

[![Neueste GitHub-Version](https://img.shields.io/github/v/release/gematik/app-referencevalidator?label=release&logo=github)](https://github.com/gematik/app-referencevalidator/releases) [![Maven Central](https://img.shields.io/maven-central/v/de.gematik.refv/referencevalidator.svg)](https://search.maven.org/artifact/de.gematik.refv/referencevalidator) [![Lizenz](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

## Inhaltsverzeichnis

<details>
  <summary>Inhaltsverzeichnis anzeigen</summary>
  <ol>
    <li>
      <a href="#über-das-projekt">Über das Projekt</a>
      <ul>
        <li><a href="#release-notes">Release Notes</a></li>
      </ul>
    </li>
    <li><a href="#funktionen">Funktionen</a></li>
    <li>
      <a href="#erste-schritte">Erste Schritte</a>
      <ul>
        <li><a href="#voraussetzungen">Voraussetzungen</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#verwendung">Verwendung</a></li>
    <li><a href="#mitarbeit">Mitarbeit</a></li>
    <li><a href="#lizenz">Lizenz</a></li>
    <li><a href="#zusätzliche-hinweise-und-haftungsausschluss-der-gematik-gmbh">Zusätzliche Hinweise und Haftungsausschluss</a></li>
    <li><a href="#mitarbeit-und-danksagungen">Mitarbeit und Danksagungen</a></li>
    <li><a href="#kontakt">Kontakt</a></li>
  </ol>
</details>

## Über das Projekt

Der Referenzvalidator ermöglicht die erweiterte Validierung von FHIR-Ressourcen, die im deutschen Gesundheitswesen und
in Anwendungen der Telematikinfrastruktur (TI) eingesetzt werden. Er liefert eine maßgebliche Einschätzung der
Datenvalidität und kann als Referenz für weitere FHIR-Validatoren in TI-Anwendungen dienen.

Weitere Informationen enthalten
die [Use Cases, Anforderungen, Architektur und der Entwicklungsprozess](docs/concept/concept.md).

> [!WARNING]
> Nutzer sind für den Betrieb des Referenzvalidators in ihren Anwendungen und Systemlandschaften verantwortlich. Die
gematik ergreift angemessene Maßnahmen zur Unterstützung der Sicherheit und Performance des Referenzvalidators,
übernimmt jedoch keine Verantwortung für Schäden, die durch die Integration in Produktionssysteme entstehen. Siehe dazu
die Haftungsbeschränkung der [Apache-Lizenz 2.0](https://www.apache.org/licenses/LICENSE-2.0). Nutzer müssen die
Sicherheit des Gesamtsystems unter Berücksichtigung der technischen und organisatorischen Bedingungen ihrer
Betriebsumgebung selbst bewerten. Auch die Performance hängt wesentlich von dieser Umgebung ab.

> [!WARNING]
> Die verbindliche Rolle des Moduls `e-Prescription` (`erp`) des Referenzvalidators in Abrechnungsprozessen ist in
der [Technischen Anlage 7, Anhang 2, zur Arzneimittelabrechnungsvereinbarung nach § 300 Absatz 3 SGB V](https://www.gkv-datenaustausch.de/leistungserbringer/apotheken/apotheken.jsp)
festgelegt. Siehe
außerdem [E-Rezept: Technische Verarbeitbarkeit, Schiedsrichter-Rolle und Problemlösungsverfahren](docs/concept/concept.md#e-rezept-technische-verarbeitbarkeit-schiedsrichter-rolle-und-probleml%C3%B6sungsverfahren).

> [!WARNING]
> Der Referenzvalidator ist nicht für die Prüfung von KIM-Payloads in Produktivumgebungen (PU) vorgesehen.

> [!NOTE]
> Die Verbindlichkeit anderer Validierungsmodule wurde bisher nicht festgelegt. Die gematik empfiehlt deren Nutzung.

### Release Notes

Die [Release Notes](ReleaseNotes.md) beschreiben die Änderungen.

## Funktionen

- FHIR-Ressourcen anhand der referenzierten Profile validieren.
- Der Prüfumfang entspricht dem des [HL7 Java Validators](https://www.hl7.org/fhir/validation.html) und umfasst unter
  anderem:
    - **Struktur:** Jedes Element einer Instanz muss im referenzierten Profil definiert sein.
    - **Kardinalität:** Mindest- und Höchstanzahlen werden geprüft.
    - **Wertebereiche:** Wertebereiche von Eigenschaften, einschließlich aufgelisteter Codes, werden geprüft.
    - **Coding- und CodeableConcept-Bindings:** Instanzcodes werden mit den im Profil definierten Code-Systemen
      abgeglichen.
    - **Constraints und Invarianten:** Die für Profileigenschaften definierten Regeln werden geprüft.
- Gültigkeitszeiträume der in Instanzen referenzierten Profile prüfen.
- FHIR-Package-Abhängigkeiten anhand des Erstellungsdatums der Instanz auswählen, beispielsweise
  datumsabhängige [KBV-Schlüsseltabellen](https://applications.kbv.de/overview.xhtml).

### Validierungsmodule

Ab Version 3.0 werden die Validierungsmodule `erp`, `eau` und das experimentelle Modul `erpta7` nicht mehr zusammen mit
der CLI-Anwendung des Referenzvalidators ausgeliefert. Enthalten sind nur die FHIR-Core-Definitionen für R4 und R5.

Die Module werden in einem eigenen Repository gepflegt und können zur Laufzeit geladen werden. Legen Sie die
Modul-JAR-Dateien im Dateisystem ab und übergeben Sie der CLI den Pfad zum Plugin-Verzeichnis sowie den Namen des
gewünschten Moduls. Pro Anwendungsausführung kann jeweils nur ein Plugin geladen werden.

**TODO:** Liste der verfügbaren Plugins ergänzen.

> [!WARNING]
> Die in diesem [Repository](https://github.com/gematik/app-referencevalidator-plugins/releases) verfügbaren
Validierungsmodule sind aufgrund interner Formatänderungen und des Snapshot-Generierungsprozesses nicht mit Version 3.0
des Referenzvalidators kompatibel.

## Erste Schritte

### Voraussetzungen

Der Referenzvalidator wird als Java-Bibliothek und als CLI-Anwendung bereitgestellt.

Für den Bau dieses Projekts benötigen Sie:

- Java JDK 25 oder höher
- Apache Maven 3.9 oder höher

### Installation

#### CLI-Anwendung

Laden Sie `referencevalidator-cli-X.Y.Z.jar` aus
den [GitHub-Releases](https://github.com/gematik/app-referencevalidator/releases) herunter und legen Sie die Datei in
einem Verzeichnis Ihrer Wahl ab.

#### Java-Bibliothek

Der Referenzvalidator wird über [Maven Central](https://search.maven.org/artifact/de.gematik.refv/referencevalidator)
veröffentlicht. Fügen Sie die Bibliothek zu Ihrem Projekt hinzu:

```xml

<dependency>
    <groupId>de.gematik.refv</groupId>
    <artifactId>referencevalidator-lib</artifactId>
    <version>${version.referencevalidator}</version>
</dependency>
```

Ersetzen Sie `${version.referencevalidator}` durch die gewünschte Version.

> [!WARNING]
> Verwenden Sie die vom Referenzvalidator-Bill-of-Materials (BOM) vorgegebenen Abhängigkeitsversionen, insbesondere für
HL7 Core (`ca.uhn.hapi.fhir`). Abweichende Laufzeitversionen können zu anderen oder unerwarteten Validierungsergebnissen
führen.

## Verwendung

### Kommandozeile

Eine Anleitung zur Verwendung der CLI finden Sie in
der [Dokumentation zur Kommandozeile](./docs/user-guide/cli-user-guide.md).

#### Plugins mit der CLI verwenden

Legen Sie die Plugin-`.jar`-Dateien in einem Verzeichnis namens `plugins` oder in einem anderen konfigurierten
Verzeichnis
ab. Übergeben Sie den Pfad mit `--modules-folder` und wählen Sie das Modul mit `--module-name` aus, oder nutzen Sie die
Parametern in der YAML Konfigurationsdatei.

Beispiel für die Verzeichnisstruktur:

```text
referencevalidator/
├── plugins/
│   └── valmodule-erp-3.0.0.jar
├── referencevalidator-cli-X.Y.Z.jar
└── test-bundle.xml
```

### Java-Bibliothek

Beispiele zur Verwendung der Java-Bibliothek finden Sie in
der [Bibliotheksdokumentation](./docs/user-guide/library-user-guide.md).

## Mitarbeit

Informationen zur Mitarbeit finden Sie in [CONTRIBUTING.md](./CONTRIBUTING.md).

## Lizenz

Copyright 2022–2026 gematik GmbH

Apache-Lizenz, Version 2.0

Die geltenden Bedingungen finden Sie in der [LICENSE-Datei](./LICENSE).

## Zusätzliche Hinweise und Haftungsausschluss der gematik GmbH

1. **Urheberrechtshinweis:** Jedes veröffentlichte Arbeitsergebnis enthält einen ausdrücklichen Hinweis auf die
   Nutzungsbedingungen. Sofern nicht anders angegeben, handelt es sich bei den beschriebenen, bereitgestellten oder
   verlinkten Programmen um freie Software.
2. **Genehmigungshinweis:** Jede Person, die eine Kopie dieser Software und der zugehörigen Dokumentation (die
   „Software“) erhält, darf die Software uneingeschränkt nutzen, kopieren, ändern, zusammenführen, veröffentlichen,
   verbreiten, unterlizenzieren und verkaufen sowie Personen, denen die Software bereitgestellt wird, dieselben Rechte
   einräumen, sofern folgende Bedingungen erfüllt sind:
    1. Der Urheberrechtshinweis und dieser Genehmigungshinweis müssen allen Kopien oder wesentlichen Teilen der Software
       beigefügt werden.
    2. Die Software wird ohne jegliche ausdrückliche oder stillschweigende Gewährleistung bereitgestellt. Dies umfasst
       insbesondere keine Gewährleistung der Gebrauchstauglichkeit, der Eignung für einen bestimmten Zweck oder der
       Nichtverletzung von Rechten Dritter. Die Autoren oder Urheberrechtsinhaber haften nicht für Schäden oder sonstige
       Ansprüche, die aus der Software, ihrer Nutzung oder sonstigen Geschäften mit der Software entstehen, unabhängig
       davon, ob sie auf Vertrag, unerlaubter Handlung oder einem anderen Rechtsgrund beruhen.
    3. Die Software ist das Ergebnis von Forschungs- und Entwicklungsarbeiten und stellt kein haftungsrechtlich
       zugesichertes Produkt dar.
3. Die gematik kann veröffentlichte Ergebnisse jederzeit ohne vorherige Ankündigung oder Begründung vorübergehend oder
   dauerhaft entfernen.
4. Teile dieser Software und in Einzelfällen auch Inhalte wie Texte oder Bilder können mithilfe von KI-Werkzeugen
   erstellt worden sein. Sie unterliegen denselben Prüfungen, Tests und Sicherheitskontrollen wie andere Beiträge. Die
   Funktionalität der Software selbst basiert nicht auf KI-Entscheidungen.
5. Die Einhaltung von Open-Source-Lizenzen hat für uns einen hohen Stellenwert. Wir arbeiten kontinuierlich daran, die
   Compliance und unsere Prozesse zu verbessern. Wenn Sie Probleme feststellen oder Hinweise, Vorschläge oder Kommentare
   haben, kontaktieren Sie uns unter ospo@gematik.de.

## Mitarbeit und Danksagungen

Beiträge, Vorschläge, Fehlerberichte und Feature Requests sind willkommen. Reichen Sie diese
über [GitHub Issues](https://github.com/gematik/app-referencevalidator/issues) oder per E-Mail
an [referenzvalidator@gematik.de](mailto:referenzvalidator@gematik.de) ein.

Teile dieses Projekts basieren auf
dem [ABDA E-Rezept-Referenzvalidator](https://github.com/DAV-ABDA/eRezept-Referenzvalidator/) (Copyright 2022 Deutscher
Apothekerverband, DAV), der unter der [Apache-Lizenz, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) steht.

## Kontakt

Bei Fragen, Vorschlägen, Fehlerberichten oder Feature Requests nutzen
Sie [GitHub Issues](https://github.com/gematik/app-referencevalidator/issues) oder schreiben Sie
an [referenzvalidator@gematik.de](mailto:referenzvalidator@gematik.de).
