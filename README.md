# Arztlisten-Konvertierer

## Funktionen

![Dialog vor Auswahl der JSON-Datei](/docs/screenshots/start.png?raw=true "Dialog vor Auswahl der JSON-Datei")

Dieses Programm ermöglicht es, die von
der [Webseite der kassenärztlichen Bundesvereinigung für die Arzt- und Therapeutensuche](https://arztsuche.116117.de)
angezeigte JSON-Datei für bessere Übersichtlichkeit zu filtern und in eine CSV-Datei zu konvertieren. Die Auswahl der
JSON-Datei öffnet die restlichen Funktionen des Programms.

![Dialog nach Auswahl aller Optionen](/docs/screenshots/full-dialog.png?raw=true "Dialog nach Auswahl aller Optionen")

### Filter

Zum Ausschluss bestimmter Ärzte und Therapeuten kann je eine Datei geladen werden, die die Namen oder Telefonnummern,
die herausgefiltert werden sollen, enthält. Die Namen bzw. Telefonnummern müssen exakt mit denen im Datenbestand der 
kassenärztlichen Bundesvereinigung übereinstimmen. Mehrere zu filternde Einträge werden durch einen Zeilenumbruch 
getrennt. Beispiel:

```
Max Mustermann
Dr. Test Test
Moritz Manfred Muster
```

Ebenfalls kann nach den gewünschten Sprechstunden-Arten (z.B. Offene Sprechstunde) gefiltert werden, hierfür müssen 
lediglich die gewünschten Sprechstunden-Arten ausgewählt werden.

Die JSON-Datei reicht über einen Zeitraum von zwei Wochen. Dieser Zeitraum kann durch eine Beschränkung verkürzt werden.
Eine Beschränkung von "1 Tag" am 01.01. würde nur Sprechzeiten am 01.01. und 02.02. ausgeben.

### Dateiausgabe
Das Programm schreibt nach der Konvertierung eine Datei mit dem Namen "data.csv" in den ausgewählten Ausgabeordner oder,
falls kein Ausgabeordner ausgewählt wurde, in das Arbeitsverzeichnis des Programms (typischerweise das 
Installationsverzeichnis).

## Installation und Build

### Windows-Installation

Unter [Releases](https://github.com/Pikachamp/arztliste/releases) findet sich ein Installationsprogramm (msi-Datei) für
die aktuelle Version. Diese kann heruntergeladen und ausgeführt werden, um das Programm zu installieren. Aus dem 
Quellcode gebaut werden kann das Installationsprogramm mit dem Befehl 
``` sh
gradlew.bat packageMsi
```

### Build
Gebaut und ausgeführt werden kann das Programm mit dem Gradle-Task `run`, die mit dem Installationsprogramm installierte
und ausgeführte Version kann mit dem Task `runDistributable` gestartet werden.
