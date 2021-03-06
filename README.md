# Prosjekt Nettverksprogrammering

Frivillig prosjektoppgave i Nettverksprogrammering.

Av
- Ian-Angelo Roman Evangelista
- Nikolai Preben Dokken
- Kasper Vedal Gundersen

## 


### Innhold
* [Introduksjon](#introduksjon)
* [Implementert funksjonalitet](#funksjonalitet)
    * [Tjener](#funksjonalitet_tjener)
    * [Klient](#funksjonalitet_klient)
    * [KlientTraad](#funksjonalitet_klientTraad)
    * [Loggforer](#funksjonalitet_loggforer)
* [Diskusjon](#diskusjon)
* [Teknologier](#teknologier)
* [Forbedringer](#forbedringer)
* [Eksempler](#eksempler)
* [Installasjon](#installasjon)
* [Testing](#testing)
* [CI](#biblioteker)
* [API](#api)



<a name="introduksjon"></a>
## Introduksjon

I dette prosjektet implementer vi en two-phase commit løsning.
Denne fungerer ved at alle tilkoblede klienter har en egen saldo 
og tjeneren spør alle klienter om det er greit å trekke et beløp fra hver klient.
Kliente må "stemme" om de kan gjennomføre beløpstrekket eller ikke. 
Saldoen til hver enkelt klient endres hvis alle kan gjennomføre trekket. Hvis ikke vil 
en klient som har gjort endringer, rulle tilbake til sin gamle saldo.
Alle parter må stemme før et valg er avgjort, men hvis en stemmer for at man ikke kan gjennomføre
tranaksjonen, vil hele two-phase commit avbrytes.

<a name="funksjonalitet"></a>
## Implementert funksjonalitet
### Klassediagram
![Image description](https://i.imgur.com/1UyJrre.png "Klassediagram") 

Siden prosjektet er inndelt i tjener og klient,
to deler i et distibuert system, ser vi også på
funksjonaliteten for disse hver for seg.

<a name="funksjonalitet_tjener"></a>
### Tjener
På tjenerside har vi først og fremst implementert
tråder slik at hver tilkobling til tjener kjører på
en egen tråd. Dermed sikrer vi at klienter ikke
begrenses av andre klienters tilkobling til tjeneren.

Videre bruker vi sockets for å lytte etter tilkoblinger
fra klient. I Java bruker vi ServerSocket for å lytte på
port 1111 etter klienter. Vi har implementert løsning for å kunne kjøre applikasjonen fra
både server som er hostet på NTNUs VM og lokalt via Localhost.
Her er den lokale varianten kommentert ut:
```java
Socket klientSocket = null;
        ServerSocket tjenerSocket = null;
        int port_number = 1111;
        Tjener tjener = new Tjener();
        try {
            // tjenerSocket = new ServerSocket(port_number); // Localhost
            tjenerSocket = new ServerSocket(); // server
            tjenerSocket.bind(new InetSocketAddress("129.241.96.153", 1111)); // server
            System.out.println("Serveren er startet...");
        } catch (IOException e) {
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
        // String host = "localhost"; // localhost
        try {
            InetAddress host= InetAddress.getByName("129.241.96.153"); // server
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
stemt. Det er tre ulike meldinger tjeneren kan sende til klienten, som blir forklart
mer nøyaktig under:
```java
try {
    while ((responseLinje = is.readLine()) != null) {
        System.out.println("\n"+responseLinje);
        if (responseLinje.indexOf("VOTE_REQUEST") != -1) {
                  // Se eksempelet under
            
        }
        if (responseLinje.equalsIgnoreCase("GLOBAL_ABORT")) {
                  // Se eksempelet under
            
        }
        if (responseLinje.equalsIgnoreCase("GLOBAL_COMMIT")) {
                  // Se eksmepelet under
            
        }
    }
    lukket=true;
} catch (IOException e) {
    System.err.println("IOException:  " + e);
}
```
Det første som blir sendt er den første delen av *Two-phase commit protocol*
som er VOTE_REQUEST. Det som skjer da er at klienten får spørsmål om den godtar at det trekkes penger fra saldoen.
Om man godtar dette så regner systemet ut om man har råd til transaksjonen og det blir sendt enten COMMIT eller ABORT tilbake til Tjeneren.
```java
   if (responseLinje.indexOf("VOTE_REQUEST") != -1) {
                    System.out.println("Trykk enter om du vil fortsette. ");
                    inputLinje.readLine();
                    belop = Integer.parseInt(responseLinje.split(":")[2]);
                    logg.loggfor("Fikk VOTE_REQUEST om å trekke " + belop + "kr.");
                    if (saldo >= belop) {
                        System.out.println("Du har nok på konto. Sender klarsignal til tjener. \nOm alle er klare gjennomføres COMMIT.");
                        os.println("COMMIT");
                        logg.loggfor("SAVE: Lagrer gammel saldo(kr): " + saldo);
                        logg.loggfor("Sender COMMIT til tjener.");
                        saldo -= belop;
                        gjordeEndringer = true;
                    } else {
                        System.out.println("Saldoen din er for lav. Sender ABORT til tjener");
                        os.println("ABORT");
                        logg.loggfor("Sender ABORT til tjener, har ikke raad.");
                    }
                }
```
Om en eneste klient ikke har råd vil Tjeneren sende en GLOBAL_ABORT
tilbake til alle klientene. Dersom klienten har stemt for COMMIT, vil den gjøre et *rollback*, hvis ikke avbrytes transaksjonen og det loggføres.
```java
if (responseLinje.equalsIgnoreCase("GLOBAL_ABORT")) {
                    logg.loggfor("Fikk beskjed om ABORT fra tjener");
                    if (gjordeEndringer) {
                        saldo = logg.getRollbackSaldo();
                        logg.loggfor("Rollback. Saldo er nå " + saldo + "kr");
                    }
                    break;
                }
```

Om alle klientene har råd sender Tjener GLOBAL_COMMIT. 
Klientene loggfører transaksjonen og utfører commiten. Deretter sendes det en bekreftelsesmelding(ACKNOWLEDGEMENT)
tilbake til Tjeneren. Her gjør vi en antagelse om at når klienten commiter, før den sender bekreftelsen:
```java
  if (responseLinje.equalsIgnoreCase("GLOBAL_COMMIT")) {
                    logg.loggfor("Fikk klarsignal(GLOBAL_COMMIT) fra tjener.");
                    logg.loggfor("Utførte transaksjon: [Opprinnelig beløp: " + (saldo+belop) + ", Transaksjonsbeløp: " + belop + ", Nytt beløp: " + saldo + "]");
                    System.out.println("Fikk GLOBAL_COMMIT: commiter, og sender ACKNOWLEDGE \ntil tjener.");
                    os.println("ACKNOWLEDGEMENT");
                    logg.loggfor("Sendte ACKNOWLEDGE til tjener.");
                    break;
                }
```

<a name="funksjonalitet_klientTraad"></a>
### KlientTraad
KlientTraad fungerer som bindeleddet mellom tjener og klient. For hver klient så opprettes
det en egen KlientTraad. Det her man både skriver til klient og leser responsen fra klient.
Denne kjører i loop så lenge ingen aborter eller hvis two-phase er gjennomført. Hvis en 
av de to nevnte forekommer, vil alle forbindelser lukkes.


Under ser man hva som skal skje hvis responsen fra en klient er ABORT:
```java
if (linje.equalsIgnoreCase("ABORT")) {                                                                             
    System.out.println("\nFra '" + klientIdentitet                                                                 
            + "' : ABORT\n\nSiden det ble skrevet ABORT, vil vi ikke vente paa flere input fra andre klienter.");  
    System.out.println("\nAborted...");                                                                            
                                                                                                                   
    while(tjener.traadListe.size() > 0) {                                                                          
        ((tjener.traadListe).get(0)).os.println("GLOBAL_ABORT");                                                   
        tjener.data.remove(tjener.traadListe.indexOf(tjener.traadListe.get(0)));                                   
        tjener.traadListe.remove(0);                                                                               
    }                                                                                                              
    break;                                                                                                         
}                                                                                                                  
```
Her vil den printe ut hvem som skrev ABORT og at man avslutter two-phase.
Alle trådene klientene mottar en GLOBAL_ABORT-melding og klientene og dataene fjernes fra listen fra listen.
Deretter avsluttes two-phase commit.

Under ser man hva som skal skje hvis responsen fra en klient er COMMIT:
```java
if (linje.equalsIgnoreCase("COMMIT")) {                                               
    System.out.println("\nFra '" + klientIdentitet + "' : COMMIT");                   
    if ((tjener.traadListe).contains(this)) {                                         
        (tjener.data).set((tjener.traadListe).indexOf(this), "COMMIT");               
        for (int j = 0; j < (tjener.data).size(); j++) {                              
            if (!(((tjener.data).get(j)).equalsIgnoreCase("NOT_SENT"))) {             
                tjener.inputFraAlle = true;                                           
                continue;                                                             
            } else {                                                                  
                tjener.inputFraAlle = false;                                          
                System.out.println("\nVenter paa input fra andre klienter.");         
                break;                                                                
            }                                                                         
        }                                                                             
                                                                                      
        if (tjener.inputFraAlle) {                                                    
            System.out.println("\n\nSending GLOBAL_COMMIT to all....");               
            for(int i = 0; i < tjener.traadListe.size(); i++) {                       
                ((tjener.traadListe).get(i)).os.println("GLOBAL_COMMIT");             
            }                                                                         
            tjener.data.clear();                                                      
        }                                                                             
    } // if traadListe.contains                                                       
}                                                                                     
```
Her vil den printe ut hvem som skrev COMMIT og sjekke om objektet faktisk finnes i listen av tråder.
Deretter vil man i sette klientens tilhørende element i data-listen fra "NOT_SENT" til "COMMIT".
Så sjekkes det om alle klienter er klare til å committe. Hvis alle er klare så sendes det
en GLOBAL_COMMIT til alle klienter.


Under ser man hva som skjer når responsen er ACKNOWLEDGEMENT fra en klient.
```java
if (linje.equalsIgnoreCase("ACKNOWLEDGEMENT")) {                                         
    tjener.traadListe.remove(tjener.traadListe.indexOf(this));                           
                                                                                         
    // Dersom alle har sendt acknowledge og koblet fra                                   
    if (tjener.traadListe.size() == 0) {                                                 
        System.out.println("MOTTAT ACK FRA ALLE KLIENTER, TWO PHASE COMMIT ER NAA OVER");
        tjener.data = new ArrayList<String>();                                           
        tjener.traadListe = new ArrayList<KlientTraad>();                                
        break;                                                                           
    } else {                                                                             
        System.out.println("\nVenter paa acknowledgement fra andre klienter.");          
        break;                                                                           
    }                                                                                    
}                                                                                        
```

Her fjernes klieneten som sendte ACKNOWLEGDEMENT fra klientlisten til tjeneren. 
Hvis alle har blitt fjernet vil alle ha sendt ACKNOWLEGDEMENT og two-phase commit er
gjennomført. Hvis ikke vil man fortsette å vente.

<a name="funksjonalitet_loggforer"></a>
### Loggforer
Denne klassen brukes når en klient skal loggføre handlingene sine.
Det opprettes en egen dedikert loggfører for hver klient. Hver logg får sin
egen fil i sitt oppgitte navn. Filen legges i mappen logger:
````java
public Loggforer(String navn) {
        this.filnavn = "logger/" + navn.toLowerCase() + ".txt";
        try {
            this.loggFil = new File(filnavn);
            this.loggFil.createNewFile();
            } catch(IOException e) {
            e.printStackTrace();
        }
    }
````


For å skrive til fil benyttes loggfor-metoden:
````java
public boolean loggfor(String loggforing) {
        Calendar kalender = Calendar.getInstance();
        Date dato = kalender.getTime();
        try {
            skriveForbindelse = new BufferedWriter(new FileWriter(filnavn, true));
            skriveForbindelse.write(dato + "," + loggforing + "\n");
            skriveForbindelse.close();
            return true;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }
````
Denne metoden tar inn en streng og henter inn dato og klokkeslett og skriver til fil.

For en klient skal finne tilbake til sin tidligere saldo hvis den har gjort endringer, vil
den måtte bruke getRollbackSaldo-metoden:
````java
public int getRollbackSaldo() {
        try {
            leseForbindelse = new BufferedReader(new FileReader(filnavn));
            String currentLine;
            String lagretSaldo = "";
            while ((currentLine = leseForbindelse.readLine()) != null) {
                if (currentLine.indexOf("SAVE") != -1) lagretSaldo = currentLine;
            }
            leseForbindelse.close();
            return Integer.parseInt(lagretSaldo.split(":")[4].trim());
        } catch(IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
````
Her vil den finne filen til klienten og finne siste linje der man skrev "SAVE" for å 
finne den lagrede saldoen og returnerer den.

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

Det er flere fordeler med *Two-phase commit protocol*, men også noen ulemper. Den store fordelen er jo at denne
atomiske protokollen skal sikre at alle deltagere i en transaksjon enten fullfører eller avbryter transaksjonen.
I tillegg funker protokollen i mange tilfeller der det skjer en midlertidig feil, som
for eksempel om prosess, nettverksnode eller kommunikasjon feiler. Like vel er ikke protokollen feilfri, og 
sliter dersom det er en svikt av både koordinatoren(tjener) og et klient i løpet av commit-fasen.

Dersom kun tjeneren mislykes, og ingen klienter mottar en commit-melding, kan det trygt utledes at ingen forpliktelser hadde skjedd. 
Hvis imidlertid både tjeneren og en klient mislyktes, er det mulig at det mislykkede klienten var den 
første som ble varslet, og faktisk hadde commitet. Selv om en ny tjener blir valgt, kan den ikke trygt fortsette
med operasjonen før den har mottatt en acknowledgement fra alle klientene, 
og må derfor blokkere til alle klientene svarer.
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
|![Image description](https://media.giphy.com/media/h1tvXrcXm6H7PPtqJZ/giphy.gif)|![Image description](https://media.giphy.com/media/j2GFYeYQwCGhHisM9C/giphy.gif)|

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

*Første kjøring*


Her ser vi loggen fra den første kjøringen:
Dette er Erna sin logg etter at transaksjonen ble gjennomført og alt gikk som det skulle.
Her ser vi at det logges både når klienten koblet seg til og fra. Samt kommunikasjonen mellom klienten og tjeneren i tillegg til operasjonene klienten gjorde:


![Image description](https://i.imgur.com/aEwlaFT.png "Loggen til Erna etter første kjøring")

*Andre kjøring*


Her ser vi både loggen til Erna og Sylvi etter at transaksjonen ble avbrutt. De er ikke helt like, og det er fordi Erna hadde nok penger og derfor sendte hun klar for commit til tjener. Men da Sylvi ikke hadde råd og tjener ble nødt til å sende *global abort* ser vi at Erna ble nødt til å gjøre et *rollback*.
Sylvi derimot var den som initialiserte aborten og derfor ble ikke hennes saldo lagret og det er derfor ingen behov for rollback fra hennes side. 

Dette er loggen til Erna og Sylvi etter at transaksjonen ble avbrutt:

**Erna Solberg** 

![Image](https://i.imgur.com/5Z6ibaX.png "Loggen til Erna etter abort") 


**Sylvi Listhaug**

![Image](https://i.imgur.com/VA9Gygb.png "Loggen til Sylvi etter abort")



<a name="installasjon"></a>
## Installasjonsinstruksjoner
I denne seksjonen finnes det to valg. Enten kan KlientMotServer.jar kjøres, eller så kan man kjøre Tjener og Klient lokalt.
Jar filen kobles til tjeneren som hostes på en NTNU vm(man må altså være koblet til NTNU sin VPN).
For å kjøre denne kjører man "java -jar KlientMotServer.jar" i prosjektmappen.

For å kjøre tjener og klient lokalt kjører man følgende kommandoer i prosjekt-mappen. (Koden kan også kjøres i en IDEA)

1. javac src/*.java
2. java src/Tjener
3. java src/Klient


<a name="testing"></a>
## Hvordan man kan teste løsningen?
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

<a name="biblioteker"></a>
## Continuous integration 
Lenke til oversikt over prosjektets CI:

https://gitlab.stud.idi.ntnu.no/nikolard/NettproggProsjekt/pipelines

<a name="api"></a>
## Lenker til API dokumentasjon

Lenke til prosjektets JavaDoc(må være koblet til NTNU nettverk for å kunne se):

[JavaDoc](http://folk.ntnu.no/nikolard/JavaDoc/allclasses.html)

Lenke til API brukt i prosjektet:


[Socket](https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html)

[Thread](https://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html)

[FileWriter](https://docs.oracle.com/javase/7/docs/api/java/io/FileWriter.html)

[FileReader](https://docs.oracle.com/javase/7/docs/api/java/io/FileReader.html)

[PrintStream](https://docs.oracle.com/javase/7/docs/api/java/io/PrintStream.html)

[DataInputStream](https://docs.oracle.com/javase/7/docs/api/java/io/DataInputStream.html)
