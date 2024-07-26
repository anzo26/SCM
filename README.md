# Simple Contact Manager - MMA

<p align="center">
  <img alt="SCM Logo" width="400" src="https://github.com/mihaprah/projekt/assets/116807398/1d171956-57b7-49b9-93d9-3525148c70d7" >
</p>

SCM oziroma Simple Contact Manager, je spletna aplikacija namenjena upravljanju s kontakti. Njena glavna prednost pred ostalimi CRM (Customer Relation Management) programi, je njena preposta uporaba, saj je namenjena izključno delu s kontakti.
Ta datoteka, bo služila kot predstavitev projekta, navodila za namestitev aplikacije lokalno, uporaba aplikacije na spletu (za namene predstavitve) in dokumentacija za aplikacijo SCM.
### Člani ekipe MMA
- [Miha Prah](https://github.com/mihaprah)
- [Matija Krsnik](https://github.com/Matija334)
- [Anže Golob](https://github.com/anzo26)

## Kazalo vsebine projekta
1. [Predstavitev projekt](#1-predstavitev-projekta)
    - [Podroben opis projekta](#podroben-opis-projekta)
2. [Navodila za namestitev lokalno](#3-navodila-za-namestitev-lokalno)
    - [Testno lokalno okolje](#testno-lokalno-okolje)
3. [Uporaba aplikacije](#4-uporaba-aplikacije)
    - [Uporabniški priročnik](#uporabniški-priročnik)
    - [Končni izgled aplikacije](#končni-izgled-aplikacije)


## 1. Predstavitev projekta
### Podroben opis projekta
Načrtovati, nadgraditi, implementirati in namestiti bo potrebno sistem, ki bo uporabnikom omogočal vodenje kontaktov na nekem projektu ali v nekem podjetju. Skupino kontaktov lahko uporabniki kreirajo sami, kontakti naj bodo vidni le znotraj te skupine; poskrbeti bo torej treba za izolacijo podatkov med različnimi uporabniki in njihovimi projekti.
Podatki o uporabnikih naj bodo do določene mere predefinirani, omogočeno pa naj bo tudi poljubno definiranje kontaktov v smislu ključ-vrednost. Pričakuje se označevanje kontaktov s poljubnimi značkami, ki bodo služile kot kategorije kontaktov. Voditi bo potrebno tudi vse spremembe kontaktov (revizijska sled) in jih tudi primerno verzionirati. Dostop do zalednega sistema naj bo mogoč preko ustrezno zavarovanega REST vmesnika.
Uporabniški vmesnik naj nudi prijazen pregled, iskanje, filtriranje ter izvoz označenih kontaktov v
format MS Excel. Konkurenčna prednost izdelka pred konkurenčnimi CRM sistemi bo torej ravno
enostavnost uporabe.

Povezava do glavnega repozitorija [SCM - povezava](https://github.com/mihaprah/projekt/tree/main)

## 2. Navodila za namestitev lokalno

#### Koraki za zagon

###### 1. Kloniranje repozitorija

Najprej klonirajte repozitorij na vašo lokalno napravo.

###### 2. Pridobitev firebase serviceAccountKey.json datoteke za backend

Za konfiguracijo Firebase je potrebno pridobiti **serviceAccountKey.json** 

1. Pojdite na [Firebase Console](https://firebase.google.com).
2. Izberite svoj projekt.
3. V stranski vrstici kliknite na Project Settings (Nastavitve projekta).
4. Izberite zavihek Service accounts (Storitveni računi).
5. Izberite Java in kliknite na gumb Generate new private key (Ustvari nov zasebni ključ). To bo preneslo datoteko **serviceAccountKey.json** na vaš računalnik.

Ustvarjeno datoteko kopirajte v **projekt_local/backend/scm/src/main/resources**

###### 3. Konfiguracija docker compose

V datoteki **docker-compose.yml**, ki se nahaja v projekt_local/ uredite naslednjo vrstico
```bash
SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017******Your database name goes here****** 
```
tukaj navedite poljubno ime za podatkovno bazo

###### 4. Zagon docker compose

Postavite se v mapo **SCM**, kjer se nahaja datoteka **docker-compose.yml** in izvedite ukaz 
```bash
docker-compose up --build
```

###### 5. Pridobitev firebase SDK za frontend

1. Pojdite na [Firebase Console](https://firebase.google.com).
2. Izberite svoj projekt.
3. V stranski vrstici kliknite na Project Settings (Nastavitve projekta).
4. Izberite zavihek General.

Generirane vrednosti prekopirajte v **SCM/frontend/scm/.env**

###### 6. Zagon frontenda

Postavite se v mapo SCM/frontend/scm in izvedite naslednje ukaze:
```bash
npm install
npm run build
npm run start
```

## 3. Uporaba aplikacije

### Uporabniški priročnik
Uporabniški priročnik, je namenjen novim uporabnikom, da se seznanijo z delovanjem aplikacije. V njem so v 6. kategorijah zajete vse funkcionalnosti aplikacije, ki jih lahko uporablja uporabnik. Kategorije so sledeče:
1. Prijava/Registracija
2. Skupina (Tenant)
3. Kontakt
4. Uvoz/Izvod kontaktov
5. Delo z večimi kontakti hkrati
6. Filtririranje

Kategorije imajo tudi podkategorije, ki podrobno razložijo, kaj mora uporabnik storiti in kakšni so pogoji, da bo akcija uspešna. Uporabniški priročnik se nahaja v mapi [**documentation/user-manual.pdf**](https://github.com/mihaprah/projekt/blob/main/documentation/user-manual.pdf).

### Končni izgled aplikacije
Spodaj so prikazani nekateri končni zaslonski posnetki aplikacije. Vsi posnetki so na voljo v mapi [**documentation/app-photos**](https://github.com/mihaprah/projekt/tree/main/documentation/app-photos).

<p align="center">
  <img alt="dashboard" width="800" src="https://github.com/mihaprah/projekt/assets/111431985/66186e0e-8c1b-4564-8fc5-c88f944df5c5">
  <br/>
  Nadzorna plošča, kjer so vidne vse skupine (Tenanti), ki jih je uporabnik naredil oziroma je v njih vključen.
  
</p>
<p align="center">
  <img alt="tenant-list-view" width="800" src="https://github.com/mihaprah/projekt/assets/111431985/3c0065ec-61af-4a70-9538-9bdb0ffc5904">
  <br/>
  <img alt="tenant-grid-view" width="800" src="https://github.com/mihaprah/projekt/assets/111431985/0e9adb36-6701-4a02-9741-ebbca9a24b98">
  <br/>
  Pregled vseh kontaktov za ene skupine (Tenant). Pogled je nastavljen na <b>seznam</b> ali na <b>mrežo</b>.
</p>
