# Prosjekt Nettverksprogrammering
### Innhold
* [Biblioteker og CI](#biblioteker)
* [Introduksjon](#introduksjon)
* [Implementert funksjonalitet](#funksjonalitet)
    * [Tjener](#funksjonalitet_tjener)
    * [Klient](#funksjonalitet_klient)
* [Diskusjon](#diskusjon)
* [Teknologier](#teknologier)
* [Forbedringer](#forbedringer)
* [Eksempler](#eksempler)
* [Installasjon](#installasjon)
* [Testing](#testing)
* [API](#api)



<a name="biblioteker"></a>
## Navn på biblioteket og eventuell lenke til continuous integration løsning
Insert CI link her

<a name="introduksjon"></a>
## Introduksjon
I dette prosjektet lager vi en two-phase commit løsning.
Denne fungerer ved at alle tilkoblede klienter må "stemme" over valg som gjøres.
I dette prosjektet bruker vi tjenerens navn som et eksempel.
Navet endres og alle tilkoblede parter må være enige om å beholde det nye navnet.
Dersom én er uenig ruller vi tilbake til det gamle tjenernavnet (rollback).
Alle parter må stemme før et valg er avgjort.

<a name="funksjonalitet"></a>
## Implementert funksjonalitet
###Klassediagram
![Image description](https://i.imgur.com/TNIkv8b.png "Klassediagram") 

Siden prosjektet er inndelt i tjener og klient,
to deler i et distibuert system, ser vi også på
funksjonaliteten for disse hver for seg.

<a name="funksjonalitet_tjeneer"></a>
### Tjener
På tjenerside har vi først og fremst implementert
tråder slik at hver tilkobling til tjener kjører på
en egen tråd. Dermed sikrer vi at klienter ikke
begrenses av andre klienters tilkobling til tjeneren.

Videre bruker vi sockets for å lytte etter tilkoblinger
fra klient. I Java bruker vi ServerSocket for å lytte på
port 1111 ettr klienter:
```java
int port_nummer = 1111;
ServerSocket tjenerSocket = null;
try {
    tjenerSocket = new ServerSocket(port_nummer);
} catch(IOException e) {
    System.out.println(e);
}
```
Tjeneren kjører så i loop og oppretter egne tråder
for hver klient som kobler seg til, gitt at serverSocket
aksepterer forbindelsen uten å kaste exceptions:
```java
while (!tjener.lukket) {
    try {
        klientSocket = tjenerSocket.accept();
        KlientTraad klientTraad = new KlientTraad(tjener, klientSocket);
        klientTraad.start();
    } catch (IOException e) {
        e.pritStackTrace();
    }
}
```

<a name="funksjonalitet_klient"></a>
### Klient
I klient klassen opprettes også en socket på port 1111. 
Deretter oppretter vi lese og skrive forbindelse til
tjeneren ved hjelp av PrintStream(os) og 
DataInputStream(is) i form av output- og inputstream:
```java
int port=1111;
String host="localhost";
try {
    klientSocket = new Socket(host, port);
    inputLinje = new BufferedReader(new InputStreamReader(System.in));
    os = new PrintStream(klientSocket.getOutputStream());
    is = new DataInputStream(klientSocket.getInputStream());
} catch (Exception e) {
    System.out.println("Exception occurred : " + e.getMessage());
}
```
Ved nye hendelser fra tjener sørger en run-metode
i klient for å plukke opp meldinger fra tjener i en loop
som kjøres så lenge ikke alle tilkoblede klienter har 
stemt:
```java
try {
    while ((responseLinje = is.readLine()) != null) {
        System.out.println("\n"+responseLinje);
        if (responseLinje.equalsIgnoreCase("GLOBAL_COMMIT") || responseLinje.equalsIgnoreCase("GLOBAL_ABORT")) {
            break;
        }
    }
    lukket=true;
} catch (IOException e) {
    System.err.println("IOException:  " + e);
}
```
<a name="diskusjon"></a>
## Diskusjon
Først og fremst har vi valgt å programmere løsningen i Java. 
Hovedgrunnen til dette er at gruppen hadde god kjennskap til det, 
men også fordi flere av relevante øvinger tilknyttet til dette prosjektet ble skrevet i Java.
Til inspirasjon har vi tatt i bruk litt av den samme implementasjonen som vi innførte i øving 4. 
Vi har valgt å ta i bruk socketer for mottak og sending av data. Vi bruker derfor også en-til-en-koblings
protokollen Transmission Control Protocol (TCP). Den typen socket som bruker TCP kalles en 
strømmingssocket eller en tilkoblingsorientert socket. Med TCP kan man koble flere klienter opp mot samme
TCP-tjener. For å gjøre dette må man opprette en barnprosess for hver enkelt klient og deretter opprette en 
TCP-kobling mellom tilhørende prosess og klient. I dette prosjektet brukte vi tråder for å gjennomføre dette
og det opprettes en socket for hver tilkobling.

###
En annen protokoll som vi kunne ha brukt i stedet for TCP er U​ser D​atagram P​rotocol (UDP).
Dette er en av de vanligste protokollene som brukes på internett.  UDP sender pakker raskt mellom maskiner som er 
tilkoblet på samme nettverk med minst mulig overhead. 
Dette er fordi UDP ikke gir noen garanti om at pakker kommer fram.
Sammenliknet med TCP er at UDP har en fordel ved at hver pakke er mye mindre enn en TCP-pakke. 
UDP har ikke noe kvalitetssikring som sørger for at man slipper ekstra trafikk for å sjekke om at alt er riktig overført.
Likevel er UDP er en upålitelig protokoll og man kan derfor ikke være sikre på at overføringen av data er feilfri. 
Her ønsker man gjerne at pakkene skal komme frem korrekt og i riktig rekkefølge, noe UDP ikke kan garantere. 
Tjeneren skiller heller ikke mellom forskjellige klienter, 
så hvis flere sender meldinger til tjeneren samtidig kan det fort bli rot.
Benytter man seg av TCP vil man derimot kunne få informasjon om pakkene som sendes mellom sender og mottaker. 
Underveis vil man kunne få status angående pakkene, og TCP vil kunne ordne opp dersom dataene er blitt ødelagte 
underveis og det vil sørge for at pakkene når mottakeren. 
Med andre ord er TCP et grensesnitt som sørger for en pålitelig overføring sammenlignet med UDP.
###
Når det kommer til arkitektur/design-valg har vi som tidligere nevnt, en tjener, klient, klientTraad og en 
loggforer-klasse. TCP opererer på klient-tjener-arkitekturen og forbindelsen må gå begge veier. 
Det er derfor logisk at vi har et designmønsteret og arkitekturen klient-tjener på grunn av bruken av TCP. 
En deltaker i two phase commit vil i klient-tjener modellen være en klient, og koordinatoren vil derfor bli tjeneren. 
Det er klienten/deltakeren som initierer kommunikasjon med tjeneren som venter på inngående forespørsler.
###
En beskrivelse og diskusjon/argumentasjon (denne delen en veldig viktig ved evaluering) av hvilke teknologi- og arkitektur-/designvalg dere har stått ovenfor (når dere skulle løse oppgaven), hva dere hadde å velge mellom og hvorfor dere har valgt det dere har valgt. Når det gjelder teknologivalg så kan denne delen begrenses til «pensum i faget».

<a name="teknologier"></a>
## Hvilke teknologier har vi brukt og hvorfor?
Hva hadde vi å velge mellom, hvorfor valgte vi som vi gjorde?

**Sockets**

Den typen socket som bruker TCP kalles en strømmingssocket eller en tilkoblingsorientert socket. Med TCP kan man koble flere klienter opp mot samme
TCP-tjener. For å gjøre dette må man opprette en barnprosess for hver enkelt klient og deretter opprette en 
TCP-kobling mellom tilhørende prosess og klient. 

**Tråder**

I dette prosjektet brukte vi tråder for å opprettes TCP-kobling mellom tjener og socketer for hver klient. 
Vi bruker Java Thread der klassen KlientTraad, er en underklasse som extender Thread. 
Der overstyrer vi kjøremetoden som er den første metoden som kjøres når etter vi starter tråden. 
Fordelen med dette er at en Java tråd oppfører seg som en virtuell CPU slik at hver klient kan kjøre uforstyrret. 


Klientklassen implementerer Runnable som er en annen måte å spesifisere hvordan en tråd skal kjøre. Dette grensesnittet har kun en run() metode.

**Filewriter/reader til logging**

Java FileWriter brukes til å skrive bokstavorientert data til en fil. Fungerer omtrent som FileOutputStream bortsett fra at FOS er bytebasert, mens FileWriter er karakterbasert.
FileWriter passer seg altså bedre til å skrive ord og tekst. 


**Printstream & DataInputStream**
PrintStream og DataInputStream legger til funksjonalitet til andre strømmer, altså input og putputStream fra Socket. På denne måten kan tjeneren lese dataen som har blitt sendt via socketene over nettet til klientene. 
PrintStream og DataInputStream pakker inn outputStream og inputStream fra Socketen slik at vi kan lese data(tall og bokstaver), i stedet for bytes.

<a name="forbedringer"></a>
## Fremtidig arbeid med oversikt over mangler og mulige forbedringer
Det finnes flere ting som kan implementeres for å få en mer fullverdig implementasjon av
two-phase commit:
- GLOBAL_ABORT hvis en av klientene aldri svarer med at den er klar med COMMIT
- GLOBAL_ABORT hvis en av klientene feiler eller termineres før ACKNOWLEDGEMENT

<a name="eksempler"></a>
## Eksempler som viser bruken av løsningen
Det første skjer når tjeneren starter er at man får tilbakemelding om at "Serveren er startet...". Deretter får man beskjed hver gang en klient kobler seg til.


![Tjeneren start](https://media.giphy.com/media/H3NkJDdphWTUph0NYQ/giphy.gif)

Når man så kjører en klient får man følgende beskjed om å skrive inn navnet sitt. Når klienten gjør dette, får den
beskjed om å oppgi en saldo. Navnet brukes til å opprette en logg i navnet til klienten, samt en bedre visuell tilbakemelding på tjenersiden.
Saldoen som blir oppgitt er for å simulere transaksjonen i denne applikasjonen. På dette steget kan man "manuelt" velge om transaksjonen skal commites eller abortes, ved at saldoen skal være over 5 for at transaksjonen skal fullføres.


Det ser slik ut hos klient 1:

![Image description](https://media.giphy.com/media/VgO01PgxtM4Tjq2n1h/giphy.gif)



Vi har nå kommet til første del av *Two-phase commit*-protokollen, der klientene skal stemme om de er klare til å commite. 
Herfår man en velkomstmelding, og et spørsmål om de vil godkjenne at det trekkes 5 fra saldoen sin. 
Her har vi gjort slik at stemmingen gjøres ved at klientene kun trykker på enter, og så gjør systemet resten. 
I *Two-phase commit*-protokollen skal klientene stemme **ja** om de har gjort alt klart for å commite dersom alle klientene stemmer commit, og **nei** dersom noe ikke stemmer eller har gått galt.
Siden dette kun er en simulering av en transaksjon har vi gjort slik at systemet avgjør om en klient har klargjort sin del av prosessen. 
Det eneste som avgjør om en klient har klargjort sin del er om de har en saldo som er større eller lik 5. 


På denne måten må klienten ha "råd" for at det skal stemmes **ja** for commit. 
Dersom en av klientene har over 5 i saldo og derfor "stemmer" for å commite vil kun tjeneren se denne beskjeden:


![Image description](https://media.giphy.com/media/W3U1apPvHton0F833t/giphy.gif "Tjener får commit")


Hvis de andre klienten også har råd og dermed også "stemmer" for commit, har alle klientene stemt for commit, og transaksjonen er klar til å gjennomføres. 
Tjeneren sender så en beskjed om *global commit* til alle klientene. Når klientene får denne beskjeden om betyr det at de selv skal commite hver for seg.

Slik ser det ut for henholdsvis tjener og klient 1:


| Server | Klient |
| --- | --- |
|![Image description](https://media.giphy.com/media/h2CIotR7wSUlcaGoqN/giphy.gif "Tjeneren fullfører transaksjonen")|![Image description](https://media.giphy.com/media/UTLcpVz402Gwpbyq4C/giphy.gif)|

I *Two-phase commit*-protokollen skal klienten commite og deretter sende en bekreftelse(acknowledgement) tilbake til tjeneren, når den får beskjed om global commit. 
I vår simulasjon av en transaksjon simuleres denne commiten ved at klienten trykker enter. Når dette gjøres er commiten gjennomført og det sendtes en acknowledgement-beskjed til tjeneren.
På samme måte som i første fase av protokollen, så må alle klientene sende em slikt bekreftelsesbeskjed til tjeneren for at transaksjonen er fullført. 
Dette skal forhåpentligvis ikke skje, men om en klient fjernes eller på en eller annen måte aldri får gjennomført commiten, så vil tjeneren vente en bestemt tid, før den gir beskjed til alle klientene om **abort**.



**Rollback**  
Om vi kjører samme eksempel, men denne gangen har vi tre klienter. 
I denne situasjonen så har Jonas og Erna fremdeles råd, og dermed "stemmer" for COMMIT, mens den nye klienten Sylvi har dessverre ikke råd og "stemmer" for ABORT. 
Når klientene da stemmer, vil systemet finne ut om de stemmer **ja** eller **nei** basert på saldoen deres. Og siden Sylvi ikke har råd vil hennes stemme være nei, som initialiserer en *global abort*.
Da vil tjeneren sende *global abort* til alle klientene, og de vil dermed kjøre abort metoden sin.
Transaksjonen blir dermed ikke utført. 


| Server | Klient |
| --- | --- |
|![Image description](https://i.imgur.com/cDpZ40d.png)|![Image description](https://i.imgur.com/Dax8u2V.png)|

Tjeneren vil vanligvis vente på svar fra alle klientene i den første fasen der det skal stemmes. Dersom en klient stemmer for ABORT vil tjeneren initialisere en global abort uansett. 
Tjeneren vil da ikke vente på svar fra resten av klientene, for i en two-phase commit protocol er det nok at én klient stemmer for abort. 
Enten må alle stemme JA, eller så skjer ingenting. 


**Logging**

Samtidig som alt dette skjer, blir også alle valgene til klientene lagret i en tekstfil som en logg. På denne måten kan man finne ut hva som skjer i transaksjonen, og hva som gikk galt om transaksjonen ikke ble fullført. 
Tjeneren sin logg er utskriften i terminalen, da det er tenkt at serveren alltid skal kjøre, og man kan finne historie på tidligere kjøringer der.
Når klientene skriver inn navn og kobler seg til vil det bli opprettet en tekstfil i deres navn. Dersom en klient kjører flere ganger, så utvides filen med ny data om de nye kjøringene.


I vår applikasjon så loggføres det:
- At klienten er tilkoblet og hva den har som saldo.
- Beskjeder fra tjeneren(VOTE_REQUEST, GLOBAL_COMMIT, ABORT).
- Lagring av gammel saldo
- Lagring av transaksjonen dersom den fullføres, det vil si trekk av penger.
- Acknowledgement.
- Klient er frakoblet.


Her ser vi loggene fra kjøringen:
Den første er Erna sin logg etter at transaksjonen ble gjennomført og alt gikk som det skulle:


![Image description](https://i.imgur.com/NiImQFZ.png "Loggen til Erna")


Dette er loggen til Sylvi etter at transaksjonen ble avbrutt fordi hun ikke hadde råd:


![Image description](https://i.imgur.com/NPi1Xkm.png "Loggen til Sylvi")|


<a name="installasjon"></a>
## Installasjonsinstruksjoner
### Tjener
For å installere tjeneren på en linux server:
1. Clone prosjektet til din linux maskin
2. Kjør *apt-get install default-jdk* for å installere java
3. Kjør *javac Tjener.java*
4. Kjør *java Tjener*

### Klient
For å kjøre klienten trenger du java installert
på din maskin. Deretter kan du feks. følge trinn 3-4
over i samme mappe som filen(og med Klient i stedet for Tjener), eller bruke en IDEA for å kjøre koden. 

<a name="testing"></a>
## Hvordan man kan teste løsningen
Det finnes flere måter å teste koden og løsningen på. Her er et par scenarier som du kan prøve
og teste:
- Opprette en tjener og en klient
- Opprette en tjener og en klient der en feil forekommer
- Opprette en tjener og to klienter der ingen feil forekommer
- Opprette en tjener og to klienter der en feil forekommer
- Gjenbrukbar tjener, der man utfører en eller flere av de øvrige punktene flere ganger
### Opprette en tjener og en klient
- Her opprettes en tjener og en klient.
- Klienten skriver inn navnet og saldo større enn beløpet som skal trekkes (5kr).
- Klienten trykker enter for å si at den er klart til å "committe".
- Klienten vil motta GLOBAL_COMMIT.
- Klienten trykker enter for å si at den har "committed" og "acknowledeger" til tjeneren.
- Klienten mottar en melding om at two phase er gjennomført.
- Alt av handlinger loggføres i en logg i navnet til klienten.
- Forbindelsen termineres.

### Opprette en tjener og en klient der en feil forekommer
- Her opprettes en tjener og en klient.
- Klienten skriver inn navnet og saldo mindre enn beløpet som skal trekkes (5kr).
- Klienten trykker enter for å si at den er klar til å "committe".
- Klienten vil motta GLOBAL_ABORT fordi saldo er mindre en beløpet som skal trekkes.
- Klienten vil aborte.
- Klienten mottar en melding om at two phase er gjennomført.
- Alt av handlinger loggføres i en logg i navnet til klienten.
- Forbindelsen termineres.

### Opprette en tjener og to klienter der ingen en feil forekommer
- Her opprettes en tjener og to klienter.
- Klient_1 skriver inn navn og saldo større enn beløpet som skal trekkes (5kr).
- Klient_1 trykker enter for å si at den er klart til å "committe".
- Klient_2 skriver inn navn og saldo større enn beløpet som skal trekkes (5kr).
- Klient_2 trykker enter for å si at den er klart til å "committe".
- Klient_1 vil motta GLOBAL_COMMIT.
- Klient_2 vil motta GLOBAL_COMMIT.
- Klient_1 responderer at den har "committed" og "acknowledeger" til tjeneren.
- Klient_2 responderer at den har "committed" og "acknowledeger" til tjeneren.
- Begge klientene mottar en melding om at two phase er gjennomført.
- Alt av handlinger loggføres i en logg i navnet til klienten.
- Forbindelsen termineres.

### Opprette en tjener og to klienter der en feil forekommer
- Her opprettes en tjener og to klienter.
- Klient_1 skriver inn navn og saldo større enn beløpet som skal trekkes (5kr).
- Klient_1 trykker enter for å si at den er klart til å "committe".
- Klient_2 skriver inn navn og saldo mindre enn beløpet som skal trekkes (5kr).
- Klient_2 trykker enter for å si at den er klart til å "committe".
- Klient_1 vil motta GLOBAL_ABORT.
- Klient_2 vil motta GLOBAL_ABORT.
- Klient_1 vil rulle tilbake (rollback) og hente sin lagrede saldo.
- Begge klientene mottar en melding om at two phase er gjennomført.
- Alt av handlinger loggføres i en logg i navnet til klienten.
- Forbindelsen termineres.

### Gjenbrukbar tjener, der man utfører en eller flere av de øvrige punktene flere ganger
- Gjennomfør et eller flere av punktene over om igjen.



<a name="api"></a>
## Eventuell lenke til API dokumentasjon
