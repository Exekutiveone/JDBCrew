# JDBCrew

## Lokale Entwicklung

### Datenbank initialisieren

Die Spring-Boot-Anwendung im Modul `device-bridge` nutzt eine lokale SQLite-Datenbank (`data.db`).
Führe den folgenden Befehl im Modulverzeichnis aus, um die Datenbank und das Schema anzulegen:

```bash
cd device-bridge
mvn spring-boot:run
```

Beim Start liest Spring Boot die Datei `src/main/resources/schema.sql` und legt dadurch die benötigten
Tabellen an (z. B. `items`). Nachdem der Server erfolgreich gestartet ist, kannst du ihn mit `Strg+C`
beenden; die initialisierte `data.db` bleibt erhalten.

### Anwendung testen

Nach der Initialisierung kannst du denselben Befehl erneut verwenden, um die Anwendung zu starten
und lokal zu testen:

```bash
cd device-bridge
mvn spring-boot:run
```

Die Anwendung läuft dann standardmäßig auf `http://localhost:8080` und greift auf die eben
initialisierte SQLite-Datenbank zu.
