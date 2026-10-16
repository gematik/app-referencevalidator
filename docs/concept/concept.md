![gematik GmbH](https://raw.githubusercontent.com/gematik/gematik.github.io/master/Gematik_Logo_Flag_With_Background.png)

# Konzept des gematik Referenzvalidators

**Dokumentationsstand: 16.10.2026 · Bezugsstand: 3.0.0-SNAPSHOT**

Dieses Konzept beschreibt fachliche Zielsetzung, Nutzungsszenarien, Governance und wesentliche technische Eigenschaften. Grundlage der Aktualisierung sind die [arc42-Architekturdokumentation](../arc42/arc42.md), die Anleitungen für [CLI](../user-guide/cli-user-guide.md), [Java-Bibliothek](../user-guide/library-user-guide.md) und [Modulentwicklung](../user-guide/module-user-guide.md), die [Migrationshinweise von 2.17.0 zu 3.0.0](../Regressions.md) sowie die [Release Notes](../../ReleaseNotes.md). Der beschriebene Entwicklungsstand bedeutet keine fachliche Freigabe einer bestimmten Validator-/Modulkombination.

Die bestehenden Anforderungsnummern bleiben zur Nachverfolgbarkeit erhalten. Es wird unterschieden zwischen:

* **Fachlichen Anforderungen:** Bedarf der TI-Anwendungen, nicht automatisch eine implementierte oder für jedes Modul nachgewiesene Eigenschaft.
* **Aktuellem technischem Verhalten:** Eigenschaften des oben genannten Entwicklungsstands mit den in Architektur und Anleitungen beschriebenen Grenzen.
* **Historischen E-Rezept-Regeln:** Die bisherigen Tabellen zur Validierungskonfiguration und Ergebnisinterpretation bleiben als fachlicher Bestand erhalten. HAPI-spezifisches Verhalten und Ausnahmen sind keine unverändert übernommenen Standardregeln der Version 3.0.

Wesentliche Änderungen sind Java 25, der Wechsel von HAPI zum HL7 Core Validator, unabhängig ausgelieferte Modul-JARs, Snapshot-Generierung sowie begrenzte Eingaben und parallele Validierung. Frühere Aussagen über ausschließlich flüchtige Datenverarbeitung, ausnahmslos netzwerkfreien Betrieb, 512 MB Mindestheap oder einmaliges Laden aller Pakete für die gesamte Prozesslaufzeit gelten nicht mehr.

Einführung und Zielsetzung
==========================
Im Kontext der Einführung des elektronischen Rezepts (E-Rezept) kristallisiert sich heraus, dass die Interoperabilität von beteiligten Informationssystemen und formale Korrektheit von übertragenen Daten über die gesamte Prozesskette hinweg unabdingbar ist. Die sich hieraus ergebende Validität erstreckt sich über das E-Rezept hinaus auch auf weitere Anwendungen innerhalb der Telematikinfrastruktur (z.B. eAU). Es ist bereits heute absehbar, dass aktuelle bzw. künftige Projekte innerhalb der Telematikinfrastruktur valide Datensätze benötigen. Leider gestaltet sich die Erstellung und Überprüfung von validen Datensätzen auf FHIR-Basis heute schwierig, weil:

*   FHIR-Profile nicht immer vollständig die zugrundeliegenden Informationsmodelle abbilden
*   FHIR-Profile Konstrukte verwenden, die von Validierungswerkzeugen unterschiedlich interpretiert werden
*   Die Tiefe der sinnvollen Überprüfung (FHIR-Profile referenzieren (rekursiv) weitere Profile, ValueSets, Kataloge etc.) ist für die beteiligten Systeme nicht einheitlich festgelegt
*   Die Abweichungen von Profilen, die von Validierungswerkzeugen als Warnungen festgestellt werden, aktuell unzureichend und nicht einheitlich bei der Validitätsentscheidung berücksichtigt werden.
*   FHIR-Profilversionen mit der Zeit ablaufen und durch neue im Sinne der Anwendung gültigen Versionen ersetzt werden.

Aufgrund dessen besteht Bedarf für die TI-Anwendungen die Validierungsregeln über die Systeme hinweg festzuschreiben und eine Prüfinstanz in Form eines Referenzvalidators bereitzustellen, die diese Regeln auch als Referenz umsetzt.

Begriffsdefinition
==================

Der **gematik** **Referenzvalidator** ist ein Softwarewerkzeug, das die im Rahmen von TI-Anwendung verarbeiteten FHIR-Ressourcen auf ihre "technische Verarbeitbarkeit" hin überprüft. Der Begriff der "technischen Verarbeitbarkeit" kann sich je nach TI-Anwendung unterscheiden, mindestens liegen aber folgende Voraussetzungen zugrunde:

*   Die FHIR-Ressourcen sind konform zu den referenzierten Profilen bzw. Profilversionen
*   Die referenzierten Profile bzw. Profilversionen sind gemäß den für die TI-Anwendung festgelegten Zeiträumen zum Erstellungszeitpunkt der Ressource gültig. Eine neuere Veröffentlichung macht eine ältere Version nicht automatisch ungültig.

**Prüfmodule** bzw. **Validierungsmodule** kapseln anwendungsspezifische Profile, Profilversionen, Paketgruppen, Gültigkeitszeiträume und Ergebnisregeln. Ab Version 3.0 sind sie getrennt veröffentlichte JAR-Dateien, keine eingebauten Bestandteile der CLI. Ohne Modul können FHIR-Core-Ressourcen geprüft werden; Bibliotheksnutzer können eigene FHIR-Pakete bereitstellen. Unterstützt werden JSON und XML sowie die FHIR-Releases R4 (Standard) und R5.

Ein technisches Ergebnis ist **Valid**, wenn nach Anwendung der konfigurierten Meldungsregeln und Filter keine ERROR- oder FATAL-Meldungen verbleiben. Dies belegt weder klinische Richtigkeit noch vollständige Terminologieprüfung. Die fachlich geforderte Gültigkeitsprüfung setzt ein geeignetes Modul, die richtigen Pakete und passende Laufzeitrichtlinien voraus; fehlende Datumsangaben können einen Rückfall auf eine Paketgruppe ohne Enddatum auslösen.

Nutzungsszenarien E-Rezept-Prüfmodul
====================================

Entwicklungsunterstützung
-------------------------

Ein Primärsystemhersteller möchte während der Implementierung des E-Rezept-Moduls die Validität seiner zu Testzwecken erzeugten VO-Datensätze überprüfen. Er verwendet die Stand-alone-Konsolenanwendung mit einem separat bereitgestellten, kompatiblen E-Rezept-Modul. Der Befehl `validate` verarbeitet einzelne Dateien oder Verzeichnisse und erzeugt eine Konsolenzusammenfassung sowie einen HTML- oder JSON-Bericht.

Schiedsrichter-Szenario
-----------------------

Ein empfangendes System A weist einen E-Rezept-Datensatz aufgrund der fehlenden Konformität zu gültigen FHIR-Profilen ab. Das sendende System B nimmt bei der Problemanalyse den Referenzvalidator um festzustellen, ob die gesendete Ressource tatsächlich invalide ist. Der Datensatz wird als Datei über die Kommandozeile an den Referenzvalidator zur Prüfung übergeben. Je nach dem "Urteil" des Referenzvalidators, wird entweder die Implementierung eines im Prozess beteiligten Systems korrigiert um zukünftig valide Datensätze zu erzeugen, oder die Abweisung des Datensatzes angefochten.

Bestätigungsverfahren der gematik
---------------------------------

Im Rahmen der Bestätigungsverfahren der gematik können sich die Primärsysteme einer Reihe von Tests unterziehen, die die grundsätzliche Kompatibilität ihrer Umsetzung zur Spezifikation einer TI-Anwendung für Client-Systeme überprüfen. Der Referenzvalidator wird in die Bestätigungsplattform integriert, um alle eingehenden FHIR-Ressourcen auf ihre technische Validität im Sinne der jeweiligen TI-Anwendung zu überprüfen. Mit diesen Überprüfungen wird sichergestellt, dass die Primärsysteme zumindest im Rahmen der Bestätigungstests im Stande waren technisch valide Datensätze zu erzeugen.

Echtzeitvalidierung von FHIR-Datensätzen
----------------------------------------------------------------

Ein Apothekenrechenzentrum muss wissen, ob ein E-Rezept den technischen Verarbeitungsanforderungen entspricht, um über Annahme, Abweisung oder Korrektur zur Weiterverarbeitung zu entscheiden. Es integriert den Referenzvalidator als Java-Bibliothek oder als externen CLI-Prozess. Die Bibliothek nimmt JSON-/XML-Inhalte oder Dateipfade entgegen und liefert strukturierte Ergebnisse bzw. kategorisierte Fehler. Die CLI verwendet Ressourcendateien, Konfiguration, Prozess-Exitcode und Berichte; eine allgemeine Streaming-Schnittstelle über die Standardeingabe wird hier nicht zugesichert. Die einbettende Anwendung verantwortet Parallelisierung und Ressourcenfreigabe ihrer Bibliotheksaufrufe.

> **Warning**
> Der Betrieb des Referenzvalidators in bestehenden Anwendungen oder Anwendungslandschaften obliegt der Verantwortung der Nutzer. Die gematik trifft angemessene Maßnahmen, um die Sicherheit und Performance des Referenzvalidators zu gewährleisten. Sie übernimmt aber keine Verantwortung für etwaige Schäden, die durch die Integration des Referenzvalidators in Produktionssysteme entstehen (siehe auch Haftungsausschuss der [Apache 2.0-Lizenz](https://www.apache.org/licenses/LICENSE-2.0). Insbesondere müssen die Sicherheitsaspekte des Gesamtsystems unter Einbeziehung der technischen und organisatorischen Rahmenbedingungen der jeweiligen Betriebsumgebung durch die Nutzer selbst bewertet werden. Des Weiteren hängen die Performance-Eigenschaften des Validators stark von der jeweiligen Betriebsumgebung ab.

Weitere Nutzungsszenarien 
====================================

Weiterentwicklung der Profile
-----------------------------

Eine standardisierende Organisation entwickelt ihre FHIR-basierte Spezifikation einschließlich Profildefinitionen kontinuierlich weiter. Sie erstellt unabhängige Validierungsmodule auf Basis des [Modul-Templates](https://github.com/gematik/app-referencevalidator-module-template). Der Modulbau erzeugt Paket-Snapshots, berücksichtigt bei Bedarf dokumentierte Patches und prüft positive Beispielinstanzen gegen das gebaute JAR. Abwärtskompatibilität wird mit repräsentativen älteren und aktuellen Instanzen anhand festgehaltener Validator-/Modulkombinationen überprüft.

> **Warning**
> Die im bisherigen [Plugin-Repository](https://github.com/gematik/app-referencevalidator-plugins/releases) verfügbaren Module sind laut README wegen Formatänderungen und des neuen Snapshot-Prozesses nicht mit Version 3.0 kompatibel. Ein Modul muss für die eingesetzte Validatorversion bereitgestellt und getestet werden; gleicher Name oder ladbares Manifest allein belegen keine Kompatibilität.

E-Rezept: Nutzung- und Stakeholderanforderungen an den Referenzvalidator
========================================================================

Die folgenden Anforderungen beschreiben den fachlichen Bedarf. Die Vorgabe, rückwirkend die höchste Patch-Version von Abhängigkeiten einzusetzen, ist keine automatische Laufzeitfunktion: Freigegebene Module müssen die vorgesehenen Versionen in ihren Paketgruppen festlegen. Für reproduzierbare Entscheidungen sind die tatsächlich geprüften Artefakte aufzubewahren.

<table class="wrapped confluenceTable" style="letter-spacing: 0.0px;">
    <colgroup>
        <col style="width: 29.0px;">
        <col style="width: 430.0px;">
        <col style="width: 337.0px;">
        <col style="width: 344.0px;">
    </colgroup>
    <tbody>
    <tr>
        <th class="confluenceTh" colspan="1"><br></th>
        <th class="confluenceTh">Erfordernisse</th>
        <th class="confluenceTh">Nutzung- und Stakeholderanforderungen</th>
        <th class="confluenceTh" colspan="1">Systemanforderungen</th>
    </tr>
    <tr>
        <td class="confluenceTd" colspan="1">1</td>
        <td class="confluenceTd"><p>Ein<strong> Entwickler vom PVS/KIS-, AVS-, Apothekenrechenzentrum-,
            Annahmestellen-Software, E-Rezept-App</strong> muss ein Werkzeug verfügbar haben, mit dem FHIR-Ressourcen,
            die bei E-Rezepten zum Einsatz kommen, gegen gültige FHIR-Profile/Profilversionen validiert werden können
            (aktuell gültige oder demnächst gültige). Damit soll die Qualitätssicherung der Anpassungen an
            E-Rezept-Modulen unterstützt werden.</p></td>
        <td class="confluenceTd"><p>1.1. Der Entwickler muss bei dem Referenzvalidator die zu validierende
            FHIR-Ressource aus der TI-Anwendung E-Rezept eingeben können.</p>
            <p>1.2. Der Entwickler muss an dem Referenzvalidator das binäre Ergebnis der Validierung erkennen können.
                Falls das Ergebnis der Validierung negativ sein sollte, muss der Entwickler an den Ausgaben des Referenzvalidators die erkannten Verletzungen der FHIR-Profile eindeutig erkennen können.
            </p>
            <p>1.3 Der Benutzer muss bei dem Referenzvalidator eine zu validierende FHIR-Ressource eingeben können, die
                FHIR-Profilversionen referenziert, die zwar schon veröffentlicht wurden, die aber noch nicht gültig
                sind.</p></td>
        <td class="confluenceTd" colspan="1">
            <div class="content-wrapper"><p>1.1.1. Der Referenzvalidator muss Instanzen aller im Kontext von E-Rezept definierten Profile als Eingabe akzeptieren können.</p>
                <p>1.2.1. Der Referenzvalidator muss Zugriff auf für die Validierung erforderlichen FHIR-Pakete und Ressourcen sowie Gültigkeitszeiträume der Profile, Profilversionen, FHIR-Pakete und Ressourcen haben (siehe <a
                        class="external-link"
                        href="https://github.com/gematik/api-erp/blob/master/docs/erp_fhirversion.adoc" rel="nofollow" target="_blank">Profil-Roadmap</a>). Insbesondere sollen die im Kontext von E-Rezept eingesetzten <a href="https://applications.kbv.de/overview.xhtml" target="_blank">Schlüsseltabellen der KBV</a> sowie deren Gültigkeitszeiträume und Release-Zyklen berücksichtigt werden. 
                    </p>
                    <p>Die Auswahl der FHIR-Packages, die zur Validierung hinzugezogen werden müssen, richtet sich nach den von der jeweiligen Standardisierungsorganisation festgelegten Abhängigkeiten des FHIR-Packages, das für das jeweilige Profil verbindlich ist. Abweichend davon und falls verfügbar müssen FHIR-Packages der Abhängigkeiten mit der höchsten Patch-Versionnummer eingesetzt werden (auch rückwirkend). Als Zeitpunkt zur Auswahl der geltenden Terminologien dienen die <a href="#creation-date-elements">Erstellungszeitpunktangaben</a> aus den zu validierenden Ressourcen.
                <p>1.2.2. Das E-Rezept muss als Ergebnis eine Valid / Invalid-Antwort und, falls Invalid, als Ergänzung weitere eventuell erkannte Konformitätsprobleme ausgeben können. </p>
                <p>1.2.3. Um die Valid / Invalid-Antwort zu produzieren, muss der Referenzvalidator die <a href="#validation-configuration">Validierung-Konfiguration</a> und <a href="#result-interpretation">Interpretationsregeln</a> anwenden. </p>
                <p>1.2.4. Der Referenzvalidator kann als Startparameter eine unterstützte Profil-URL akzeptieren, um die Konformität der eingegebenen Instanz zu dem zugrundeliegenden Profil zu überprüfen.</p>
                <p>1.3.1. Der Referenzvalidator
                    muss Zugriff auf die bereits veröffentlichten aber potenziell noch nicht verpflichtend geltenden
                    FHIR-Pakete haben (siehe <a class="external-link"
                                                                 href="https://github.com/gematik/api-erp/blob/master/docs/erp_fhirversion.adoc"
                                                                 rel="nofollow" target="_blank">Profil-Roadmap</a>) </p>
                <p>1.3.2. Als Referenzzeitpunkt zur Gültigkeitsprüfung von referenzierten Profilversionen sollen die <a href="#creation-date-elements">Erstellung-Datumsangaben aus
                    der FHIR-Ressource</a> dienen</p> 
                <p>1.3.3.<b>*</b> Der Referenzvalidator kann als Startparameter einen Referenzzeitpunkt akzeptieren, der anstatt der in
                    der Nachricht angegebenen Datumsangabe, zur Bestimmung der zum Zeitpunkt gültigen FHIR-Profilversion
                    verwendet werden soll. (siehe Use Case Bestätigungsverfahren)</p>
                <p></p>
                <p>1.3.4. Der Referenzvalidator kann mittels eines
                    Startparameters auch FHIR-Core-Ressourcen validieren, die keine explizite Profilangabe aufweisen (z.B. 
                    Parameters).</p>
                <p>1.3.5. Der Referenzvalidator kann als Startparameter die zu unterstützenden Instanzformate (XML und/oder JSON) akzeptieren. Standardmäßig soll nur XML akzeptiert werden.</p>
                <p>1.3.6. Die Zeitraum-Gültigkeitsprüfung der Profilversionen kann bei Bedarf mit einem Startparameter abgeschaltet werden.</p>
                </div>
        </td>
    </tr>
    <tr>
        <td class="confluenceTd" colspan="1">2</td>
        <td class="confluenceTd" colspan="1"><p>Ein <strong>Apothekenrechenzentrum </strong>oder eine <strong>KK-Annahmestelle</strong>
            müssen im Konfliktfall wissen, ob ein vor maximal 2 Jahren erstellter
            und als technisch invalider bewerteter und abgewiesener E-Rezept-Datensatz den zum Erstellungszeitpunkt
            geltenden FHIR-Profilversionen entsprach, um die Abweisung ggf. neu bewerten zu können.</p></td>
        <td class="confluenceTd" colspan="1"><p>2.1 Der Benutzer muss bei dem Referenzvalidator die zu validierende
            E-Rezept-FHIR-Ressource, die maximal vor 2 Jahren erstellt wurde, eingeben können.</p>
            <p><br></p></td>
        <td class="confluenceTd" colspan="1">
            <div class="content-wrapper"><p>2.1.1. Der Referenzvalidator muss FHIR-Ressourcen, deren Ausstellungsdatum
               2 Jahre zurückliegen kann, als Eingabe akzeptieren können (siehe <a href="#creation-date-elements">Profil-Elemente zur Bestimmung des Instanz-Erstellungszeitpunktes</a>).
            </p>
                <p>2.1.2. Der Referenzvalidator muss Zugriff auf für die Validierung erforderlichen FHIR-Pakete und Ressourcen sowie Gültigkeitszeiträume der Profile, Profilversionen, FHIR-Pakete und Ressourcen der letzten 3,5 Jahre haben (siehe auch E-Rezept-Fachdienst-Anforderungen, A_19252-02).
                </p>
                </div>
        </td>
    </tr>
    <tr>
        <td class="confluenceTd" colspan="1">3</td>
        <td class="confluenceTd" colspan="1"><p>Ein <strong>Apotheken-Rechen-Zentrum </strong>oder eine <strong>KK-Annahmestelle</strong>
            muss einen Referenzvalidator verfügbar haben, um FHIR-Ressourcen im laufenden Betrieb automatisiert validieren zu können.</p></td>
        <td class="confluenceTd" colspan="1"><p><br></p></td>
        <td class="confluenceTd" colspan="1">
            <div class="content-wrapper"><p>3.1.1 Der Referenzvalidator muss als Open Source bereitgestellt werden.
                </p>
                <p>3.1.2. Der Referenzvalidator muss als Java 25-Bibliothek und als Konsolenanwendung bereitgestellt
                    werden, damit die Integration in andere Anwendungen möglich wird.</p> 
                <p>3.1.3. Der Referenzvalidator kann als Startparameter einen regulären Ausdruck übergeben bekommen, um die in den Instanzen referenzierten Profile dagegen prüfen zu können. Damit soll im Kontext der Verarbeitung von TA7-Dateien die Möglichkeit geschaffen werden, die extrahierten base64-Daten auf ihren "Typ" zu prüfen (Verordnung, Quittung, Abgabedaten etc.)</p>
            </div>
        </td>
    </tr>
    </tbody>
</table>

Die mit <b>*</b> gekennzeichnete Anforderung 1.3.3 (externer Referenzzeitpunkt) ist weiterhin kein hier dokumentierter Startparameter. Weitere Einordnungen zum aktuellen Stand:

| Anforderung | Einordnung für Version 3.0 |
| --- | --- |
| 1.2.1, 1.3.1, 2.1.2 | Fachliche Inhalte und zeitliche Abdeckung hängen vom separat bereitgestellten Modul ab. Es gibt keine automatische Versorgung mit allen bisherigen oder künftigen E-Rezept-Paketen. |
| 1.2.4 | Ein explizites Prüfprofil ist möglich. Die Modul-Paketauswahl extrahiert trotzdem zuerst das Ressourcenprofil; ein Override ersetzt dort keine fehlende Profilangabe. |
| 1.3.2 | Datumsquelle und Zeiträume stehen im Modulmanifest; Auswahl und Rückfallverhalten werden unten beschrieben. |
| 1.3.4 | Core-Validierung ohne Modul benötigt kein modulunterstütztes Profil. Sie ist vom profilabhängigen Modul-Auswahlverfahren zu unterscheiden. |
| 1.3.5 | JSON und XML werden unterstützt; „standardmäßig nur XML“ beschreibt nicht das aktuelle Verhalten. |
| 1.3.6 | Das Abschalten der Zeitraumprüfung erzwingt keine Paketgruppe für ein Datum außerhalb aller Zeiträume. |
| 3.1.2 | Java 25 ersetzt die frühere Basis Java 11. |
| 3.1.3 | Der Profil-RegEx wirkt bei Modul-Paketauswahl auf das extrahierte Profil vor einem Profil-Override, nicht als allgemeiner Ergebnisfilter. |

> **Warning**
> Für das [Schiedsrichter-Szenario](#e-rezept-technische-verarbeitbarkeit-schiedsrichter-rolle-und-problemlösungsverfahren) ist die für das Verfahren festgelegte Kombination aus Validator, Modul, Paketen und Konfiguration zu verwenden. Technisch erforderliche Modul- und Dateiauswahl sind nicht untersagt. Ergebnisverändernde QA-Overrides, zusätzliche Unterdrückungen oder abgeschaltete Zeitraumprüfungen dürfen nicht eigenmächtig vom vereinbarten Prüfverhalten abweichen.

Anzuwendende Validierungskonfiguration
-----------------------------------------------------------------

**Fachlicher E-Rezept-Bestand, keine Liste aktueller Engine-Defaults.** Die Tabelle hält die bisher dokumentierten Vorgaben und Abweichungen fest. Ihre Übernahme in ein 3.0-Modul ist anhand von Manifest, Kontextkonfiguration und Regressionstests zu prüfen. Die neue Engine kann insbesondere nicht auflösbare Bundle-Referenzen als ERROR, unbekannte CodeSystems dagegen nur als WARNING melden; siehe [Migrationshinweise](../Regressions.md).

`ignoredCodeSystems` und `ignoredValueSets` beeinflussen im aktuellen Ergebnisworkflow die Schwere passender Meldungen. Sie beweisen weder eine erfolgreiche Terminologieprüfung noch, dass die Engine die Prüfung vollständig ausgelassen hat. Offlinebetrieb erlaubt Prüfungen lokal vorhandener Terminologien, garantiert aber keine vollständige Abdeckung externer Terminologien.

<table id="validation-configuration">
<tr>
<th>Konfigurationselement</th>
<th>Fachlich dokumentierter Wert (Altstand)</th>
<th>Anmerkung</th>
</tr>
<tr>
<td>Unbekannte Profile in den Instanzen führen zu Fehlern</td>
<td>ja</td>
<td></td>
</tr>
<tr>
<td>Unbekannte Erweiterungen (Extensions) führen zu Fehlern</td>
<td>ja</td>
<td></td>
</tr>
<tr>
<td>Code-Systeme und ValueSets, die nicht validiert werden</td>
<td>
<ul>
<li>http://fhir.de/CodeSystem/ifa/pzn</li>
<li>http://fhir.de/CodeSystem/ask</li>
<li>http://fhir.de/CodeSystem/bfarm/atc (wird in _GEM_ERP_PR_Medication_ verwendet)</li>
<li>http://snomed.info/sct (wird in _GEM_ERP_PR_Medication_ verwendet)</li>
<li>https://terminologieserver.bfarm.de/fhir/CodeSystem/arzneimittel-referenzdaten-pharmazeutisches-produkt (wird in _GEM_ERP_PR_Medication_ verwendet)</li>
<li>http://hl7.org/fhir/uv/ips/ValueSet/medicine-doseform (wird in _GEM_ERP_PR_Medication_ verwendet)</li>
</ul></td>
<td></td>
</tr>
<tr>
<td>Code-Systeme und ValueSets, bei denen die Validierung mit dem Schweregrad <i>Warnung</i> erfolgen soll</td>
<td>
<ul>
<li>http://unitsofmeasure.org</li>
</ul></td>
<td>Das Verhalten wurde mit der Version 2.0 des E-Rezept-Moduls eingeführt. Die Gründe sind:
<ul>
<li>Fehlende MustSupport-Anforderung an die Angaben im FHIR-Profil</li>
<li>Der bisherigen Abwesenheit von Use Cases für die Verwendung der UCUM-Codes</li>
<li>Der unpräzisen Vorgaben bzgl. der Befüllung der Codes (bspw. Abbildung von "Stücken")</li>
</ul></td>
</tr>
<tr>
<td>Referenzierte ValueSets, für die eine Definition fehlt, führt zu Fehlern</td>
<td>ja</td>
<td></td>
</tr>
<tr>
<td>Nicht auflösbare Querreferenzen in Bundles, führen zu Fehlern</td>
<td>nein</td>
<td>Der Fehler tritt auf, falls die in einem Bundle verwendeten Referenzen sich nach dem <a href="https://hl7.org/fhir/R4B/bundle.html#references">Algorithmus aus der FHIR-Spezifikation</a> nicht auflösen lassen. 

Die frühere Regel vermied Fehler zur Angleichung an den ABDA Referenzvalidator und den damaligen E-Rezept-Fachdienst. Das Ignorieren solcher Referenzen kann fehlende Referenzziele oder unpassende Zieltypen verdecken. In Version 3.0 können nicht auflösbare oder unpassende Bundle-Referenzen als ERROR erscheinen (Migration R3); die frühere Wertung „nein“ ist keine aktuelle Engine-Garantie.</td>
</tr>
<tr>
<td>Ressourcen in document-Bundles, die aus der Composition heraus über Referenzen nicht erreichbar sind und keine anderen Bundle-Ressourcen referenzieren, führen zu Fehlern</td>
<td>ja</td>
<td>Das Verhalten entspricht der <a href="https://www.hl7.org/fhir/R4/documents.html#content">FHIR Standard-Vorgabe</a>:
"The document bundle SHALL include only:
<ol>
<li>The Composition resource, and any resources directly or indirectly (e.g. recursively) referenced from it</li>
<li>A Binary resource containing a stylesheet (as described below)</li>
<li>Provenance Resources that have a target of Composition or another resource included in the document."</li></ol>
</td>
</tr>
<tr>
<td>Ressourcen in document-Bundles, die aus der Composition heraus über Referenzen nicht erreichbar sind aber andere Bundle-Ressourcen referenzieren, führen zu Fehlern</td>
<td>nein</td>
<td>Das früher beobachtete Verhalten widersprach der <a href="https://www.hl7.org/fhir/R4/documents.html#content">FHIR Standard-Vorgabe</a>:
"The document bundle SHALL include only:
<ol>
<li>The Composition resource, and any resources directly or indirectly (e.g. recursively) referenced from it</li>
<li>A Binary resource containing a stylesheet (as described below)</li>
<li>Provenance Resources that have a target of Composition or another resource included in the document."</li></ol> 
Solche Ressourcen führten im damaligen Stand nur zu einer Warnung. Die Wertung muss für die eingesetzte Engine-/Modulversion erneut geprüft werden.</td>
</tr>
<tr>
<td>Werte bestehend nur aus "Leerzeichen" führen zu Fehlern</td>
<td>nein</td>
<td>Das früher beobachtete Verhalten widersprach der <a href="https://hl7.org/fhir/R4/xml.html#2.6.1">FHIR Standard-Vorgabe</a> ("Attributes cannot be empty. Either they are absent, or they are present with at least one character of non-whitespace content"). Werte nur aus Leerzeichen führten damals zu Warnungen. Die aktuelle Wertung ist versionsbezogen zu testen.</td>
</tr>
<tr>
<td>Werte, die mit "Leerzeichen" starten oder enden, führen zu Fehlern</td>
<td>nein</td>
<td>Die <a href="https://hl7.org/fhir/R4/xml.html#2.6.1">FHIR Standard-Vorgabe</a> lautet: "Implementers SHOULD trim leading and trailing whitespace before writing and SHOULD trim leading and trailing whitespace when reading attribute values (for XML schema conformance)". Führende oder abschließende Leerzeichen können die Verarbeitung erschweren und führten im früheren Stand zu Warnungen. Die aktuelle Wertung ist versionsbezogen zu testen.</td>
</tr>

</table>

Anzuwendende Ergebnis-Interpretationsregeln
-----------------------------------------------------------------

Die folgenden **historischen, paketgebundenen HAPI-Ausnahmen** bleiben zur fachlichen Nachverfolgbarkeit erhalten. Sie sind keine automatisch aktiven 3.0-Regeln. Modulherausgeber müssen prüfen, ob der jeweilige Fehler noch auftritt, Meldungs-ID und Text unverändert sind und eine eng begrenzte Transformation weiterhin gerechtfertigt ist. Ein Enginewechsel kann Ausnahmen überflüssig oder bisherige Matcher unwirksam machen.

<table id="result-interpretation">
<tr>
<th>Package</th>
<th>Regel</th>
<th>Anmerkung</th>
</tr>
<tr>
<td>de.abda.erezeptabgabedaten-1.0.3.tgz</td>
<td>Folgende HAPI-Ausgaben werden als INFO statt als ERROR-Nachrichten gewertet:
<ul>
<li>MessageId: _DT_Fixed_Wrong. Diagnostics enthält: 'http://fhir.abda.de/Identifier/DAV-Herstellerschluessel' but must be 'http://fhir.de/NamingSystem/arge-ik/iknr'</li>
<li>MessageId: Reference_REF_CantMatchChoice. Diagnostics enthält: http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-ZusatzdatenEinheit</li>
<li>MessageId: Reference_REF_CantMatchChoice. Diagnostics enthält: http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-ZusatzdatenHerstellung</li>
<li>MessageId: Reference_REF_CantMatchChoice. Diagnostics enthält: http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-Base-ZusatzdatenHerstellung</li>
<li>MessageId: Validation_VAL_Profile_NotSlice. Diagnostics enthält: http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-ZusatzdatenEinheit</li>
</ul>
</td>
<td>Die Interpretationsregel kam infolge eines Profilierungsfehlers zustande</td>
</tr>
<tr>
<td>de.gematik.epa.medication-1.0.3.tgz</td>
<td>Folgende HAPI-Ausgaben werden als WARN statt als ERROR-Nachrichten gewertet:
<ul>
<li>MessageId: BUNDLE_BUNDLE_ENTRY_MULTIPLE_PROFILES. Diagnostics enthält: 2 profiles found for contained resource. More than one is not supported at this time. (Type Medication: https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication-pharmaceutical-product, https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication-pzn-ingredient)</li>
</ul>
</td>
<td>Im Profil werden zwei alternative Profile für contained-Ressourcen erlaubt. Die Ausnahme kompensierte eine Beschränkung der damals eingesetzten HAPI-Version. Ihre Notwendigkeit und ihr Matcher sind für Version 3.0 erneut zu prüfen; aus dem Enginewechsel allein folgt keine bestätigte Behebung.</td>
</tr>
</table>

Profil-Elemente zur Bestimmung des Instanz-Erstellungszeitpunktes
-----------------------------------------------------------------

Die Tabelle beschreibt fachliche Datumsquellen, keine fest in der Engine codierte Zuordnung. Das Modul hinterlegt passende FHIRPath-Ausdrücke in `validityDateSource` bzw. `validityDateSourceByVersion`. Profilnamen und Ausdrücke müssen zur konkreten Profilversion passen; eine Expression für ein Bundle muss beispielsweise die entsprechende enthaltene Ressource erreichen.

<table id="creation-date-elements">
    <tr>
        <th>Profil</th>
        <th>Datenelement</th>
        <th>Anmerkung</th>
    </tr>
    <tr>
        <td>KBV_PR_ERP_Bundle</td>
        <td>MedicationRequest.authoredOn bzw. SupplyRequest.authoredOn</td>
        <td>Verordnung, Datum der Ausstellung</td>
    </tr>
    <tr>
        <td>ErxMedicationDispense</td>
        <td>MedicationDispense.whenHandedOver</td>
        <td>Abgabedatum</td>
    </tr><tr>
        <td>GEM_ERP_PR_MedicationDispense</td>
        <td>MedicationDispense.whenHandedOver</td>
        <td>Abgabedatum ab 01.07.2023</td>
    </tr><tr>
        <td>GEM_ERP_PR_CloseOperationInputBundle</td>
        <td>MedicationDispense.whenHandedOver</td>
        <td>Abgabedatum ab 01.07.2023</td>
    </tr>
<tr>
        <td>ErxReceipt</td>
        <td>Composition.date</td>
        <td>Quittung, Ausstellungsdatum</td>
    </tr><tr>
        <td>GEM_ERP_PR_Bundle</td>
        <td>Composition.date</td>
        <td>Quittung, Ausstellungsdatum ab 01.07.2023</td>
    </tr><tr>
        <td>GEM_ERPCHRG_PR_ChargeItem</td>
        <td>ChargeItem.enteredDate</td>
        <td>PKV Patientenrechnung</td>
    </tr><tr>
        <td>GEM_ERPCHRG_PR_Consent</td>
        <td>Consent.dateTime</td>
        <td>PKV-Einwilligung</td>
    </tr><tr>
        <td>DAV-PR-ERP-AbgabedatenBundle</td>
        <td>MedicationDispense.whenHandedOver</td>
        <td>GKV Abgabedaten, Abgabedatum</td>
    </tr><tr>
        <td>DAV-PKV-PR-ERP-AbgabedatenBundle</td>
        <td>MedicationDispense.whenHandedOver</td>
        <td>PKV Abgabedaten, Abgabedatum</td>
    </tr>
    <tr>
<td>GKVSV_PR_TA7_Sammelrechnung_Bundle</td>
        <td>Composition.date</td>
        <td>Abrechnungsdaten, Abrechnungsmonat</td>
    </tr>
    <tr>
        <td>GKVSV_PR_TA7_Rechnung_Bundle</td>
        <td>Composition.date</td>
        <td>Abrechnungsdaten, Abrechnungsmonat ab 01.07.2023</td>
    </tr>
</table>

E-Rezept: Anforderungen an den Entwicklungsprozess
==================================================

<table class="wrapped confluenceTable">
    <colgroup>
        <col>
        <col>
    </colgroup>
    <tbody>
    <tr>
        <th class="confluenceTh"><br></th>
        <th class="confluenceTh">Anforderung</th>
    </tr>
    <tr>
        <td class="confluenceTd">1</td>
        <td class="confluenceTd">Die <strong>gematik </strong>muss im Falle von eingegangenen neuen oder geänderten
            Anforderungen an den Referenzvalidator einen Änderungsprozess einschließlich Bewertung der Änderungen
            starten können
        </td>
    </tr>
    <tr>
        <td class="confluenceTd">2</td>
        <td class="confluenceTd"><strong>Eine Krankenkasse, die gematik, ein Apothekenrechenzentrum oder eine
            Apotheke</strong> müssen im Falle der entdeckten Verarbeitungsprobleme von E-Rezepten einen Prozess zur
            Änderung der Prüflogik des Referenzvalidators starten können
        </td>
    </tr>
    <tr>
        <td class="confluenceTd">3</td>
        <td class="confluenceTd">Ein<strong> Entwickler vom PVS/KIS-, AVS-, Apothekenrechenzentrum-,
            Annahmestellen-Software </strong>muss spätestens 3 Monate vor dem Gültigkeitsbeginn der neuen
            Profilversionen den aktualisierten Referenzvalidator verfügbar haben, mit dem Anpassungen im E-Rezept-Modul
            und Änderungen an den FHIR-Ressourcen getestet werden können
        </td>
    </tr>    <tr>
        <td class="confluenceTd">4</td>
        <td class="confluenceTd">Ein<strong> Entwickler vom PVS/KIS-, AVS-, Apothekenrechenzentrum-,
            Annahmestellen-Software </strong>muss eine neue Version des Prüfmoduls E-Rezept nach ihrer Veröffentlichung in Integration testen und eventuell aufgedeckte Probleme melden können, um sicherzustellen, dass nur stabile und funktional korrekte Versionen die Rolle des <a href="#e-rezept-technische-verarbeitbarkeit-schiedsrichter-rolle-und-problemlösungsverfahren">Schiedsrichters</a> erlangen. 
        </td>
    </tr>
    </tbody>
</table>

Weitere Nutzung- und Stakeholderanforderungen an den Referenzvalidator
========================================================================

<table class="wrapped confluenceTable" style="letter-spacing: 0.0px;">
    <colgroup>
        <col style="width: 29.0px;">
        <col style="width: 430.0px;">
        <col style="width: 337.0px;">
        <col style="width: 344.0px;">
    </colgroup>
    <tbody>
    <tr>
        <th class="confluenceTh" colspan="1"><br></th>
        <th class="confluenceTh">Erfordernisse</th>
        <th class="confluenceTh">Nutzung- und Stakeholderanforderungen</th>
        <th class="confluenceTh" colspan="1">Systemanforderungen</th>
    </tr>
    <tr>
        <td>1</td>
        <td>Ein Spezifikation-Ersteller muss ein Werkzeug verfügbar haben, mit dem die Validität der Beispielinstanzen im Rahmen der Weiterentwicklung der FHIR-basierten Spezifikation kontinuierlich überprüft werden können.</td> 
        <td>
            1.1. Der Spezifikation-Ersteller muss FHIR-package der zu erstellenden Spezifikation, Validierungskonfiguration sowie Beispielinstanzen eingeben können.<br/>
            1.2. Der Spezifikation-Ersteller muss erkennen können, ob die eingegebenen Beispielinstanzen zu dem übergebenen FHIR-package entsprechend der Validierungskonfiguration konform sind.
        </td>
        <td>
            1.1.1 Ein eigenständiger Modulbau muss FHIR-Packages, Validierungskonfiguration sowie Beispielinstanzen akzeptieren und daraus ein Validierungsmodul erstellen können. Version 3.0 verwendet ein separates Template-Projekt mit verwaltetem Modul-Parent und der CLI für Snapshot-Generierung und Verifikation.<br/>
            1.2.1 Der Referenzvalidator muss die unabhängig erstellten Plugins zur Validierung von Instanzen hinzuziehen können. Für die CLI-Variante des Referenzvalidators muss dafür ein spezieller Ablageort für die Plugins definiert werden, von wo aus der Referenzvalidator die letzteren automatisch lädt. Für die Java-Bibliothek-Variante muss die Angabe des Plugins über die API möglich sein
        </td>
    </tr>
    </tbody>
</table>

Referenzvalidator: Infrastrukturelle Anforderungen
===================================================

<table class="wrapped relative-table confluenceTable" style="width: 86.4426%;">
    <colgroup>
        <col style="width: 1.88067%;">
        <col style="width: 9.40337%;">
        <col style="width: 30.5447%;">
        <col style="width: 58.1712%;">
    </colgroup>
    <tbody>
    <tr>
        <th class="confluenceTh"><br></th>
        <th class="confluenceTh">Erfordernis</th>
        <th class="confluenceTh">Nutzung- und Stakeholderanforderungen</th>
        <th class="confluenceTh">Systemanforderungen</th>
    </tr>
    <tr>
        <td class="confluenceTd">1</td>
        <td class="confluenceTd">Der Entwickler von einer Software, die FHIR-Ressourcen im Kontext einer TI-Anwendung
            verarbeitet, muss einen Referenzvalidator für die jeweilige TI-Anwendung verfügbar haben, um in der
            Qualitätssicherung der entwickelten Funktionen, insbesondere Verarbeitung von FHIR-Ressourcen, unterstützt
            zu werden.
        </td>
        <td class="confluenceTd">
            <ul>
                <li>1.1 Der Entwickler muss beim Referenzvalidator eine zu validierende FHIR-Ressource einschließlich eventueller Referenzvalidator-Konfiguration eingeben können
                </li>
            </ul>
        </td>
        <td class="confluenceTd">
            <div class="content-wrapper">
                <ul>
                    <li>1.1.1 Der Referenzvalidator muss als Eingabe folgendes akzeptieren:
                        <ol>
                            <li class="auto-cursor-target">Eine zu validierende FHIR-Ressource aus dem Kontext einer
                                TI-Anwendung
                            </li>
                            <li class="auto-cursor-target">Ein separat bereitgestelltes Validierungsmodul für anwendungsspezifische Prüfung oder reine FHIR-Core-Validierung ohne Modul</li>
                            <li>Modulbezogene Konfiguration; ein externer Referenzzeitpunkt gemäß Anforderung 1.3.3 ist weiterhin nicht als aktueller Startparameter dokumentiert</li>
                            <li>Modul-unabhängige Konfigurationseinstellungen (z.B. für detaillierte Protokollausgaben)
                            </li>
                        </ol>
                    </li>
                    <li>1.1.2 Der Referenzvalidator muss separat bereitgestellte Prüfmodule für unterschiedliche TI-Anwendungen laden können. Ein
                        Prüfmodul besteht dabei aus
                        <ol>
                            <li>Profilen, Profilversionen, Paketen und Gültigkeitsräumen, die für eine Validierung
                                erforderlich sind
                            </li>
                            <li>Konfiguration der zugrundeliegenden Validierungsbibliothek
                            </li>
                            <li>(optional) Paketspezifische Interpretationsregeln der Ausgaben der zugrundeliegenden Validierungsbibliothek<br>
                            </li>
                        </ol>
                    </li>
                    <li>1.1.3. Der Referenzvalidator muss die Release-Version, unterstützten Module, Profile, Profilversionen, Gültigkeitszeiträume, FHIR-Paket-Abhängigkeiten als Information ausgeben können</li>
                    <li>1.1.4. Mittels eines Startparameters muss der Referenzvalidator interne Protokolle und sämtliche Validierungsmeldungen unabhängig vom Validierungsergebnis ausgeben können. Ohne den Parameter enthalten die Validierungsmeldungen des Validators nur die eventuell aufgedeckten Validierungsfehler.</li>
                </ul>
                <p><br></p></div>
        </td>
    </tr>
    </tbody>
</table>

Anforderung 1.1.3 beschreibt den fachlich gewünschten Informationsumfang, keine Zusage eines einzelnen CLI-Befehls für sämtliche Modul-, Profil- und Paketinformationen. Verfügbare Befehle und Diagnoseoptionen stehen in der [CLI-Anleitung](../user-guide/cli-user-guide.md). Anders als im früheren Anforderungsbild schreibt die CLI Berichte auf Datenträger; Bibliotheksoptionen behalten standardmäßig alle Meldungen. Diagnoseumfang, Engine-Meldungsrichtlinien und Ergebnisfilter sind getrennt zu konfigurieren.

Architekturskizze
=================

Technische Grundlage und Bausteine
---------------------------------

Der **HL7 Core Validator** übernimmt Konformitätsprüfungen und Snapshot-Operationen. Die BOM legt `org.hl7.fhir.validation` im beschriebenen Stand auf **7.0.1** fest. Version 3.0 ersetzt damit die bisherige HAPI-Validierungsengine. Verbleibende HAPI-bezogene Modellhilfen oder Abhängigkeiten sind keine zweite Engine. Die Laufzeit benötigt **Java 25**; für den Bau wird Maven 3.9 oder neuer verwendet.

| Baustein | Verantwortung |
| --- | --- |
| `referencevalidator-cli` | `validate`, `generate-snapshot`, `configuration`; Eingaben, Konfiguration, Ablaufsteuerung, begrenzte Parallelisierung und Berichte. |
| `referencevalidator-lib` | Öffentliche Schnittstellen, Modelle und Implementierungen für Konfiguration, FHIR-Kontexte, Paketauflösung, Validierung, Snapshots und Modulladen. Boundary-, Control- und Entity-Pakete liegen innerhalb eines Artefakts. |
| `validation-module-api` | Manifestmodell und SPI-Vertrag für externe Validierungsmodule. |
| `validation-module-parent` und `bom` | Verwalteter Modulbau und abgestimmte Abhängigkeitsversionen; keine deploybaren Validierungsmodule. |
| `report-aggregate` | Aggregation der JaCoCo-Testabdeckung. |
| `performance-benchmarks` | Optionales JMH-Modul, aktiviert über das Profil `jmh`. |

Die aktuellen [Baustein-](../arc42/05-building-block-view.md) und [Laufzeitsichten](../arc42/06-runtime-view.md) beschreiben die Verantwortungsgrenzen. Die folgende Übersicht ersetzt die früheren HAPI-Diagramme:

![Referenzvalidator Architektur](../arc42/diagrams/referencevalidator-architecture.drawio.png)

Validierung und Paketauswahl
----------------------------

1. Die CLI liest ihre Konfiguration, lädt gegebenenfalls das Modul und sammelt Dateien innerhalb der Eingabegrenzen. Core-Definitionen und Modularchive werden über den Paketresolver verfügbar gemacht.
2. Für die **Modul-Paketauswahl** wird genau ein oberstes `meta.profile` erwartet; mehrere Einträge führen zu einem Extraktionsfehler. Fehlt bei Bundle oder Parameters das oberste Profil, wird nur die erste ressourcenhaltige Entry bzw. der erste ressourcenhaltige Parameter betrachtet und dessen erstes Profil verwendet. Dies ist keine vollständige rekursive Prüfung.
3. Ein Profil-RegEx wirkt auf das extrahierte Profil, danach greift gegebenenfalls ein Profil-Override. Ein versionloser Canonical verwendet die deklarierte `defaultVersion`. Nicht einzeln deklarierte Profil-URLs können über das Canonical-Basispräfix einer Familie aufgelöst werden.
4. Die FHIRPath-Datumsquelle wird ausgewertet; eine versionsspezifische Quelle hat Vorrang. Werte mit Zeitzone werden auf das Kalenderdatum in Europe/Berlin umgerechnet; Werte ohne Zeitzone behalten ihr Kalenderdatum. `validFrom` und `validTill` gelten inklusive. Bei Überlappung gewinnt der erste passende Eintrag in Deklarationsreihenfolge.
5. Ohne nutzbares Datum wird der einzige Zeitraum ohne `validTill` verwendet. Auswertungsfehler, mehrere Ergebnisse, Nicht-Datumswerte sowie unbrauchbare oder nur jahres-/monatsgenaue Werte führen ebenfalls zu diesem Rückfall. Ein leerer Locator bleibt ein Konfigurations-/Extraktionsfehler. Sind alle Zeiträume geschlossen, liefert die aktuelle Auswahl keine Modulpakete; fehlender Profilinhalt kann erst später einen Fehler auslösen. Dies ist eine [bekannte Grenze](../arc42/11-risks-and-technical-debt.md), keine erfolgreiche Gültigkeitsprüfung.
6. Ein nutzbares Datum außerhalb aller Zeiträume löst bei aktivierter Zeitraumprüfung `ProfileOutsideValidityPeriodException` aus. Ohne diese Prüfung wird für das Datum keine Modulpaketgruppe ausgewählt; eine passende Gruppe wird nicht erzwungen.
7. Jede Ressource wird mit eigenem Validator und Kontextklon geprüft. Nach den Meldungsregeln werden Ergebnisse berichtet. Ohne verbleibende ERROR-/FATAL-Meldungen ist das verarbeitete Ergebnis Valid. Initialisierungs-, Konfigurations- oder Auflösungsfehler sind davon getrennte Fehlerfälle.

Direkte Core-Validierung und Bibliotheksaufrufe mit selbst bereitgestellten Paketen unterliegen nicht automatisch dieser Modulauswahl. Ein Manifest zu laden importiert weder Archive noch konfiguriert es einen Validator. Bibliotheksnutzer müssen Laden, Archivimport, Auswahl, Kontextaufbau und Übernahme der Modulrichtlinien ausdrücklich zusammensetzen; siehe [Bibliotheksanleitung](../user-guide/library-user-guide.md).

Ergebnisregeln und Konfiguration
--------------------------------

Die Validierungsfähigkeit verarbeitet die übergebenen Optionen in dieser Reihenfolge:

1. Meldungen entfernen, die einer Unterdrückungsregel entsprechen.
2. Die erste passende Schweregradtransformation in Deklarationsreihenfolge anwenden.
3. Verbleibende Meldungen mit ignorierten CodeSystem-/ValueSet-Canonicals auf INFORMATION herabstufen.
4. Den angeforderten Schweregradfilter anwenden. Ohne verbleibende Meldungen wird eine informative Kein-Fehler-Meldung zurückgegeben.

Die Bibliotheksstandardoptionen behalten alle Meldungen, aktivieren die Zeitraumprüfung und enthalten keine Overrides oder zusätzlichen Meldungsregeln. Die CLI setzt für Modulvalidierung effektive Optionen zusammen. **Bekannte Einschränkung:** Sie fügt derzeit alle Transformationslisten des Manifests hinzu, statt sie auf die ausgewählte Paketgruppe einzugrenzen. Der Modulindex aggregiert seinerseits Transformationsnamen aller Gruppen einer Profilversion. Regeln dürfen deshalb nicht ungeprüft als ausschließlich datumsgruppenspezifisch angesehen werden; siehe [arc42, Kapitel 8](../arc42/08-cross-cutting-concepts.md) und [Kapitel 11](../arc42/11-risks-and-technical-debt.md).

Validierungs-CLI-Konfiguration, Snapshot-CLI-Konfiguration und Modulmanifest sind verschiedene YAML-Formate. Unbekannte Felder werden zurückgewiesen. Bei `--config` gilt die Datei anstelle der entsprechenden Kontext-/Modul-Flags, nicht als Zusammenführung mit ihnen. Relative Pfade beziehen sich auf das Prozessarbeitsverzeichnis. `--json` wählt JSON-Berichte, andernfalls wird HTML geschrieben; die YAML-Formatangabe allein entscheidet dies nicht. `--debug` aktiviert allgemeine Debug-Protokollierung, `--additional-debug` zusätzlich HL7-Protokollierung; `--verbose` erweitert die ausgegebenen Validierungsmeldungen.

Module und Snapshot-Generierung
--------------------------------

Ab Version 3.0 werden `erp`, `eau` und das experimentelle `erpta7` nicht mehr mit der CLI ausgeliefert. Ein externes Modul-JAR enthält `META-INF/config.yaml` mit Manifestformat `configSpecVersion: "3.0"` und vorbereitete `package/*.tgz`-Archive. Es beschreibt Profilfamilien, Versionen, Paketgruppen und Zeiträume, Terminologieausnahmen und Meldungsregeln.

Die CLI untersucht JARs direkt in `--modules-folder` und wählt mit `--module-name` den ersten gültigen Treffer anhand des Manifestnamens ohne Beachtung der Groß-/Kleinschreibung. Der Loader verlangt Manifest und SPI-Registrierung im Kandidaten-JAR selbst, führt aber keine Provider über `ServiceLoader` aus und importiert keine Pakete. Ein gültiges Verzeichnis ohne Treffer liefert ein leeres Ladeergebnis; die CLI meldet eine ausdrücklich angeforderte, aber fehlende Modulauswahl weiterhin als Fehler. Ungültige Verzeichnisse sind Ladefehler. Es gibt keine automatische Auswahl der höchsten Version, ausgehandelte Engine-Kompatibilität oder zugesicherten Hot-Reload. Pro Modulname soll nur die beabsichtigte Version im Verzeichnis liegen.

Modulherausgeber verwenden das externe [Template](https://github.com/gematik/app-referencevalidator-module-template) und einen zur CLI passenden `validation-module-parent`:

1. Manifest, Paketquellen bzw. Registry-Koordinaten, optionale Patches und positive Testressourcen definieren. Providerklasse und Service-Registrierung gehören zum Aufbau, auch wenn der Loader die Provider nicht ausführt.
2. Die verwalteten Exec-Ausführungen im Kindprojekt aktivieren. In `generate-resources` werden Abhängigkeiten aufgelöst, Patches angewendet und Snapshot-Archive erzeugt.
3. Manifest, Provider und generierte Archive in ein JAR packen. Quellarchive und Patches sind Bau-Eingaben, keine separat ausgelieferten Quellressourcen.
4. In `verify` positive JSON-/XML-Beispiele gegen das gebaute JAR prüfen. Die empfohlene Konfiguration deaktiviert Downloads und Terminologieserverzugriff. Ein nicht erfolgreicher CLI-Exitcode lässt den verwalteten Maven-Schritt fehlschlagen. `mvn package` allein führt diese Verifikation nicht aus.
5. Snapshot- und Validierungsberichte prüfen; erwartete Fehler, Zeitraumgrenzen und Meldungsregeln mit Assertion-Tests abdecken. Das JAR mit Profilumfang, geprüfter Validatorversion und Einschränkungen veröffentlichen.

Die Snapshot-Generierung löst den vollständigen Abhängigkeitsgraphen vor Beginn auf und weist fehlende Abhängigkeiten oder Zyklen zurück. Sie verarbeitet Pakete abhängigkeitsgerecht, erzeugt jedes Paket höchstens einmal pro Auftrag und veröffentlicht Archive erst nach erfolgreicher Kompression. Fehler blockieren abhängige, nicht unabhängige Pakete. Snapshots vervollständigen nicht automatisch die offline verfügbare Terminologie. Die [Modulentwicklungsanleitung](../user-guide/module-user-guide.md) beschreibt Aufbau und Freigabe im Detail.

Performancebetrachtungen
------------------

Antwortzeiten und Speicherbedarf hängen von Engine, Paketgruppen, Ressourcen, Terminologiezugriff, Cachezustand und Betriebsumgebung ab. Es gibt **keinen verifizierten Mindestheap** oder allgemeine Durchsatzgarantie. Die frühere Angabe von 512 MB ist keine aktuelle Dimensionierungsempfehlung.

Modul-Snapshots verlagern Vorbereitung in den Modulbau. Die CLI hält für einen Validierungsaufruf einen begrenzten Cache initialisierter Paketgruppen-Kontextvorlagen. Vorlagen dienen nur als Klonquelle; jede Ressource erhält einen eigenen Kontextklon und Validator. Die Kapazität entspricht höchstens der konfigurierten Anzahl gleichzeitiger Validierungen; aktive Leases schützen Vorlagen vor Verdrängung. Am Ende des Aufrufs werden die Vorlagen freigegeben. Ein prozessweit einmaliges Laden aller Pakete wird nicht zugesichert.

Mehrdatei-Validierung nutzt virtuelle Threads mit begrenztem Fenster laufender Aufgaben. Ressourcendaten werden erst in der jeweiligen Aufgabe größenbegrenzt gelesen. Die Eingabesammlung weist symbolische Links, nicht reguläre Dateien und überschrittene Grenzen zurück.

| Grenze | Standard | Harte Obergrenze |
| --- | --- | --- |
| Eingabedateien | 1.000 | 10.000 |
| Größe pro Datei | 50 MiB | 100 MiB |
| Gesamtgröße | 500 MiB | 1 GiB |
| Verzeichnistiefe | 32 | 64 |
| Gleichzeitig laufende Validierungen | 5 | 32 |

Diese Grenzen steuern angenommene Arbeit, nicht den maximalen Heapbedarf. HL7-Klone teilen ausgewählte interne Abhängigkeiten; Ergebnisparität und parallele Isolation sind versionsbezogen nachzuweisen. Das optionale [JMH-Benchmarkmodul](../../performance-benchmarks/README.md) vergleicht Kontextneuaufbau mit batchweiser Vorlagennutzung. Synthetische Messungen ersetzen keine Messungen mit realen Paketgruppen, Prozessspeicher und vollständigem CLI-Ablauf.

> **Warning**
> Eine einzelne FHIR-Datei mit 1 GB liegt außerhalb der CLI-Grenzen. Eine Aufteilung ist nur sinnvoll, wenn eigenständig valide Ressourcen entstehen und Bundle-Zusammenhänge sowie notwendige Referenzen erhalten bleiben. Grenzwerte und Parallelität sind innerhalb der harten Obergrenzen anhand der Betriebsumgebung zu dimensionieren.

Sicherheitsbetrachtungen
------------------

Der Referenzvalidator kann höchstvertrauliche Gesundheitsdaten verarbeiten. Lesender Zugriff auf Eingaberessourcen bedeutet **keine Zusicherung ohne Speicherung oder Netzwerkkommunikation**. HTML-/JSON-Berichte und Protokolle können Ressourceninhalte oder identifizierende Angaben enthalten. Caches, Snapshot-Ausgaben und temporäre Terminologieverzeichnisse benötigen Schreibzugriff und Aufbewahrungsregeln. Eine zeitnahe oder sichere Löschung aus dem JVM-Speicher durch den Garbage Collector wird nicht zugesichert.

| Bereich | Verhalten und Betreiberverantwortung |
| --- | --- |
| Paketdownloads bei `validate` | Standardmäßig deaktiviert; `--online-mode` erlaubt Downloads. Offlinebetrieb benötigt verfügbare lokale Pakete. Standardcache: `~/.fhir/packages`. |
| Paketdownloads bei `generate-snapshot` | Standardmäßig erlaubt; `--offline-mode` deaktiviert sie. Der persistente Cache `~/.fhir/snapshot-packages` einschließlich generierter Archive bleibt erhalten. |
| Terminologieserver | Unabhängig von Paketdownloads konfiguriert. Aktivierter Zugriff kann prüfungsrelevante Inhalte an den Server übermitteln; Serverauswahl, Netzfreigaben und Datenschutz sind betrieblich zu bewerten. |
| Temporärer Terminologiecache | Bibliothekseigene Caches werden nach Schließen des letzten mitnutzenden Kontexts entfernt. Explizite Cachepfade bleiben aufruferverwaltet und werden nicht automatisch gelöscht. |
| Berichte und Protokolle | Konsolenausgaben und HTML-/JSON-Dateien sind vertraulich zu behandeln. Pfade, Berechtigungen, Umleitungen, Aufbewahrung und Löschung sind festzulegen. Die Bibliothek nutzt SLF4J, die CLI Log4j2. |

Netzwerkfreier Betrieb verlangt deaktivierte Remote-Paketauflösung **und** deaktivierten Terminologieserverzugriff. Offlinebetrieb verhindert keine Prüfung lokal verfügbarer Codes; erfolgreiche Validierung garantiert keine vollständige Terminologieabdeckung.

Bei Bibliotheksintegration sind Validatoren, Kontexte, Batch-Provider/Leases und Snapshot-Generatoren gemäß ihren Besitzverhältnissen zu schließen. Aus Konfiguration erzeugte Validatoren schließen ihren Kontext; Validatoren um einen vorhandenen Kontext lassen diesen offen. Ein Ressourcenkontext wird vor seiner Lease geschlossen. Persistente Caches, Modul-JARs und Berichte bleiben betreiberverwaltet.

Die Archivverarbeitung weist unter anderem absolute Pfade, Pfadtraversierung, Links, doppelte bzw. widersprüchliche Einträge und überschrittene Größen-/Anzahlgrenzen zurück. Diese Schutzmaßnahmen ersetzen keine abgesicherte Betriebsumgebung.

Die Betriebsumgebung des Referenzvalidators ist für die Vertraulichkeit der Daten entscheidend. Für den sicheren Betrieb ist es erforderlich, die Übertragungswege der zu validierenden Daten zum Referenzvalidator sicher zu gestalten und vom unberechtigten Zugriff zu schützen. Die Vertraulichkeit, Integrität und Authentizität der zu validierenden Daten auf dem Weg zum Validator müssen durch den Betreiber durch geeignete technische und/oder organisatorische Maßnahmen sichergestellt werden. Das Gleiche gilt auch für die Entgegennahme bzw. Umleitung der Ausgaben des Referenzvalidators aus der Standardausgabe. Beim Betrieb des Validators neben Drittanwendungen muss die Authentizität und Integrität der Referenzvalidator-Anwendung (der JAR-Datei) sichergestellt werden. Unter anderem sollen die State-of-the-Art-Maßnahmen zum Schutz der Zielumgebung vor unberechtigtem Zugriff bzw. Kompromittierung ergriffen werden.   

Bei Einbettung sind die durch die BOM abgestimmten Abhängigkeitsversionen zu verwenden. Dies reduziert Versionsabweichungen, ist aber allein keine Korrektheits- oder Vertraulichkeitsgarantie. Authentizität und Integrität der separat ausgelieferten Modul-JARs müssen ebenso wie die der Validator-JARs sichergestellt werden.

> **Warning**
> Laut README ist der Referenzvalidator nicht für die Prüfung von KIM-Payloads in Produktivumgebungen vorgesehen. Betriebssicherheit und Datenschutz des Gesamtsystems bleiben Verantwortung des Betreibers.

Entwicklungsprozess
===================

Anforderungsanalyse
-------------------

Für die (Weiter-)Entwicklung eines Prüfmoduls werden Anforderungen und Nutzungsszenarien der jeweiligen TI-Anwendung ermittelt. Die beteiligten Akteure werden in Entscheidungen zu Profilen, Versionen, Gültigkeitszeiträumen, Paketen und Engine-Meldungsregeln eingebunden. Terminologieausnahmen und ihre Auswirkungen auf die Prüfabdeckung sind ausdrücklich festzuhalten.

Zu den Anforderungen gehört auch ein Satz an Testressourcen, die vom Referenzvalidator als gültig bzw. als nicht gültig validiert werden sollen.

Design & Implementierung
------------------------

Die Implementierung erfolgt anhand der identifizierten Anforderungen mit fachlicher Unterstützung der beteiligten Akteure. Produkt und Modul sind getrennte Entwicklungs- und Releaseeinheiten. Modulherausgeber verantworten Manifest, Pakete, Zeitregeln, Patches, Meldungsregeln, Beispiele und Einschränkungen; die Validatorentwicklung verantwortet Engineintegration, Bibliothek, CLI, API, BOM und Modul-Parent.

Signifikante Änderungen an Architektur, Engine, Paketauswahl, Kontextklonierung oder Ergebnisregeln, die Ausgaben oder Performance beeinflussen können, werden mit den beteiligten Akteuren abgestimmt.

Test
----
Die Prüfmodule des Referenzvalidators werden anhand der identifizierten Testdatensätze automatisiert getestet. Dabei sollten möglichst viele in den Prüfmodulen verwendeten Profile berücksichtigt und möglichst realitätsnahe Datensätze zum Einsatz kommen (sowohl valide als auch invalide). Die fachliche Zuarbeit der jeweiligen Standardisierungsorganisation ((siehe [Verantwortliche für FHIR-Profilierungs-Projekte im Kontext von E-Rezept](https://github.com/gematik/api-erp/blob/master/docs/erp_fhirversion.adoc))) bei der Erarbeitung der Testfälle ist erforderlich. Je mehr repräsentative Datensätze in dieser Phase zum Einsatz kommen, umso besser können die Ausgaben von neuen Versionen des Referenzvalidators vor Veröffentlichung verifiziert und ggf. angepasst werden.

Im Rahmen der Weiterentwicklung kommen auch Testfälle aus Eskalationsfällen und bekannten aufgetretenen Fehlern in die Testfalldatenbasis hinzu. Testfälle können auch über GitHub jederzeit beigetragen werden.

Zusätzliche Prüfschwerpunkte für Version 3.0:

* Positive Beispiele gegen das **gebaute Modul-JAR** ohne Paketdownloads und Terminologieserverzugriff prüfen; Snapshot- und Validierungsbericht gemeinsam auswerten. Erwartet invalide Fälle benötigen eigene Assertion-Tests statt Ablage im positiven Testverzeichnis.
* Zeitraumgrenzen, fehlende/ungültige Datumswerte, Profil-Overrides, Defaultversionen und JSON-/XML-Verhalten prüfen. Transformationen und Unterdrückungen mit beabsichtigten Treffern und ähnlich aussehenden Nichttreffern testen.
* Ältere und aktuelle Instanzen bei Validator- oder Modulwechsel erneut auswerten. Die [Migrationsfälle R1–R5](../Regressions.md) betreffen nichtkonforme FHIRPath-Stringliterale mit doppelten Anführungszeichen, unbekannte CodeSystems, Bundle-Referenzen, unversionierte Terminologien und Kindressourcen.
* Für erforderliche Prüfungen eingebetteter Ressourcen `recursiveModePolicy` aktivieren und testen. Dies ist unabhängig von rekursiver Dateisuche und der ersten Kindressource zur Modulauswahl.
* Bei Engine-/Klonpfadänderungen Ergebnisparität, parallele Isolation und reale Implementation-Guide-Pakete prüfen. JUnit, ArchUnit, JaCoCo, Spotless und Buildprüfungen unterstützen die Qualitätssicherung, ersetzen aber keine fachliche Freigabe.

Diese Dokumentaktualisierung behauptet keinen neuen erfolgreichen Test- oder Benchmarklauf. Bekannte technische Grenzen bleiben in [arc42, Kapitel 11](../arc42/11-risks-and-technical-debt.md) dokumentiert.

Veröffentlichung
----------------

Das bisherige fachliche Schema **MAJOR.MINOR** für unabhängig versionierte Module bleibt eine Governance-Konvention: Neue Profile oder ValueSets ohne Verhaltensänderung für ältere Instanzen erhöhen MINOR; Verhaltensänderungen bei älteren Instanzen erfordern MAJOR. Technisch ist Manifest-`version` eine vom Herausgeber festgelegte Zeichenfolge; der Loader erzwingt die Konvention nicht. Modulrelease, FHIR-Paket-/Profilversionen und Manifestformat `configSpecVersion` sind getrennte Identitäten.

Der Referenzvalidator wird nach [SemVer](https://semver.org/lang/de/) versioniert und als CLI sowie Maven-Artefakte **ohne anwendungsspezifische Module** veröffentlicht. Modulherausgeber veröffentlichen ihre JARs separat mit geprüfter Validator-/Modulkombination, Profilumfang, Terminologieausnahmen und Einschränkungen. Änderungen werden explizit kommuniziert. Ein neues Template aktualisiert bestehende Modulprojekte nicht automatisch; konkrete kompatible Parent-/CLI-/API-/BOM-Versionen sind auszuwählen und zu testen.

Ein unabhängiges Modulrelease ist nicht automatisch enginekompatibel. Betreiber übernehmen neue Versionen erst nach Regressionstests; automatische Laufzeitupdates sind nicht vorgesehen. Für reproduzierbare Entscheidungen und Rollback sind exakte JARs, effektive Konfiguration und Pakete samt Herkunft gemäß geltenden Aufbewahrungsregeln zu erhalten. Ein späterer Neubau aus veränderlichen Registries ersetzt die geprüften Artefakte nicht.

Die Prozess-Anforderungen aus den jeweiligen TI-Anwendungen (z.B. [E-Rezept](#e-rezept-anforderungen-an-den-entwicklungsprozess)) müssen beachtet werden. So wird für eine neue Version des Prüfmoduls E-Rezept eine Integrationstestphase eingeplant, bevor die neue Version die alte in ihrer Rolle des [Schiedsrichters](#e-rezept-technische-verarbeitbarkeit-schiedsrichter-rolle-und-problemlösungsverfahren) ablösen kann.


Change-Management / Problemlösung
---------------------------------

Änderungen an dem Referenzvalidator und an den Prüfmodulen können aus mehreren Gründen erforderlich werden:

1.  Veröffentlichung von neuen Profildefinitionen, die in einem oder mehreren Prüfmodulen verwendet werden.
2.  Entdeckte Fehler im Referenzvalidator bzw. in einem Prüfmodul, die z.B. zu falschen Ausgaben führen (false-positive und false-negative).
3.  Neue Anforderungen, die sich aus alten oder neuen Nutzungsszenarien ergeben (Schnittstelle des Referenzvalidators, Konfigurierbarkeit etc.)
4.  Interne Optimierungen (Technologieaktualisierungen einschließlich Upgrades des HL7 Core Validators, Verbesserungen hinsichtlich Performance oder Wartbarkeit)

Änderungswünsche, Anmerkungen und Problem-Meldungen können über die [GitHub-Seite des Projektes](https://github.com/gematik/app-referencevalidator) bzw. der dort angegebenen Kontaktmöglichkeiten eingereicht werden.

Die Standardisierungsorganisationen informieren die gematik über anstehende Veröffentlichungen. Die fachliche Prozessanforderung bleibt, neue Profile spätestens drei Monate vor ihrer verpflichtenden Einführung im jeweiligen Modul zum Testen bereitzustellen. Im aktuellen Modell wird dazu ein Modulrelease erstellt; ein Produktrelease ist nur bei notwendigen Validatoränderungen erforderlich. Herausgeber, Kompatibilitätsprüfung und Integrationstestphase sind abzustimmen. Die Frist ist keine automatisch überwachte Laufzeitgarantie.

Die entdeckten und gemeldeten Fehler werden mit dem jeweiligen Steuergremium bewertet und das korrekte Verhalten wird spezifiziert. Es wird entschieden, ob z.B. Korrektur des Prüfmoduls und/oder des Profils erforderlich ist. Dabei wird der Entwicklungsprozess erneut durchlaufen.

E-Rezept: Technische Verarbeitbarkeit, Schiedsrichter-Rolle und Problemlösungsverfahren
----------------------

Im Rahmen von E-Rezept spielen die Begriffe der "technischen Verarbeitbarkeit" / "Validität" und des "Schiedsrichters" eine wichtige Rolle. Eine FHIR-Ressource gilt als "technischen verarbeitbar" bzw. "valide" im Kontext von E-Rezept, falls 
1. sie konform zu einem im Kontext von E-Rezept eingesetzten FHIR-Profil ist und
2. das referenzierte FHIR-Profil zum Erstellungszeitpunkt der Ressource gültig war

Automatisierte Verfahren benötigen eine einheitliche Umsetzung dieser Regeln. Die Schiedsrichter-Rolle bezieht sich auf das für das Verfahren festgelegte E-Rezept-Modul zusammen mit der vereinbarten Validatorversion und Konfiguration. Die verbindliche Verwendung in Abrechnungsprozessen richtet sich nach der [Technischen Anlage 7, Anhang 2, zur Arzneimittelabrechnungsvereinbarung nach § 300 Absatz 3 SGB V](https://www.gkv-datenaustausch.de/leistungserbringer/apotheken/apotheken.jsp). Installation der Version 3.0 oder eines neuen Modul-JARs allein begründet keine fachliche Freigabe oder Ablösung einer bisherigen Schiedsrichterversion. Für andere Module ist laut README keine solche Verbindlichkeit festgelegt.

Normative Grundlage bleiben die Spezifikationen; im Zweifelsfall muss ein Mensch die Konformität bewerten. Ein Valid-Ergebnis nach Unterdrückungen oder Herabstufungen beweist nicht, dass sämtliche Prüfungen erfolgreich durchgeführt wurden. Abweichungen und Einschränkungen müssen im geregelten Problemlösungsverfahren bewertet werden.

Falls es Hinweise gibt, dass die Bewertung des Schiedsrichters für bestimmte FHIR-Ressourcen oder sogar Ressourcenklassen fehlerhaft ist, dann soll wie folgt vorgegangen werden:

<ol>
    <li>Der "Schiedsstelle" des Prüfmoduls E-Rezepts werden die Hinweise auf ein potentiell fehlerhaftes Verhalten zur Verfügung gestellt:
        <ol>
            <li>Über den <a href="https://service.gematik.de/servicedesk/customer/portal/11" target="_parent">TI-Servicedesk (ERPFIND-Ticket)</a></li>
            <li>Über ein <a href="https://github.com/gematik/app-referencevalidator/issues" target="_parent">GitHub-Issue des Referenzvalidator-Projektes</a></li>
        </ol>
    Die Schiedsstelle besteht aus den Vertretern der am E-Rezept beteiligten Sektoren, der Industrie und der gematik.
    </li>
    <li>Die Schiedsstelle prüft die Hinweise und entscheidet über den Umgang mit eventuell potentiell fehlerhaften Verhalten. Dabei können ggf. Profilkorrekturen oder Anpassungen des "Schiedsrichters" (neue Versionen des Referenzvalidators) als mögliche Maßnahmen sein.   
    </li>
    <li>Eventuelle Änderungen und an dem Prüfmodul bzw. dem Referenzvalidator und deren Zeitplan werden an die Melder und Nutzer des Prüfmoduls bekanntgegeben. 
    </li>
</ol>

> **Note** Bedeutung der "Verbindlichkeit" des Referenzvalidators im Kontext von E-Rezept
> * Der Referenzvalidator stellt ein qualitatives und daher empfohlenes Unterstützungswerkzeug dar - autoritativ und bindend sind für die Umsetzung von E-Rezept allerdings weiterhin NUR die jeweiligen Spezifikationen. Des Weiteren MUSS die endgültige Validität einer Instanz zu einer Spezifikation im Zweifelsfall weiterhin durch einen Menschen bestätigt werden.
> * Alle IT-Systeme, die mit E-Rezept-Spezifikationen arbeiten, SOLLEN den Referenzvalidator im Rahmen der eigenverantwortlichen Qualitätssicherung nutzen.
> * Die spezifizierenden Organisationen SOLLEN den Referenzvalidator zur Qualitätssicherung ihrer Spezifikationen, bspw. durch Validierung der (eigenen) Beispielinstanzen, nutzen
> * Die Abweichungen im Validierungsverhalten zwischen Fachdienst und Referenzvalidator MÜSSEN durch geeignete technische und organisatorische Maßnahmen minimiert werden, bspw. indem für einen repräsentativen Satz an Spezifikation-konformen und nicht konformen Testressourcen die Validierungsergebnisse vom Fachdienst und vom Referenzvalidator kontinuierlich überprüft werden. Entdeckte Abweichungen, die auf fehlerhafte Implementierung im Fachdienst zurückzuführen sind, durchlaufen den bereits existierenden Problemlösungsprozess im E-Rezept.
> * Bestätigungssysteme wie Titus MÜSSEN den Referenzvalidator einsetzen um die Implementierung der zu testenden Systemen zu testen

Abgrenzung zu anderen Projekten
===============================

ABDA Referenzvalidator
----------------------

Unter [https://github.com/DAV-ABDA/eRezept-Referenzvalidator/](https://github.com/DAV-ABDA/eRezept-Referenzvalidator/) wird der ABDA E-Rezept-Referenzvalidator entwickelt, der vor dem gematik Referenzvalidator den verbindlichen Status einer Schiedsrichterinstanz für die Apotheken und Krankenkassen hatte. Teile des Referenzvalidators sind in das Prüfmodul E-Rezept des gematik Referenzvalidators eingeflossen. Die Ablösung des ABDA E-Rezept-Referenzvalidators in seiner Funktion durch den gematik Referenzvalidators wird in der [Technische Anlage 7, Anhang 2, zur Arzneimittelabrechnungsvereinbarung gemäß § 300 Absatz 3 SGB V](https://www.gkv-datenaustausch.de/leistungserbringer/apotheken/apotheken.jsp) festgelegt.

Neben E-Rezept-Prüfmodul unterstützt der Referenzvalidator noch weitere Anwendungen. 

HL7 FHIR Validator
--------------

Der [HL7 FHIR-Validator](https://confluence.hl7.org/display/FHIR/Using+the+FHIR+Validator) liefert die zugrundeliegenden Konformitätsprüfungen. Ab Version 3.0 verwendet der gematik Referenzvalidator dessen HL7 Core Validation Library direkt statt der früheren HAPI-Validierungsengine. Darüber liegen Modul-Paketauswahl, Zeiträume, Kontext-/Terminologierichtlinien, Meldungsregeln und Berichte. Prüftiefe und Laufzeit hängen weiterhin von Engine und verfügbaren Paketen ab. Identische Ergebnisse zu einer separat konfigurierten HL7-CLI werden nicht pauschal zugesichert.

E-Rezept-Fachdienstvalidator
--------------

Der E-Rezept-Fachdienst enthält eine eigene Validierungskomponente für Verordnungs- und Abgabedatensätze. Der gematik Referenzvalidator ist eine eigenständige Java-Bibliothek bzw. CLI, kein automatisch eingebauter Bestandteil des Fachdienstes. Ergebnisse sind mit repräsentativen konformen und nicht konformen Ressourcen zu vergleichen und Abweichungen über das zuständige Verfahren zu bewerten. Der Enginewechsel begründet weder eine Integration in den Fachdienst noch eine nachgewiesene Ergebnisgleichheit.
