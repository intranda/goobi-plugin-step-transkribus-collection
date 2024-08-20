---
title: Einspielen und Download aus Transkribus Collections
identifier: plugin-step-transkribus-collection
description: Step Plugin für Goobi workflow zum Einspielen von Bildern und den Download von annotierten Ergebnissen aus Transkribus Collections
published: true
---

## Einführung
Die vorliegende Dokumentation beschreibt die Installation, Konfiguration und den Einsatz des Step-Plugins für das Einspielen von Bildern in Transkribus Collections sowie den späteren Download der annotierten Ergebnisse als ALTO-Dateien.

## Installation
Um das Plugin nutzen zu können, müssen folgende Dateien installiert werden:

```bash
/opt/digiverso/goobi/plugins/step/plugin-step-transkribus-collection.jar
/opt/digiverso/goobi/config/plugin_intranda_step_transkribus_collection.xml
```

Nach der Installation des Plugins kann dieses innerhalb des Workflows für die jeweiligen Arbeitsschritte ausgewählt und somit automatisch ausgeführt werden. Ein Workflow könnte dabei beispielhaft wie folgt aussehen:

![Beispielhafter Aufbau eines Workflows](screen1_de.png)

Für das Einspielen der Bilder in eine Transkribus-Collection muss das Werk über einer öffentlich erreichbaren METS-Datei mit dem Plugin `intranda_step_transkribus_collection_ingest` in Transkribus eingespielt werden.

![Konfiguration des Arbeitsschritts für das Einspielen der Bilder in eine Transkribus Collection](screen2_de.png)

Wenn das Werk innerhalb von Transkribus wie gewünscht bearbeitet wurde, kann dieses mit dem Plugin `intranda_step_transkribus_collection_download` wieder heruntergeladen werden.

![Konfiguration des Arbeitsschritts für den Download der ALTO-Datien aus einer Transkribus Collection](screen3_de.png)


## Überblick und Funktionsweise
Dieses Plugin spielt nach dessen Aufruf die METS-Datei bei Transkribus ein und nimmt eine Document-ID entgegen. Diese wird in Goobi als Eigenschaft mit dem Namen `Transkribus Document ID` für den späteren Download gespeichert. 

Nach der Bearbeitung des Werkes in Transkribus kann in einem weiteren Arbeitsschritt der Download der Ergebnisse aus Transkribus angestoßen werden. Hierfür wird erneut die die Document-ID aus der Eigenschaft verwendet und die Daten innerhalb des ALTO-Verzeichnisses im Ordner `ocr` gespeichert.


## Konfiguration
Die Konfiguration des Plugins erfolgt in der Datei `plugin_intranda_step_transkribus_collection.xml` wie hier aufgezeigt:

{{CONFIG_CONTENT}}

{{CONFIG_DESCRIPTION_PROJECT_STEP}}

Parameter               | Erläuterung
------------------------|-----------
`transkribusLogin`      | Geben Sie hier den Nutzernamen für Transkribus an.
`transkribusPassword`   | Geben Sie hier das Passwort für Transkribus an.
`transkribusApiUrl`     | Verwenden Sie hier die API-URL von Transkribus. Diese lautet üblicherweise `https://transkribus.eu/TrpServer/rest/`
`transkribusCollection` | Tragen Sie hier die ID der Transkribus-Collection ein, in die die Dokumente eingespielt werden sollen.
`metsUrl`               | Definieren Sie hier die URL zu der METS-Datei. Hierbei kann innerhalb der URL mit dem Variablen-System von Goobi gearbeitet werden, so dass die URL mit den richtigen Parametern ausgestattet wird.
`ingestDelay`           | Legen Sie hier einen Delay in Millisekunden fest, der für den Ingest der Daten gewartet werden soll, um anschließend die Document-ID von Transkribus abzufragen.
`downloadDelay`         | Legen Sie hier einen Delay in Millisekunden fest, der nach dem Auslösen des Exports und dem tatsächlichen Download gewartet werden soll.