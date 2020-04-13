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
port 1111:
```java
import java.io.*;
import java.net.*;

public class Server {
    public static void main(String args[]) {
        int port_number = 1111;
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(port_number);
        } catch(IOException e) {
            System.out.println(e);
        } 
    }
}
```

<a name="funksjonalitet_klient"></a>
### Klient

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
