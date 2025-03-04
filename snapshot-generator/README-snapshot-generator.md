# SnapshotGenerator

### Wie generiere ich Snapshots für FHIR-Packages für *neue* Validierungsmodule?
1. In der `pom.xml` vom snapshot-generator muss dem profile mit der id `create-fhir-snapshots` folgender Eintrag hinzugefügt werden:
```xml
<execution>
    <id>id-des-neuen-validationmodule</id>
    <phase>compile</phase>
    <goals>
        <goal>java</goal>
    </goals>
    <configuration>
        <arguments>
            <argument>${project.basedir}/neues-valmodule/src/main/resources/src-package/</argument>
            <argument>${project.basedir}/neues-valmodule/src/main/resources/package/</argument>
        </arguments>
    </configuration>
</execution>
```
Zudem kann hier auch noch ein weiteres optionales Argument angegeben werden, ein Pfad, indem die entpackten FHIR-Packages mit bereits generierten Snapshots abgelegt werden. Diese könnten dann zu Debugging zwecken analysiert werden. Das könnte dann wie folgt aussehen:
```xml
<execution>
    <id>id-des-neuen-validationmodule</id>
    <phase>compile</phase>
    <goals>
        <goal>java</goal>
    </goals>
    <configuration>
        <arguments>
            <argument>${project.basedir}/neues-valmodule/src/main/resources/src-package/</argument>
            <argument>${project.basedir}/neues-valmodule/src/main/resources/package/</argument>
            <argument>--tempDir=C:/pfad/zum/decompressed-snapshot/verzeichnis/</argument>
            <argument>--packages=${packages}</argument>
        </arguments>
    </configuration>
</execution>
```
2. Anschließend müssen alle Quell-FHIR-Packages (ohne Snapshots), die für die Snapshot-Generierung benötigt werden, im Ordner `/neues-valmodule/src/main/resources/src-package/` abgelegt werden.
3. Falls erforderlich, können Korrekturen zu Profilen im Ordner `/neues-valmodule/src/main/resources/src-package/patches/` hinterlegt werden.
4. Um nun die Snapshots für das neue Validierungsmodul zu erzeugen, muss folgender Aufruf getätigt werden: `mvn exec:java@NEUES_VALMODULE -Pcreate-fhir-snapshots`.
5. Die generierten FHIR-Packages mit Snapshots werden im Ordner `/neues-valmodule/src/main/resources/package/` abgelegt und können für die Validierung verwendet werden.
6. Bis auf Weiteres sollen sowohl die Quell-FHIR-Packages als auch FHIR-Packages mit Snapshots mit ins Git eingecheckt werden.

> **Notice**
> Der letzte Kommandozeilenparameter _packages_ ermöglicht die zukünftige Generierung von Snapshots für neue FHIR-Packages für ein bestehendes Validierungsmodul (siehe den nächsten Abschnitt)

### Wie generiere ich Snapshots für neue FHIR-Packages für ein *bestehendes* Validierungsmodul?
1. Neue Quell-FHIR-Packages (ohne Snapshots), die für die Snapshot-Generierung benötigt werden, im Ordner `/bestehendes-valmodule/src/main/resources/src-package/` ablegen.
3. Falls erforderlich, können Korrekturen zu Profilen im Ordner `/bestehendes-valmodule/src/main/resources/src-package/patches/` hinterlegt werden.
4. Um nun die Snapshots für neue FHIR-Packages zu erzeugen, muss folgender Aufruf getätigt werden: `mvn exec:java@BESTEHENDES_VALMODULE -Pcreate-fhir-snapshots -Dpackages=PACKAGE1.tgz,PACKAGE2.tgz`.
5. Die generierten FHIR-Packages mit Snapshots werden im Ordner `/bestehendes-valmodule/src/main/resources/package/` abgelegt und können für die Validierung verwendet werden.
6. Bis auf Weiteres sollen sowohl die Quell-FHIR-Packages als auch FHIR-Packages mit Snapshots mit ins Git eingecheckt werden.


> **Warning**
> Der SnapshotGenerator ignoriert aktuell alle Einträge aus den .tgz Quell-FHIR-Packages, die keine json Dateien sind, d.h. mit `.json` enden!

