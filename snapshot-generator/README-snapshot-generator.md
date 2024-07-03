# SnapshotGenerator

### Wie generiere ich Snapshots für FHIR-Packages für neue Validierungsmodule?
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
Zudem kann hier auch noch (optional), als drittes Argument, ein Pfad angegeben werden, indem die entpackten FHIR-Packages mit bereits generierten Snapshots abgelegt werden. Das könnte dann wie folgt aussehen:
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
            <argument>C:/pfad/zum/decompressed-snapshot/verzeichnis/</argument>
        </arguments>
    </configuration>
</execution>
```
Außerdem können hier auch noch (optional), als viertes Argument, Quell-FHIR-Packages angegeben werden, die nur für die Snapshot-Generierung benötigt werden, aber nicht für die spätere Validierung von FHIR-Resourcen. Das würde dann wie folgt aussehen:
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
            <argument>C:/pfad/zum/decompressed-snapshot/verzeichnis/</argument>
            <argument>package1.tgz package2.tgz package3.tgz</argument>
        </arguments>
    </configuration>
</execution>
```
2. Anschließend müssen alle Quell-FHIR-Packages (ohne Snapshots), die für die Snapshot-Generierung benötigt werden, im Ordner `/neues-valmodule/src/main/resources/src-package/` abgelegt werden.
3. Falls erforderlich, können Korrekturen zu Profilen im Ordner `/neues-valmodule/src/main/resources/src-package/patches/` hinterlegt werden.
4. Um nun die Snapshots für das neue Validierungsmodul zu erzeugen, muss folgender Aufruf getätigt werden: `mvn exec:java@NEUES_VALMODULE -Pcreate-fhir-snapshots`.
5. Die generierten FHIR-Packages mit Snapshots werden im Ordner `/neues-valmodule/src/main/resources/package/` abgelegt und können für die Validierung verwendet werden.
6. Bis auf Weiteres sollen sowohl die Quell-FHIR-Packages als auch FHIR-Packages mit Snapshots mit ins Git eingecheckt werden. 

> **Warning**
> Der SnapshotGenerator ignoriert aktuell alle Einträge aus den .tgz Quell-FHIR-Packages, die keine json Dateien sind, d.h. mit `.json` enden!

