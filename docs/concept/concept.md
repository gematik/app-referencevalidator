![gematik GmbH](../img/Gematik_Logo_Flag.png)

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

*   Die FHIR-Ressourcen sind konform zu den referenzierten Profilen bzw. Profilversionen (im Sinne der einheitlich konfigurierter HAPI-Validierung einschließlich Interpretation der Meldungen)
*   Die referenzierten Profile bzw. Profilversionen sind zum Zeitpunkt der Erstellung der Ressource gültig und wurden nicht durch neuere Profile bzw. Profilversionen abgelöst.

Die wichtigsten Verarbeitungskomponenten des Referenzvalidators werden **Prüfmodule** sein. Diese kapseln alle Daten, die für die Validierung von FHIR-Instanzen im Rahmen einer TI-Anwendung erforderlich sind (verwendete Profile, Profilversionen, Pakete, Konfiguration der HAPI-Ausgabeverarbeitung etc.).

Nutzungsszenarien E-Rezept-Prüfmodul
====================================

Entwicklungsunterstützung
-------------------------

Ein PVS\-Hersteller möchte bereits während der Implementierung des E-Rezept-Moduls die Validität von seinen zu Testzwecken erzeugten VO-Datensätzen überprüfen. Zu diesem Zweck verwendet er den Referenzvalidator als Standalone Konsolenanwendung und prüft die aus seinem System erzeugten Dateien auf Korrektheit.

Schiedsrichter-Szenario
-----------------------

Ein empfangendes System A weist einen E-Rezept-Datensatz aufgrund der fehlenden Konformität zu gültigen FHIR-Profilen ab. Das sendende System B nimmt bei der Problemanalyse den Referenzvalidator um festzustellen, ob die gesendete Ressource tatsächlich invalide ist. Der Datensatz wird als Datei über die Kommandozeile an den Referenzvalidator zur Prüfung übergeben. Je nach dem "Urteil" des Referenzvalidators, wird entweder die Implementierung eines im Prozess beteiligten Systems korrigiert um zukünftig valide Datensätze zu erzeugen, oder die Abweisung des Datensatzes angefochten.

Bestätigungsverfahren der gematik
---------------------------------

Im Rahmen der Bestätigungsverfahren der gematik können sich die Primärsysteme einer Reihe von Tests unterziehen, die die grundsätzliche Kompatibilität ihrer Umsetzung zu den Spezifikation der TI-Anwendung für Client-Systeme überprüfen. Der Referenzvalidator wird in die Bestätigungsplattform integriert, um alle eingehenden FHIR-Ressourcen auf ihre technische Validität im Sinne der jeweiligen TI-Anwendung zu überprüfen. Mit diesen Überprüfungen wird sichergestellt, dass die Primärsysteme zumindest im Rahmen der Bestätigungstests im Stande waren technisch valide Datensätze zu erzeugen.

Weiterentwicklung des Referenzvalidators zum produktiven Einsatz
----------------------------------------------------------------

Ein Apothekenrechenzentrum muss wissen, ob ein E-Rezept den technischen Verarbeitungsanforderungen entspricht, um entscheiden zu können, ob das Rezept zur Weiterverabeitung / Weiterleitung an Krankenkassen-Annahmestellen angenommen, abgewiesen oder korrigiert werden soll. Für diesen Zweck nimmt der Softwarehersteller den Referenzvalidator als Ausgangsbasis und entwickelt diesen zu einem produktiv einsetzbaren System weiter, d.h. setzt die in seinem Fall zusätzlich geltenden Anforderungen an die Sicherheit, Datenschutz und Performance um. Des Weiteren, wird der Referenzvalidator um eine REST-Schnittstelle erweitert, die es ermöglicht den Referenzvalidator in eine bestehende Software einzubinden.


> **Warning**
> Der Referenzvalidator darf in der Produktion ohne vorherige Sicherheit- und Performancebetrachtungen nicht eingesetzt werden. Die gematik übernimmt keine Verantwortung für Schäden, die durch den Einsatz vom Referenzvalidator in Produktivsystemen entstehen können.

> **Note**
> Unter anderem aufgrund der Performance-Eigenschaften wird der Referenzvalidator auch nicht zur zentralen Validierung im E-Rezept-Fachdienst eingesetzt.

Weiterentwicklung der Profile
-----------------------------

Eine Organisation, die mit Profilierung von E-Rezept-Ressourcen beauftragt ist, entwickelt die Profildefinitionen kontinuierlich weiter. Zur Prüfung der Validität der neuen Profile, der neuen Beispieldatensätze und der Konformität der alten Datensätze zu den neuen Profilen, wird der Referenzvalidator hinzugezogen. Dieser wird entweder manuell auf neuen Profilen und Beispieldatensätzen aufgerufen oder in die CI-Pipelines integriert.

E-Rezept: Nutzung- und Stakeholderanforderungen an den Referenzvalidator
========================================================================

<table class="wrapped confluenceTable" style="letter-spacing: 0.0px;">
    <colgroup>
        <col style="width: 29.0px;">
        <col style="width: 430.0px;">
        <col style="width: 337.0px;">
        <col style="width: 344.0px;">
        <col style="width: 298.0px;">
    </colgroup>
    <tbody>
    <tr>
        <th class="confluenceTh" colspan="1"><br></th>
        <th class="confluenceTh">Erfordernisse</th>
        <th class="confluenceTh">Nutzung- und Stakeholderanforderungen</th>
        <th class="confluenceTh" colspan="1">Systemanforderungen</th>
        <th class="confluenceTh" colspan="1">Projekt bzw. Prozessanforderungen</th>
    </tr>
    <tr>
        <td class="confluenceTd" colspan="1">1</td>
        <td class="confluenceTd"><p>Ein<strong> Entwickler vom PVS-, AVS-, Apothekenrechenzentrum-,
            Annahmestellen-Software, E-Rezept-App</strong> muss ein Werkzeug verfügbar haben, mit dem FHIR-Ressourcen,
            die bei E-Rezepten zum Einsatz kommen, gegen gültige FHIR-Profile/Profilversionen validiert werden können
            (aktuell gültige oder demnächst gültige). Damit soll die Qualitätssicherung der Anpassungen an
            E-Rezept-Modulen unterstützt werden.</p></td>
        <td class="confluenceTd"><p>1.1. Der Entwickler muss bei dem Referenzvalidator die zu validierende
            FHIR-Ressource aus der TI-Anwendung E-Rezept eingeben können.</p>
            <p>1.2. Der Entwickler muss an dem Referenzvalidator das binäre Ergebnis der Validierung erkennen können.
                <span class="inline-comment-marker" data-ref="e97e0ffd-bf6a-432e-9d47-6e3641524243">Falls das Ergebnis der Validierung negativ sein sollte, muss der Entwickler an den Ausgaben des Referenzvalidators die erkannten Verletzungen der FHIR-Profile eindeutig erkennen können.</span>
            </p>
            <p>1.3 Der Benutzer muss bei dem Referenzvalidator eine zu validierende FHIR-Ressource eingeben können, die
                FHIR-Profilversionen referenziert, die zwar schon veröffentlicht wurden, die aber noch nicht gültig
                sind.</p></td>
        <td class="confluenceTd" colspan="1">
            <div class="content-wrapper"><p>1.1.1. Der Referenzvalidator muss E-Rezept-FHIR-Ressourcen
                (<strong>Bundles</strong>) als Eingabe akzeptieren können. <span
                        class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span></p>
                <p>1.1.2. Der Referenzvalidator muss E-Rezept-FHIR-Ressourcen (<strong>Bundle-Bestandteile</strong>) als
                    Eingabe akzeptieren können. <span class="status-macro aui-lozenge"><i>(Backlog)</i></span></p>
                <p>1.2.1. <span class="inline-comment-marker" data-ref="c7803a56-22d4-4157-8b57-bc567a20f468">Der Referenzvalidator muss Zugriff auf die aktuell gültigen FHIR-Profile, Profilversionen und Pakete haben und diese mit Hilfe vom HAPI-Referenzvalidator in einer festgelegten Version anwenden können (siehe <a
                        class="external-link"
                        href="https://github.com/gematik/api-erp/blob/master/docs/erp_fhirversion.adoc" rel="nofollow" target="_blank">Profil-Roadmap</a>).</span>
                    <span class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span></p>
                <p><span class="inline-comment-marker" data-ref="89f68b58-571c-4666-82ce-a18f04efd976">1.2.2. Das E-Rezept muss als Ergebnis eine Valid / Invalid-Antwort und als Ergänzung die HAPI-Ausgaben bzw. weitere erkannte Konformitätsprobleme ausgeben können. <span
                        class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span></span></p>
                <p>1.2.3. Um die Valid / Invalid-Antwort zu produzieren, muss der Referenzvalidator die Vorgaben der
                    TK300 TA7 Anhang 3 bzgl. der HAPI-Konfiguration und der Interpretationsregeln von HAPI-Ausgaben
                    (Fehler, Warnungen, Informationen) anwenden. Zukünftige Änderungen werden mit dem Steuergremium des
                    Prüfmoduls E-Rezept abgestimmt. <span
                            class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span></p>
                <p><span class="inline-comment-marker" data-ref="e0cb712c-cdbd-4bf7-935d-97e8009a54e2">1.2.4. <span
                        class="inline-comment-marker" data-ref="c683ccbe-4d1b-4212-bd4f-341a6b4c11aa">Falls es für den Zeitpunkt</span> mehrere gültige FHIR-Profilversionen gibt ("Übergangsphase"), dann ist das Ergebnis Valide, falls die FHIR-Ressource eine der gültigen Profilversionen referenziert.</span>
                    <span class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span></p>
                <p>1.3.1. Der <span class="inline-comment-marker" data-ref="e3ed44f8-d0b8-4245-9e66-67855c964ab1">Referenzvalidator</span>
                    muss Zugriff auf die bereits veröffentlichten aber potenziell noch nicht verpflichtend geltenden
                    FHIR-Profile/Profilversionen haben (siehe <a class="external-link"
                                                                 href="https://github.com/gematik/api-erp/blob/master/docs/erp_fhirversion.adoc"
                                                                 rel="nofollow" target="_blank">Profil-Roadmap</a>) <span
                            class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span></p>
                <p>1.3.2. Als <span class="inline-comment-marker" data-ref="212a1d0c-bf8e-4d4e-adac-7b992f2586df">Referenzzeitpunkt</span>
                    zur Gültigkeitsprüfung von referenzierten Profilversionen sollen die Erstellung-Datumsangaben aus
                    der FHIR-Ressource dienen:</p><span class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span>
                <ul style="text-align: left;">
                    <li>Verordnung, Datum der Ausstellung (KBV_PR_ERP_Prescription: MedicationRequest.authoredOn)</li>
                    <li>MedicationDispense, Abgabedatum (GEM_ERP_PR_MedicationDispense bzw. <span
                            class="inline-comment-marker" data-ref="1dbb0343-8dea-4739-b2da-2d6902da7e36">Gem_erxMedicationDispense)</span>:
                        MedicationDispense.whenHandedOver)
                    </li>
                    <li>Quittung, Ausstellungsdatum (GEM_ERP_PR_Composition bzw. Gem_erxComposition: Composition.date)
                    </li>
                    <li>Abgabedaten, Abgabedatum (DAV_PR_ERP_Abgabeinformationen: MedicationDispense.whenHandedOver)
                    </li>
                    <li>Abrechnungsdaten, Abrechnungsmonat (GKVSV_PR_TA7_Sammelrechnung_Composition: Composition.date)
                    </li>
                </ul>
                <p>1.3.3. Der Referenzvalidator kann als Eingabe einen Referenzzeitpunkt akzeptieren, der anstatt der in
                    der Nachricht angegebenen Datumsangabe, zur Bestimmung der zum Zeitpunkt gültigen FHIR-Profilversion
                    verwendet werden soll.</p>
                <p><span class="status-macro aui-lozenge"><i>(Backlog)</i></span></p>
                <p>1.3.4. <span class="inline-comment-marker" data-ref="1efc68b6-a652-4783-bf78-dc8e41a78b04">Der Referenzvalidator muss als</span>
                    Eingabe auch FHIR-Ressourcen akzeptieren, die keine explizite Profilangabe aufweisen (E-Rezept
                    Parameters) <span class="status-macro aui-lozenge"><i>(Backlog)</i></span></p>
                <p>1.3.5. <span class="inline-comment-marker" data-ref="59c43bfb-e103-4771-a517-e17cba51bbdc">Der Referenzvalidator muss als</span>
                    Eingabe auch FHIR-Ressourcen akzeptieren, die als JSON vorliegen (E-Rezept App-Requests) <span
                            class="status-macro aui-lozenge"><i>(Backlog)</i></span></p></div>
        </td>
        <td class="confluenceTd" colspan="1"><br></td>
    </tr>
    <tr>
        <td class="confluenceTd" colspan="1">2</td>
        <td class="confluenceTd" colspan="1"><p>Ein <strong>Apothekenrechenzentrum </strong>oder eine <strong>KK-Annahmestelle</strong>
            <span class="inline-comment-marker" data-ref="1790e8c5-fe7b-41f7-8d33-1280810836ac">müssen im Konfliktfall wissen, ob ein vor maximal 2 Jahren erstellter</span>
            und als technisch invalider bewerteter und abgewiesener E-Rezept-Datensatz den zum Erstellungszeitpunkt
            geltenden FHIR-Profilversionen entsprach, um die Abweisung ggf. neu bewerten zu können.</p></td>
        <td class="confluenceTd" colspan="1"><p>2.1 Der Benutzer muss bei dem Referenzvalidator die zu validierende
            E-Rezept-FHIR-Ressource, die maximal vor 2 Jahren erstellt wurde, eingeben können.</p>
            <p><br></p></td>
        <td class="confluenceTd" colspan="1">
            <div class="content-wrapper"><p>2.1.1. Der Referenzvalidator muss FHIR-Ressourcen, deren Ausstellungsdatum
                <span class="inline-comment-marker" data-ref="fbf6bcc6-f616-477e-8045-78e5a60af619">2 Jahre zurück liegen kann</span>,
                als Eingabe akzeptieren können. <span class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span>
            </p>
                <p>2.1.2. Der Referenzvalidator muss Zugriff auf die FHIR-Profile, Profilversionen, Pakete der letzten 2
                    Jahre haben <span class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span>
                </p>
                <p><br></p></div>
        </td>
        <td class="confluenceTd" colspan="1"><br></td>
    </tr>
    <tr>
        <td class="confluenceTd" colspan="1">3</td>
        <td class="confluenceTd" colspan="1"><p>Ein <strong>Apotheken-Rechen-Zentrum </strong>oder eine <strong>KK-Annahmestelle</strong>
            muss einen Referenzvalidator verfügbar haben, um Validierungsdienste für den Produktionsbetrieb bauen und in
            eigene Prozesse einbinden zu können.</p></td>
        <td class="confluenceTd" colspan="1"><p><br></p></td>
        <td class="confluenceTd" colspan="1">
            <div class="content-wrapper"><p>3.1.1 Der Referenzvalidator muss als Open Source bereitgestellt werden.
                <span class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span></p>
                <p>3.1.2. Der Referenzvalidator muss als Java 11-Bibliothek und als Konsolenanwendung bereitgestellt
                    werden, damit ein Umbau zum produktionsreifen Dienst bzw. Integration in andere Anwendungen möglich
                    wird. <span class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span></p>
                <p>3.1.3 Der Referenzvalidator muss Threadsafe sein <span
                        class="status-macro aui-lozenge"><i>(Backlog)</i></span></p></div>
        </td>
        <td class="confluenceTd" colspan="1"><br></td>
    </tr>
    </tbody>
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
        <td class="confluenceTd"><strong>Eine Krankenkasse, die gematik , ein Apothekenrechenzentrum oder eine
            Apotheke</strong> müssen im Falle der entdeckten Verarbeitungsprobleme von E-Rezepten einen Prozess zur
            Änderung der Prüflogik des Referenzvalidators starten können
        </td>
    </tr>
    <tr>
        <td class="confluenceTd">3</td>
        <td class="confluenceTd">Ein<strong> Entwickler vom PVS-, AVS-, Apothekenrechenzentrum-,
            Annahmestellen-Software </strong>muss spätestens 3 Monate vor dem Gültigkeitsbeginn der neuen
            Profilversionen den aktualisierten Referenzvalidator verfügbar haben, mit dem Anpassungen im E-Rezept-Modul
            und Änderungen an den FHIR-Ressourcen getestet werden können &nbsp;
        </td>
    </tr>
    <tr>
        <td class="confluenceTd"><br></td>
        <td class="confluenceTd"><br></td>
    </tr>
    </tbody>
</table>

Referenzvalidator: Infrastrukturelle Anforderungen:
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
            <ol>
                <li>Der Entwickler muss am Referenzvalidator folgendes eingeben können:
                    <ol>
                        <li class="auto-cursor-target">Eine zu validierende FHIR-Ressource aus dem Kontext einer
                            TI-Anwendung
                        </li>
                        <li class="auto-cursor-target">Ein zu verwendendes Prüfmodul (E-Rezept, eAU etc.)</li>
                    </ol>
                </li>
            </ol>
        </td>
        <td class="confluenceTd">
            <div class="content-wrapper">
                <ol>
                    <li>Der Referenzvalidator muss als Einhabe folgendes akzeptieren:
                        <ol>
                            <li class="auto-cursor-target">Eine zu validierende FHIR-Ressource aus dem Kontext einer
                                TI-Anwendung
                            </li>
                            <li class="auto-cursor-target">Ein zu verwendendes Prüfmodul (E-Rezept, eAU etc.)<br><span
                                    class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span></li>
                        </ol>
                    </li>
                    <li>Der Referenzvalidator muss Prüfmodule für unterschiedliche TI-Anwendungen intern verwalten. Ein
                        Prüfmodul besteht dabei aus
                        <ol>
                            <li>Profilen, Profilversionen, Paketen und Gültigkeitsräumen, die für eine Validierung
                                erforderlich sind
                            </li>
                            <li>(optional) Paketspezifische Interpretationsregeln der Ausgaben vom
                                HAPI-Validator<br><span class="status-macro aui-lozenge aui-lozenge-success"><i>(R1.0)</i></span>
                            </li>
                        </ol>
                    </li>
                </ol>
                <p><br></p></div>
        </td>
    </tr>
    </tbody>
</table>

Architekturskizze
=================

Der Referenzvalidator verwaltet TI-anwendungsspezifische Prüfmodule, die bei Aktivierung zur Validierung der übergebenen Ressource hinzugezogen werden. Die Prüfmodule enthalten Pakete, Gültigkeitszeiträume der verwendeten Profile sowie Profilpaket-spezifische Interpretationsregeln der HAPI-Validator-Ausgaben.

![image info](../img/architecture.png)

Entwicklungsprozess
===================

Anforderungsanalyse
-------------------

Für die (Weiter-)Entwicklung eines neuen Prüfmoduls werden die Anforderungen und entsprechende Nutzungsszenarien aus der Sicht der jeweiligen TI-Anwendung ermittelt (siehe Beispiel oben für das Prüfmodul E-Rezept). Für diesen Zweck werden Interviews mit den an dem Prozess beteiligten Parteien geführt und ein Steuerungsgremium bestehend aus entscheidungsbefugten Vertretern der wichtigsten Stakeholder installiert.

Im Rahmen der Anforderungen werden unter anderem die in der TI-Anwendung verwendeten Profile, Profilversionen, Gültigkeitszeiträume der Profilversionen, Pakete identifiziert, die für die Validierung der FHIR-Ressourcen benötigt werden. Im Rahmen der Anforderungen können paketversionspezifische Regeln zur Interpretation von HAPI-Server-Ausgaben (Warnungen, Informationen etc.) ermittelt werden.

Zu den Anforderungen gehört auch ein Satz an Testressourcen, die vom Referenzvalidator als gültig bzw. als nicht gültig validiert werden sollen.

Die erfassten Anforderungen werden dokumentiert und von dem
bSteuerungsgremium freigegeben.

Für die Kerninfrastruktur des Referenzvalidator werden weitere Anforderungen, die z.B. Verwaltung & Selektion von Prüfmodulen betreffen, gepflegt.

Design & Implementierung
------------------------

Die Implementierung vom Referenzvalidator sowie dem entsprechenden Prüfmodul findet anhand der identifizierten Anforderungen und mit Unterstützung der bereitgestellten Testdatensätzen statt.

Zwecks Robustheit und zur Unterstüzung von unabhängigen Release- / Feldeinführungszyklen von Prüfmodulen soll die Architektur des Referenzvalidators hohe Modularität / geringe Kopplung der Prüfmodule aufweisen.

Signifikante architekturelle Änderungen, die die Ausgaben oder die Performance von Prüfmodulen beeinflussen können, werden mit den Steuerungsgremien (insbesondere mit Hinblick auf die Releaseplanung) abgestimmt.

Test
----

Die Prüfmodule des Referenzvalidators werden anhand der identifizierten Testdatensätze automatisiert getestet. Dabei sollten möglichst viele in den Prüfmodulen verwendeten Profile berücksichtigt und möglichst realitätsnahe Datensätze zum Einsatz kommen (sowohl valide als auch invalide). Je mehr repräsentative Datensätze in dieser Phase zum Einsatz kommen, umso besser können die Ausgaben von neuen Versionen des Referenzvalidators vor Veröffentlichung verifiziert und ggf. angepasst werden. 

Veröffentlichung
----------------

Prüfmodule und der TI-Validator werden unabhängig voneinander nach dem [SemVar](https://semver.org/lang/de/)-Schema versioniert. Veröffentlicht wird nur der Referenzvalidator samt aller Prüfmodule. Die enthaltenen Prüfmodulversionen werden bei der Veröffentlichung explizit bekanntgegeben. 

Der Zeitpunkt, ab wann eine bestimmte Version vom Prüfmodul verbindlich wird, soll durch das Steuerungsgremium der jeweiligen TI-Anwendung festgelegt werden. Die eigentliche Veröffentlichung vom neuen Prüfmodul im Rahmen einer neuen Referenzvalidator-Version findet mind. 3 Monate früher statt (die genaue Vorlaufszeit soll noch mit Steuergremien diskutiert werden).

Change-Management / Problemlösung
---------------------------------

Änderungen an dem Referenzvalidator und an den Prüfmodulen können aus mehreren Gründen erforderlich werden:

1.  Veröffentlichung von neuen Profildefinitionen, die in einem oder mehreren Prüfmodulen verwendet werden.
2.  Entdeckte Fehler im Referenzvalidator bzw. in einem Prüfmodul, die z.B. zu falschen Ausgaben führen (false-positive und false-negative).
3.  Neue Anforderungen, die sich aus alten oder neuen Nutzungsszenarien ergeben (Schnittstelle des Referenzvalidators, Konfigurierbarkeit etc.)
4.  Interne Optimierungen (Technologieaktualisierungen einschließlich Upgrades vom intern verwendeten HAPI-Validator, Verbesserungen mit Hinsicht auf Performance oder Wartbarkeit)

Die Steuergremien informieren die gematik über anstehende Veröffentlichungen von neuen Profilen. Neue Profile werden spätestens 3 Monate vor ihrer verpflichtenden Einführung durch die gematik in das jeweilige Prüfmodul integriert und ein neues Release des Referenzvalidators wird erstellt. Dabei wird der obere Entwicklungsprozess erneut durchlaufen. Für die neuen Profildefinitionen sollen entspechende Testdatensätze vom Profilersteller angefordert werden (siehe z.B. [Verantwortliche für FHIR-Profilierungs-Projekte im Kontext von E-Rezept](https://github.com/gematik/api-erp/blob/master/docs/erp_fhirversion.adoc)). Alte Testdatensätze sollen dabei auf Kompatibilität mit den neuen Profilen überprüft und eventuelle Unstimmigkeiten mit dem jeweiligen Steuergremium abgeklärt werden.

Die entdeckten und gemeldeten Fehler werden mit dem jeweiligen Steuergremium bewertet und das korrekte Verhalten wird spezifiziert. Es wird entschieden, ob z.B. Korrektur des Prüfmoduls und/oder des Profils erforderlich ist. Dabei wird der Entwicklungsprozess erneut durchlaufen.

Änderungen, die sich aus 3-4 ergeben werden direkt durchgeführt, es sei denn die Abwärtskompatibilität der Prüfmodule ist gefährdet. In diesem Fall ist eine Abstimmung mit den Steuergremien bzgl. des Impacts und der Releasezyklen erforderlich. 

Abgrenzung zu anderen Projekten
===============================

ABDA Referenzvalidator
----------------------

Unter [https://github.com/DAV-ABDA/eRezept-Referenzvalidator/](https://github.com/DAV-ABDA/eRezept-Referenzvalidator/) wird ein E-Rezept-Referenzvalidator, der voraussichtlich in Q4/2022 den verbindlichen Status einer Schiedsrichterinstanz für die Apotheken und Krankenkassen erhalten wird. Die Teile des Referenzvalidators werden in das Prüfmodul E-Rezept des Referenzvalidators einfließen. Sobald das Prüfmodul E-Rezept des Referenzvalidators veröffentlicht wurde, soll es den ABDA E-Rezept-Referenzvalidator in seiner Funktion ersetzen (das Vorgehen zur Ablösung wird mit der TK300 abgestimmt).

Von der Anforderungslage her soll der Referenzvalidator neben E-Rezept noch weitere TI-Anwendungen unterstützen. Des Weiteren wird der Referenzvalidator entsprechend den Code und Projektrichtlinien der gematik entwickelt, sodass die aktuelle Übergabestrategie die Übertragung von wiederverwendbaren Teilen aus ABDA E-Rezept Referenzvalidator in ein neu aufgesetztes gematik-Projekt vorsieht. 

HAPI-Validator
--------------

Der [HAPI-Validator](https://hapifhir.io/hapi-fhir/docs/validation/instance_validator.html) gilt als Referenzimplementierung für Konformitätsprüfungen von FHIR-Ressourcen. Der Referenzvalidator verwendet den HAPI-Validator intern als Bibliothek mit einer festgelegten Startkonfiguration und verwaltet darüber hinaus die für eine TI-Anwendung relevanten Profile, Profilversionen, Pakete, Gültigkeitszeiträume und Paket-spezifische Anpassungen für die Referenzvalidator-Ausgaben. Die Tiefe und der Umfang der FHIR-Konformitätsprüfung sowie die Performance sind daher auf die entsprechenden Fähigkeiten der zugrundeliegende HAPI-Validierungsbibliothek beschränkt. Die Ergebnisse der Validierung können im Referenzvalidator nur mit dem Paket-spezifischen Regelwerk zur Nachbearbeitung der HAPI-Ausgaben beeinflusst werden.

E-Rezept-Fachdienstvalidator
--------------

Der E-Rezept-Fachdienst enthält ebenfalls eine Validierungskomponente, mit der Verordnung- und Abgabedatensätzen validiert werden können. Da HAPI/Java als Technologie mit der Sicherheitsarchitektur des Fachdienstes inkompatibel ist, wird der gematik Referenzvalidator im Fachdienst nicht zum Einsatz kommen. Der gematik Referenzvalidator dient allerdings auch für den Fachdienst als Referenz und Schiedsrichter, sodass Ergebnisse der Validierung im Fachdienst mit denen vom Referenzvalidator kontinuierlich angeglichen werden. 
