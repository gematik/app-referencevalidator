# Unterstützte Profile und eingebundene Packages

<details>
  <summary>Inhaltsverzeichnis</summary>
  <ol>
    <li>
      <a href="#e-rezept">E-Rezept</a>
    </li>
    <li>
      <a href="#e-rezept">EAU</a>   
    </li>
    <li>
      <a href="#e-rezept">ISiP-1</a>   
    </li> 
  </ol>
</details>

## E-Rezept

### Anpassungen der Packages:
- Beispiele in Packages entfernt
- Alle Packages enthalten Snapshots

### Anpassungen der Profile
- de.gematik.erezept-workflow.r4-1.0.3-1.tgz
  - ErxChargeItem.json
    - BugFix: Korrektur der supportiveInformation-Slices (Keine Snapshot-Generierung sonst möglich)
- de.gematik.erezept-workflow.r4-1.1.1.tgz
  - ErxCommunicationReply.json 
    - Communication.about targetProfile: typos: KBV_PR_ERP_Medikament_Freitext, KBV_PR_ERP_Medikament_PZN, KBV_PR_ERP_Medikament_Rezeptur, erxTask. Korrigiert in KBV_PR_ERP_Medication_FreeText, KBV_PR_ERP_Medication_PZN, KBV_PR_ERP_Medication_Compounding, ErxTask
- kbv.ita.erp-1.0.1.tgz
  - KBV_PR_ERP_Prescription.json
    - BugFix: Korrektur der Profilreferenz (MedicationRequest.insurance = "type":[{"code":"Reference","targetProfile":["https://fhir.kbv.de/StructureDefinition/KBV_PR_FOR_Coverage|1.0.3"]}])
    - Constraint -erp-begrenzungValue hinzugefügt (Die Anzahl der verordneten Packungen darf maximal 999 sein) aufgrund eines [Bugs im HL7 FHIR Validator](https://github.com/hapifhir/org.hl7.fhir.core/issues/967)
- kbv.ita.erp-1.0.2.tgz
  - KBV_PR_ERP_Prescription.json
    - Constraint -erp-begrenzungValue hinzugefügt (Die Anzahl der verordneten Packungen darf maximal 999 sein) aufgrund eines [Bugs im HL7 FHIR Validator](https://github.com/hapifhir/org.hl7.fhir.core/issues/967)
- de.abda.erezeptabgabedatenbasis-1.1.0.tgz
  - Extension-DAV-EX-ERP-Rezeptaenderung.json
  - Extension-DAV-EX-ERP-Zusatzattribute.json
  - Extension-DAV-EX-ERP-ZusatzdatenHerstellung.json
    - Änderungen siehe Version 0.9.6 im [ChangeLog.md des ABDA Referenzvalidators](https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/main/CHANGELOG.md)

### Packages
* de.basisprofil.r4-0.9.13.tgz
* de.basisprofil.r4-1.3.2.tgz
* kbv.basis-1.1.3.tgz
* kbv.basis-1.3.0.tgz
* kbv.ita.for-1.0.3.tgz
* kbv.ita.for-1.1.0.tgz
* kbv.ita.erp-1.0.1.tgz
* kbv.ita.erp-1.0.2.tgz
* kbv.ita.erp-1.1.1.tgz
* de.abda.erezeptabgabedaten-1.0.3.tgz
* de.abda.erezeptabgabedaten-1.1.2.tgz
* de.abda.erezeptabgabedaten-1.2.0.tgz
* de.abda.erezeptabgabedaten-1.3.1.tgz
* de.abda.erezeptabgabedatenbasis-1.1.0.tgz
* de.abda.erezeptabgabedatenbasis-1.1.3.tgz
* de.abda.erezeptabgabedatenbasis-1.2.0.tgz
* de.abda.erezeptabgabedatenbasis-1.3.0.tgz
* de.abda.erezeptabgabedatenbasis-1.3.1.tgz
* de.abda.erezeptabgabedatenpkv-1.2.0.tgz
* de.gematik.erezept-patientenrechnung.r4-1.0.1.tgz
* de.gematik.erezept-workflow.r4-1.0.3-1.tgz
* de.gematik.erezept-workflow.r4-1.1.1.tgz
* de.gematik.erezept-workflow.r4-1.2.1.tgz
* de.gkvsv.erezeptabrechnungsdaten-1.0.4.tgz
* de.gkvsv.erezeptabrechnungsdaten-1.0.5.tgz
* de.gkvsv.erezeptabrechnungsdaten-1.0.6.tgz
* de.gkvsv.erezeptabrechnungsdaten-1.1.0.tgz
* de.gkvsv.erezeptabrechnungsdaten-1.2.0.tgz
* de.gkvsv.erezeptabrechnungsdaten-1.3.0.tgz
* [Validierungsrelevante Codesysteme und Valuesets der KBV:](https://update.kbv.de/ita-update/DigitaleMuster/ERP/)
  * dav.kbv.sfhir.cs.vs-1.0.2-json.tgz
  * dav.kbv.sfhir.cs.vs-1.0.3-json.tgz (Anpassung DARREICHUNGSFORM v1.09 ab 01.04.2022)
  * gematik.kbv.sfhir.cs.vs-1.0.0.tgz (Gültig ab 1.7.2023, [Quelle](https://update.kbv.de/ita-update/DigitaleMuster/ERP/III_2023/KBV_FHIR_eRP_V1.1.0_zur_Validierung.zip))


### Profile

* http://fhir.abda.de/eRezeptAbgabadaten/StructureDefinition/DAV-PR-ERP-Abgabeinformationen
  * 1.0.3

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PKV-PR-ERP-AbgabedatenBundle
  * 1.2

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PKV-PR-ERP-AbgabedatenComposition
  * 1.2

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PKV-PR-ERP-Abgabeinformationen
  * 1.2

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PKV-PR-ERP-Abrechnungszeilen
  * 1.2

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PKV-PR-ERP-Apotheke
  * 1.2

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PKV-PR-ERP-ZusatzdatenEinheit
  * 1.2

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PKV-PR-ERP-ZusatzdatenHerstellung
  * 1.2

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-Base-AbgabedatenBundle
  * 1.1.0

  * 1.2

  * 1.3

  * 1.3.1

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-Base-AbgabedatenComposition
  * 1.1.0

  * 1.2

  * 1.3

  * 1.3.1

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-Base-Abgabeinformationen
  * 1.1.0

  * 1.2

  * 1.3

  * 1.3.1

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-Base-Abrechnungszeilen
  * 1.1.0

  * 1.2

  * 1.3

  * 1.3.1

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-Base-Apotheke
  * 1.1.0

  * 1.2

  * 1.3

  * 1.3.1

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-Base-ZusatzdatenEinheit
  * 1.1.0

  * 1.2

  * 1.3

  * 1.3.1

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-Base-ZusatzdatenHerstellung
  * 1.1.0

  * 1.2

  * 1.3

  * 1.3.1

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-AbgabedatenBundle
  * 1.0.3

  * 1.1.0

  * 1.2

  * 1.3

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-AbgabedatenComposition
  * 1.0.3

  * 1.1.0

  * 1.2

  * 1.3

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-Abgabeinformationen
  * 1.1.0

  * 1.2

  * 1.3

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-Abrechnungszeilen
  * 1.0.3

  * 1.1.0

  * 1.2

  * 1.3

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-Apotheke
  * 1.0.3

  * 1.1.0

  * 1.2

  * 1.3

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-ZusatzdatenEinheit
  * 1.0.3

  * 1.1.0

  * 1.2

  * 1.3

* http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-ZusatzdatenHerstellung
  * 1.0.3

  * 1.1.0

  * 1.2

  * 1.3

* https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_Binary
  * 1.3

* https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_ERP_eAbrechnungsdaten
  * 1.0.4

  * 1.0.5

  * 1.0.6

  * 1.1.0

  * 1.2

  * 1.3

* https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_Rechnung
  * 1.0.4

  * 1.0.5

  * 1.0.6

  * 1.1.0

  * 1.2

* https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_Rechnung_Bundle
  * 1.3

* https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_Rechnung_Composition
  * 1.3

* https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_Rechnung_List
  * 1.3

* https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_RezeptBundle
  * 1.0.4

  * 1.0.5

  * 1.0.6

  * 1.1.0

  * 1.2

  * 1.3

* https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_Sammelrechnung_Bundle
  * 1.0.4

  * 1.0.5

  * 1.0.6

  * 1.1.0

  * 1.2

* https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_Sammelrechnung_Composition
  * 1.0.4

  * 1.0.5

  * 1.0.6

  * 1.1.0

  * 1.2

* https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_Sammelrechnung_List
  * 1.0.4

  * 1.0.5

  * 1.0.6

  * 1.1.0

  * 1.2

* https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle
  * 1.0.1

  * 1.0.2

  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Composition
  * 1.0.1

  * 1.0.2

  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Medication_Compounding
  * 1.0.1

  * 1.0.2

  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Medication_FreeText
  * 1.0.1

  * 1.0.2

  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Medication_Ingredient
  * 1.0.1

  * 1.0.2

  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Medication_PZN
  * 1.0.1

  * 1.0.2

  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_PracticeSupply
  * 1.0.1

  * 1.0.2

  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Prescription
  * 1.0.1

  * 1.0.2

  * 1.1.0

* https://gematik.de/fhir/StructureDefinition/ErxAcceptOperationOutParameters
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxAuditEvent
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxBinary
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxChargeItem
  * 1.1.0

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxCommunicationDispReq
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxCommunicationInfoReq
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxCommunicationReply
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxCommunicationRepresentative
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxComposition
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxConsent
  * 1.1.0

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxDevice
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxMedicationDispense
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxOrganization
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxReceipt
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/StructureDefinition/ErxTask
  * 1.0.3-1

  * 1.1.1

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PAR_OUT_OP_Accept
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_AuditEvent
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_BfArMApproval
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Binary
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Bundle
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_CloseOperationInputBundle
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Communication_DispReq
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Communication_InfoReq
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Communication_Reply
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Communication_Representative
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Composition
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Device
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Digest
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_MedicationDispense
  * 1.2

* https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Task
  * 1.2

* https://gematik.de/fhir/erpchrg/StructureDefinition/GEM_ERPCHRG_PR_ChargeItem
  * 1.0

* https://gematik.de/fhir/erpchrg/StructureDefinition/GEM_ERPCHRG_PR_Communication_ChargChangeReply
  * 1.0

* https://gematik.de/fhir/erpchrg/StructureDefinition/GEM_ERPCHRG_PR_Communication_ChargChangeReq
  * 1.0

* https://gematik.de/fhir/erpchrg/StructureDefinition/GEM_ERPCHRG_PR_Consent
  * 1.0


## EAU

### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

### Anpassungen der Profile
- de.basisprofil.r4-0.9.13.tgz
  - Extension-seitenlokalisation.json
    - BugFix: Version 0.9.12 auf 0.9.13 korrigiert 

### Packages

* de.basisprofil.r4-0.9.13.tgz
* de.basisprofil.r4-1.3.2.tgz
* kbv.basis-1.1.3.tgz
* kbv.basis-1.3.0.tgz
* kbv.ita.for-1.0.3.tgz
* kbv.ita.for-1.1.0.tgz
* kbv.ita.eau-1.1.0.tgz
* kbv.ita.eau-1.0.2.tgz
* [Validierungsrelevante Codesysteme und Valuesets der KBV:](https://update.kbv.de/ita-update/DigitaleMuster/ERP/)
  * gematik.kbv.sfhir.cs.vs-1.0.0.tgz ([Quelle](https://update.kbv.de/ita-update/DigitaleMuster/ERP/III_2023/KBV_FHIR_eRP_V1.1.0_zur_Validierung.zip))

### Profile

* https://fhir.kbv.de/StructureDefinition/KBV_PR_EAU_Bundle
  * 1.0.2
  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_EAU_Strono_Bundle
  * 1.0.2
  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_EAU_Strono
  * 1.0.2
  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_EAU_Composition
  * 1.0.2
  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_EAU_Condition_AU
  * 1.0.2
  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_EAU_Condition_ICD
  * 1.0.2
  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_EAU_Condition_Text
  * 1.0.2
  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_EAU_ServiceRequest_steps
  * 1.0.2
  * 1.1.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_EAU_Storno_Composition
  * 1.0.2
  * 1.1.0

## ISiP-1

### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

### Packages

* de.gematik.isip-1.0.1.tgz
* kbv.basis-1.2.0.tgz
* kbv.mio.ueberleitungsbogen-1.0.0-kommentierung.tgz
* de.gematik.isik-basismodul-2.0.2.tgz
* de.basisprofil.r4-1.4.0.tgz

### Profile

* https://gematik.de/fhir/isip/v1/Basismodul/StructureDefinition/ISiPAngehoeriger
  * 1.0.1

* https://gematik.de/fhir/isip/v1/Basismodul/StructureDefinition/ISiPPersonImGesundheitswesen
  * 1.0.1

* https://gematik.de/fhir/isip/v1/Basismodul/StructureDefinition/ISiPPflegeempfaenger
  * 1.0.1

* https://gematik.de/fhir/isip/v1/Basismodul/StructureDefinition/ISiPPflegeepisode
  * 1.0.1

* https://gematik.de/fhir/isip/v1/Basismodul/StructureDefinition/IsipOrganization
  * 1.0.1

## ISiK-2

### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

### Packages

* de.gematik.isik-basismodul-2.0.5.tgz
* de.gematik.isik-medikation-2.0.3.tgz
* de.gematik.isik-terminplanung-2.0.2.tgz
* de.gematik.isik-vitalparameter-2.0.3.tgz

### Profile

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKAbrechnungsfall
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKAngehoeriger
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKBerichtBundle
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKBerichtSubSysteme
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKBinary
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKCodeSystem
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKDiagnose
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKKontaktGesundheitseinrichtung
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKPatient
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKPersonImGesundheitsberuf
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKProzedur
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKValueSet
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKVersicherungsverhaeltnisGesetzlich
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Basismodul/StructureDefinition/ISiKVersicherungsverhaeltnisSelbstzahler
  * 2.0.5

* https://gematik.de/fhir/isik/v2/Medikation/StructureDefinition/ISiKMedikament
  * 2.0.2

* https://gematik.de/fhir/isik/v2/Medikation/StructureDefinition/ISiKMedikationTransaction
  * 2.0.2

* https://gematik.de/fhir/isik/v2/Medikation/StructureDefinition/ISiKMedikationTransactionResponse
  * 2.0.2

* https://gematik.de/fhir/isik/v2/Medikation/StructureDefinition/ISiKMedikationsInformation
  * 2.0.2

* https://gematik.de/fhir/isik/v2/Medikation/StructureDefinition/ISiKMedikationsListe
  * 2.0.2

* https://gematik.de/fhir/isik/v2/Medikation/StructureDefinition/ISiKMedikationsVerabreichung
  * 2.0.2

* https://gematik.de/fhir/isik/v2/Medikation/StructureDefinition/ISiKMedikationsVerordnung
  * 2.0.2

* https://gematik.de/fhir/isik/v2/Terminplanung/StructureDefinition/ISiKKalender
  * 2.0.3

* https://gematik.de/fhir/isik/v2/Terminplanung/StructureDefinition/ISiKMedizinischeBehandlungseinheit
  * 2.0.3

* https://gematik.de/fhir/isik/v2/Terminplanung/StructureDefinition/ISiKNachricht
  * 2.0.3

* https://gematik.de/fhir/isik/v2/Terminplanung/StructureDefinition/ISiKTermin
  * 2.0.3

* https://gematik.de/fhir/isik/v2/Terminplanung/StructureDefinition/ISiKTerminKontaktMitGesundheitseinrichtung
  * 2.0.3

* https://gematik.de/fhir/isik/v2/Terminplanung/StructureDefinition/ISiKTerminblock
  * 2.0.3

* https://gematik.de/fhir/isik/v2/VitalwerteUndKoerpermasse/StructureDefinition/ISiKAtemfrequenz
  * 2.0.3

* https://gematik.de/fhir/isik/v2/VitalwerteUndKoerpermasse/StructureDefinition/ISiKBlutdruck
  * 2.0.3

* https://gematik.de/fhir/isik/v2/VitalwerteUndKoerpermasse/StructureDefinition/ISiKEkg
  * 2.0.3

* https://gematik.de/fhir/isik/v2/VitalwerteUndKoerpermasse/StructureDefinition/ISiKGCS
  * 2.0.3

* https://gematik.de/fhir/isik/v2/VitalwerteUndKoerpermasse/StructureDefinition/ISiKHerzfrequenz
  * 2.0.3

* https://gematik.de/fhir/isik/v2/VitalwerteUndKoerpermasse/StructureDefinition/ISiKKoerpergewicht
  * 2.0.3

* https://gematik.de/fhir/isik/v2/VitalwerteUndKoerpermasse/StructureDefinition/ISiKKoerpergroesse
  * 2.0.3

* https://gematik.de/fhir/isik/v2/VitalwerteUndKoerpermasse/StructureDefinition/ISiKKoerpertemperatur
  * 2.0.3

* https://gematik.de/fhir/isik/v2/VitalwerteUndKoerpermasse/StructureDefinition/ISiKKopfumfang
  * 2.0.3

* https://gematik.de/fhir/isik/v2/VitalwerteUndKoerpermasse/StructureDefinition/ISiKSauerstoffsaettigung
  * 2.0.3

## ISiK-1

### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

### Packages

* de.gematik.isik-basismodul-stufe1-1.0.9.tgz


### Profile

* https://gematik.de/fhir/ISiK/StructureDefinition/ISiKAngehoeriger
  * 1.0.9

* https://gematik.de/fhir/ISiK/StructureDefinition/ISiKBerichtBundle
  * 1.0.9

* https://gematik.de/fhir/ISiK/StructureDefinition/ISiKBerichtSubSysteme
  * 1.0.9

* https://gematik.de/fhir/ISiK/StructureDefinition/ISiKDiagnose
  * 1.0.9

* https://gematik.de/fhir/ISiK/StructureDefinition/ISiKKontaktGesundheitseinrichtung
  * 1.0.9

* https://gematik.de/fhir/ISiK/StructureDefinition/ISiKPatient
  * 1.0.9

* https://gematik.de/fhir/ISiK/StructureDefinition/ISiKPersonImGesundheitsberuf
  * 1.0.9

* https://gematik.de/fhir/ISiK/StructureDefinition/ISiKProzedur
  * 1.0.9

* https://gematik.de/fhir/ISiK/StructureDefinition/ISiKVersicherungsverhaeltnisGesetzlich
  * 1.0.9

* https://gematik.de/fhir/ISiK/StructureDefinition/ISiKVersicherungsverhaeltnisSelbstzahler
  * 1.0.9

## DIGA-Toolkit

### Anpassungen der Packages:
- Alle Packages enthalten Snapshots

### Packages

* kbv.mio.diga-1.0.0-festlegung.tgz


### Profile

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_AllergyIntolerance_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Appointment_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Bundle
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_CarePlan_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Composition
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Condition_Problem_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_DeviceDefinition_DIGA
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_DeviceDefinition_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Device_DIGA
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Device_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Encounter_Past_Appointment
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Goal_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_MedicationAdministration_Medication_Intake_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_MedicationStatement_Medication_Request_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Medication_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Assessment_Scale
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Assessment_Score_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Blood_Pressure
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Body_Height
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Body_Temperature
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Body_Weight
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Diary_Entry
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Environmental_Factor_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Functional_Assessment_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Glucose_Concentration
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Head_Circumference
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Heart_Rate
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Life_Style_Factor_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Nutrition_Intake
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Nutrition_Intake_Food_Composition_Types
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Peripheral_Oxygen_Saturation
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Observation_Respiratory_Rate
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Organization
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Organization_Manufacturer
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Patient
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Practitioner
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_PractitionerRole
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Procedure_Activity_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Provenance
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_QuestionnaireResponse_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_Questionnaire_Free
  * 1.0.0

* https://fhir.kbv.de/StructureDefinition/KBV_PR_MIO_DIGA_RelatedPerson
  * 1.0.0