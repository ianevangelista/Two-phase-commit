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

<a name="installasjon"></a>
## Installasjonsinstruksjoner

<a name="testing"></a>
## Hvordan man kan teste løsningen

<a name="api"></a>
## Eventuell lenke til API dokumentasjon
