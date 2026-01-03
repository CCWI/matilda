##############################################################################################################
szenario_1 = """
    <div class="scenario-content">
        <h3>🎯 Szenario 1: Migration DB- und AppServer-Infrastruktur</h3>
        <hr>
        <p><strong>🏢 Ausgangssituation:</strong></p>
        <p>Sie leiten ein Entwicklungsteam, das eine Webanwendung zur Verwaltung von Personalressourcen entwickelt (Projektname "ResManage"). Die Anwendung befindet sich in einem frühen Entwicklungsstadium (Alpha-Version) und nutzt für das Usermanagement aktuell eine <strong>H2-In-Memory-Datenbank</strong>.</p>
        <p>Die Geschäftslogik läuft aktuell auf einem Java-EE-Applikationsserver (<strong>GlassFish</strong>), der in der weiteren Entwicklung durch eine <strong>Open-Source-Alternative</strong> ersetzt werden soll. Die neue Lösung soll <strong>sowohl</strong> einen breiteren Industrie-Support und ein größeres Community-Ökosystem bieten <strong>als auch</strong> skalierbar und kostenoptimiert sein <strong>und zudem</strong> die Neuausrichtung auf modernere, Cloud-native Architekturen ermöglichen.</p>

        <p><strong>✍️ Anforderungen:</strong></p>
        <ul>
            <li>Die Anwendung soll später etwa <strong>2.000 gleichzeitige Benutzer</strong> unterstützen.</li>
            <li>Muss <strong>DSGVO-konform</strong> sein.</li>
            <li>Soll möglichst <strong>langlebig</strong> und <strong>kostenoptimiert</strong> betrieben werden.</li>
        </ul>

        <hr>
        <p><strong>🛠️ Aufgabe:</strong></p>
        <p>Sie benötigen eine fundierte Entscheidungsgrundlage für:</p>
        <ol>
            <li><strong>Die Migration</strong> von der H2-Datenbank zu einer produktionstauglichen Datenbanklösung - vorzugsweise relational.</li>
            <li><strong>Den Wechsel</strong> vom GlassFish Applikationsserver zu einer Open-Source-Alternative, die den genannten Anforderungen besser entspricht.</li>
        </ol>
        <p>Sie ziehen einen Chatbot zur Rate, um die relevantesten Entscheidungsalternativen zu identifizieren/bestätigen und zu bewerten.</p>
    </div>
    """

##############################################################################################################
szenario_2_old = """
    <div class="scenario-content">
        <h3>🎯 Szenario 2: Cloud-Migration Legacy-Anwendung</h3>
        <hr>
        <p><strong>🏢 Ausgangssituation:</strong></p>
        <p>Ihr Unternehmen betreibt eine kritische Unternehmensanwendung zur Steuerung der Lieferkette (Supply Chain Management), die vor <strong>12 Jahren entwickelt</strong> wurde. Die Anwendung basiert auf einer <strong>dreischichtigen Architektur</strong> mit:</p>

        <ul>
            <li><strong>Oracle-Datenbankserver</strong></li>
            <li><strong>Java-Backend (WildFly)</strong></li>
            <li><strong>JavaFX-basierte Client-Anwendung</strong></li>
        </ul>

        <p>Die Infrastruktur wird derzeit auf <strong>unternehmenseigenen Servern (on-premise)</strong> betrieben. Aufgrund von Performanceproblemen, hohen Wartungskosten und der strategischen Neuausrichtung der IT-Abteilung soll die Anwendung <strong>in die Cloud migriert</strong> werden.</p>

        <p><strong>Besondere Herausforderungen:</strong></p>
        <ul>
            <li>Enge Integration mit <strong>lokalen ERP-Systemen</strong></li>
            <li>Strenge <strong>Compliance-Anforderungen</strong> für die Datenverarbeitung innerhalb der EU</li>
        </ul>

        <hr>
        <p><strong>🛠️ Aufgabe:</strong></p>
        <p>Sie benötigen im aktuellen Arbeitsschritt eine strukturierte Analyse der Migrationsmöglichkeiten mit Fokus auf technologische Optionen für die Modernisierung der Anwendungsarchitektur.</p>
        <p>Sie konsultieren einen Chatbot, um eine fundierte Entscheidungsgrundlage für geeignete Technologiealternativen zu erhalten.</p>
    </div>
    """

szenario_2 = """
<div class="scenario-content">
    <h3>🎯 Szenario 2: Cloud-Migration und Modernisierung einer Legacy-Anwendung</h3>
    <hr>
    <p><strong>🏢 Ausgangssituation:</strong></p>
    <p>Ihr Unternehmen betreibt eine geschäftskritische Anwendung zur Steuerung der Lieferkette (Supply Chain Management), die vor <strong>12 Jahren entwickelt</strong> wurde. Die Anwendung ist das technologische Rückgrat für zentrale Logistikprozesse.</p>
    <p>Die Architektur ist monolithisch und besteht aus mehreren eng gekoppelten Komponenten:</p>
    <ul>
        <li><strong>Datenbank:</strong> Ein zentraler Oracle-Datenbankserver.</li>
        <li><strong>Backend:</strong> Ein Java-Backend, das auf einem WildFly läuft.</li>
        <li><strong>Messaging:</strong> Für die interne Prozesssteuerung und asynchrone Anbindung wird <strong>ActiveMQ</strong> eingesetzt.</li>
    </ul>

    <p>Die gesamte Infrastruktur wird auf <strong>unternehmenseigenen Servern (on-premise)</strong> betrieben. Mit wachsender Nutzerbasis und steigenden Transaktionsvolumina stößt die Technologiebasis an ihre Grenzen. Es kommt zu Performanceproblemen, die Wartungskosten sind hoch, und die IT-Abteilung soll strategisch neu auf die <strong>Cloud ausgerichtet</strong> werden.</p>

    <hr>
    <p><strong>🛠️ Aufgabe:</strong></p>
    <p>Sie benötigen im aktuellen Arbeitsschritt eine strukturierte Analyse der Migrations- und Modernisierungsmöglichkeiten. Der Fokus liegt auf technologischen Optionen, die die Anwendung nicht nur in die Cloud heben (Rehosting), sondern sie zukunftsfähig machen (Refactoring/Re-architecting).</p>
    <p>Sie konsultieren einen Chatbot, um eine fundierte Entscheidungsgrundlage für geeignete Technologiealternativen zu erhalten.</p>
</div>"""

##############################################################################################################
szenario_3 = """
    <div class="scenario-content">
        <h3>🎯 Szenario 3: Modernisierung B2B-Plattform (Messaging & UI)</h3>
        <hr>
        <p><strong>🏢 Ausgangssituation:</strong></p>
        <p>Ihr Unternehmen betreibt ein etabliertes <strong>B2B-Portal für elektronische Beschaffungsprozesse</strong>. Die Anwendung wurde vor etwa <strong>6-8 Jahren als monolithische Java-Anwendung (basierend auf dem Spring Framework)</strong> entwickelt und verwaltet komplexe Beschaffungsvorgänge für über <strong>150 Unternehmenskunden</strong>.</p>
        <p>Für die interne Prozesssteuerung und asynchrone Kommunikation wird aktuell <strong>TIBCO Enterprise Message Service (EMS)</strong> eingesetzt. Die Benutzeroberfläche wurde mit <strong>Apache Pivot</strong> realisiert.</p>

        <p>Mit wachsender Nutzerbasis, steigenden Transaktionsvolumina und dem Wunsch nach agilerer Weiterentwicklung stößt die aktuelle Technologiebasis zunehmend an ihre Grenzen:</p>
        <ul>
            <li>Wartung und Weiterentwicklung von <strong>TIBCO EMS</strong> werden aufwendiger, Lizenzkosten sind ein Faktor, und es fehlt an Expertise für moderne Integrationen.</li>
            <li>Die <strong>Apache Pivot UI</strong> wirkt nicht mehr zeitgemäß, bietet eine suboptimale User Experience und erschwert die schnelle Umsetzung neuer Frontend-Features.</li>
            <li>Die <strong>Skalierbarkeit</strong> des Gesamtsystems, insbesondere bei Messaging-Last, gibt Anlass zur Sorge.</li>
            <li><strong>Release-Zyklen sind lang</strong>, Testaufwand hoch und das Risiko von Regressionsfehlern steigt.</li>
        </ul>

        <p>Das Management hat eine umfassende <strong>Modernisierung der Technologiebasis</strong> beschlossen, um die Plattform zukunftssicher aufzustellen, die Entwicklungsgeschwindigkeit zu erhöhen und die Betriebskosten langfristig zu optimieren.</p>

        <hr>
        <p><strong>🛠️ Aufgabe:</strong></p>
        <p>Sie müssen eine fundierte Analyse und Strategie für die Modernisierung des B2B-Portals durchführen, fokussiert auf die Ablösung veralteter Kerntechnologien und die Verbesserung der Gesamtarchitektur. Sie verschaffen sich aktuell eine strukturierte Übersicht über Handlungsoptionen, Best Practices und potenzielle Risiken.
        Dabei wollen Sie als Teil des Vorhabens Designentscheidungen für die Migration von den folgenden Technologien treffen:</p>
        <ol>
            <li><strong>Ersatz für TIBCO EMS:</strong>
                <ul>
                    <li>Geeignetes, modernes Open-Source-Messaging-System.</li>
                    <li>Bewertungskriterien: Skalierbarkeit, Performance, Kosten.</li>
                </ul>
            </li>
            <li><strong>Erneuerung der Apache Pivot UI:</strong>
                <ul>
                    <li>Passende moderne Frontend-Technologie oder serverseitige UI-Framework im Java-Umfeld.</li>
                    <li>Bewertungskriterien: UX, Entwicklerproduktivität, Wartbarkeit und Ansätze für eine schrittweise UI-Migration.</li>
                </ul>
            </li>
        </ol>
        <p>Sie nutzen einen Chatbot, um passende Migrationsoptionen als Entscheidungsgrundlage zu erhalten.</p>
    </div>
    """
