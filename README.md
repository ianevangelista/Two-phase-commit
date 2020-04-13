# Prosjekt Nettverksprogrammering
### Innhold
* [Biblioteker og CI](#biblioteker)
* [Introduksjon](#introduksjon)
* [Implementert funksjonalitet](#funksjonalitet)
    * [Server](#funksjonalitet_server)
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
I dette prosjektet bruker vi serverens navn som et eksempel.
Navet endres og alle tilkoblede parter må være enige om å beholde det nye navnet.
Dersom én er uenig ruller vi tilbake til det gamle servernavnet (rollback).
Alle parter må stemme før et valg er avgjort.

<a name="funksjonalitet"></a>
## Implementert funksjonalitet
Siden prosjektet er inndelt i server og klient,
to deler i et distibuert system, ser vi også på
funksjonaliteten for disse hver for seg.

<a name="funksjonalitet_server"></a>
### Server
På serversiden har vi først og fremst implementert
tråder slik at hver tilkobling til server kjører på
en egen tråd. Dermed sikrer vi at klienter ikke
begrenses av andre klienters tilkobling til serveren.

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
Serveren kjører så i loop og oppretter egne tråder
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
serveren ved hjelp av PrintStream(os) og 
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
Ved nye hendelser fra server sørger en run-metode
i klient for å plukke opp meldinger fra server i en loop
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
En beskrivelse og diskusjon/argumentasjon (denne delen en veldig viktig ved evaluering) av hvilke teknologi- og arkitektur-/designvalg dere har stått ovenfor (når dere skulle løse oppgaven), hva dere hadde å velge mellom og hvorfor dere har valgt det dere har valgt. Når det gjelder teknologivalg så kan denne delen begrenses til «pensum i faget».

<a name="teknologier"></a>
## Hvilke teknologier har vi brukt og hvorfor?
Hva hadde vi å velge mellom, hvorfor valgte vi som vi gjorde?

<a name="forbedringer"></a>
## Fremtidig arbeid med oversikt over mangler og mulige forbedringer

<a name="eksempler"></a>
## Eksempler som viser bruken av løsningen
Når man kjører Tjeneren får man beskjed om navnet til tjeneren er Per
![Image description](https://i.imgur.com/bdPlgIQ.png)

Når man kjører en klient får man følgende beskjed om å skrive inn navnet sitt. Når klienten gjør dette, får den informasjon om transaksjonen og får med en gang beskjed om en "vote request". Hos klient 1:
![Image description](https://i.imgur.com/NjhpKg9.png)

Om man kobler til en klient til vil både den første klienten og tjeneren få beskjed:
| Server | Klient |
| --- | --- |
|![Image description](https://i.imgur.com/d2GpKzC.png)|![Image description](https://i.imgur.com/zCPrrR3.png)|
   
Om en av klientene stemmer for å commite vil kun tjeneren se denne beskjeden:
| Server | Klient |
| --- | --- |
|![Image description](https://i.imgur.com/S5TUGIA.png)|![Image description](https://i.imgur.com/5yu1MAV.png)|

Hvis den andre klienten også stemmer for commit, har alle klientene stemt for commit, og transaksjonen gjennomføres. Klientene får beskjed om at det er blitt gjennomført en global commit. Tjeneren gir så tilbakemelding på at den har byttet navn, og heter nå Ola.
| Server | Klient |
| --- | --- |
|![Image description](https://i.imgur.com/n6t2m1V.png)|![Image description](https://i.imgur.com/uTgNbvd.png)|

<a name="installasjon"></a>
## Installasjonsinstruksjoner
### Server
For å installere serveren på en linux server:
1. Clone prosjektet til din linux maskin
2. Kjør "apt-get install default-jdk" for å installere java
3. Kjør "javac Tjener.java"
4. Kjør "java Tjener"

### Klient
For å kjøre klienten trenger du java installert
på din maskin. Deretter kan du feks. følge trinn 3-4
over i samme mappe som filen, eller bruke en IDEA for å kjøre koden. 

<a name="testing"></a>
## Hvordan man kan teste løsningen

<a name="api"></a>
## Eventuell lenke til API dokumentasjon
