package dispecer;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static dispecer.ANSI.*;
import static dispecer.Dispecer.*;

class ClientThread extends Thread {
    private Socket clientSocket; // socketul clientului
    private DataInputStream dataInputStream; // stream de date de la client
    private DataOutputStream dataOutputStream; // stream de date catre client
    public String idClient = null; // id-ul clientului curent
    String mesajNeautorizat = RED + "Neautorizat!" + RESET + " Trebuie sa te autentifici!" + YELLOW + "\n> ";

    public ClientThread(Socket clientSocket) throws IOException {
        // initializare socket si streamuri de intrare / iesire
        try {
            this.clientSocket = clientSocket;
            dataInputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            //listaThreaduri.add(this); // adaugare thread in lista
        } catch (IOException e) {
            inchideConexiune(clientSocket, dataInputStream, dataOutputStream);
        }
    }

    public void run() {
        try {
            // mesaj intampinare client
            trimite("Bun venit!" +
                    "\nPentru înregistrare trimite " + GREEN + "register" + RESET +
                    "\nPentru autentificare trimite " + GREEN + "auth" + YELLOW + "\n> ");

            // asculta mesaje de la client cat timp clientul este conectat
            while (clientSocket.isConnected()) {
                try {
                    String mesajClient;
                    mesajClient = citeste(); // citire mesaj de la client
                    String[] mesajDescompus = mesajClient.split(SEP); // descompunere mesaj de la client

                    if (mesajClient.equals("bye")) {
                        break; // citeste mesaje de la client pana la intalnirea lui "bye"
                    }

                    if (mesajDescompus[0].equals(IND[3])) { // daca contine indiciu solicitare-transmitere
                        trimiteRaspunsSolicitare(mesajDescompus); // metoda intoarcere raspuns la solicitare mesaje categorie
                    } else {
                        switch (mesajClient) {
                            case "register":
                                inregistrare();
                                break;

                            case "auth":
                                autentificare();
                                break;

                            case "online":
                                if ((idClient != null)) {
                                    afisareListaUtilizatoriConectati();
                                } else {
                                    trimite(mesajNeautorizat);
                                }
                                break;

                            case "usersub":
                                if ((idClient != null)) {
                                    afisareAbonamenteUtilizator();
                                } else {
                                    trimite(mesajNeautorizat);
                                }
                                break;

                            case "listcat":
                                if (idClient != null) {
                                    afisareListaCategorii();
                                } else {
                                    trimite(mesajNeautorizat);
                                }
                                break;

                            case "addcat":
                                if (idClient != null) {
                                    adaugaCategorie();
                                } else {
                                    trimite(mesajNeautorizat);
                                }
                                break;

                            case "subcat":
                                if (idClient != null) {
                                    abonareCategorie();
                                } else {
                                    trimite(mesajNeautorizat);
                                }
                                break;

                            case "unsubcat":
                                if (idClient != null) {
                                    dezabonareCategorie();
                                } else {
                                    trimite(mesajNeautorizat);
                                }
                                break;

                            case "getnews":
                                if (idClient != null) {
                                    solicitaMesajeCategorie();
                                } else {
                                    trimite(mesajNeautorizat);
                                }
                                break;

                            case "addmes":
                                if (idClient != null) {
                                    adaugaMesaj();
                                } else {
                                    trimite(mesajNeautorizat);
                                }
                                break;

                            case "bye":
                                trimite("Te-ai deconectat. La revedere!"); // trimitere mesaj catre client
                                break;

                            case "help":
                                trimite("Lista comenzilor:" +
                                        "\n" + GREEN + "register" + RESET + "   Inregistrare cont nou" +
                                        "\n" + GREEN + "auth" + RESET + "       Autentificare in sistem" + "\n" +

                                        "\n" + CYAN + "online" + RESET + "     Afisare utilizatori online" +
                                        "\n" + CYAN + "usersub" + RESET + "    Afiseaza abonamentele unui utilizator" + "\n" +

                                        "\n" + GREEN + "listcat" + RESET + "    Afisare lista categorii si abonamente" +
                                        "\n" + GREEN + "addcat" + RESET + "     Adaugare categorie noua" +
                                        "\n" + GREEN + "subcat" + RESET + "     Abonare la o categorie" +
                                        "\n" + GREEN + "unsubcat" + RESET + "   Dezabonare categorie si stergere mesaje aferente" +
                                        "\n" + GREEN + "getnews" + RESET + "    Solicita mesaje dintr-o categorie de la ceilalti clienti" + "\n" +

                                        "\n" + YELLOW + "listmes" + RESET + "    Afisare mesaje stocate (Client)" +
                                        "\n" + YELLOW + "addmes" + RESET + "     Adaugare mesaj (Server + Client)" +
                                        "\n" + YELLOW + "delmes" + RESET + "     Stergere mesaj stocat (Client)" +
                                        "\n" + YELLOW + "autodelmes" + RESET + " Stergere mesaj stocat dupa o anumita perioada de timp (Client)" +
                                        "\n" + YELLOW + "editmes" + RESET + "    Editare mesaj stocat (Client)" + "\n" +

                                        "\n" + RED + CANCEL + RESET + "          Anulare request" +
                                        "\n" + RED + "bye" + RESET + "        Iesire din sistem (Client)" + YELLOW + "\n> ");
                                break;

                            default:
                                trimite(RED + "Comanda inexistenta!" + RESET + " Incearca comanda" + GREEN + " help" + YELLOW + "\n> ");
                        }
                    }
                } catch (IOException e) {
                    // inchideConexiune(clientSocket, dataInputStream, dataOutputStream);
                    break; // cand clientul se deconecteaza atunci se iese din while
                }
            }

            // inchidere conexiune client la intalnirea lui "bye"
            inchideConexiune(clientSocket, dataInputStream, dataOutputStream);

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // metoda pentru eliminarea threadului unui client si inchiderea conexiunilor si a streamurilor
    void inchideConexiune(Socket clientSocket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
        System.out.println(BLACK + RED_BACKGROUND + "Deconectare" + RESET + " Inchidere conexiune client: " + CYAN + idClient + RESET + " | " + CYAN + clientSocket.getInetAddress() + RESET + ":" + CYAN + clientSocket.getPort() + RESET);
        partajareMesaj("Utilizatorul " + CYAN + idClient + RESET + " s-a " + RED + "deconectat" + RESET + "!" + YELLOW + "\n> "); // instiintare utilizatori
        listaUtilizatoriConectati.remove(idClient); // sterge utilizator curent din lista utilizatorilor conectati
        listaThreaduri.remove(this); // sterge threadul curent din lista

        try {
            if (dataInputStream != null) {
                dataInputStream.close(); // inchidere stream date de intrare
            }

            if (dataOutputStream != null) {
                dataOutputStream.close(); // inchidere stream date de iesire
            }

            if (clientSocket != null) {
                clientSocket.close(); // inchidere conexiune client
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // metoda pentru inregistrarea a unui nou utilizator
    void inregistrare() throws IOException {
        getListaConturi(); // adaugare conturi din fisier in hashmap

        String nume;
        String parola;

        trimite("Nume utilizator dorit: " + YELLOW);
        nume = citeste(); // obtinere nume utilizator de la client

        // anulare request
        if (nume.equals(CANCEL)) {
            trimite(YELLOW + "\n> ");
            return;
        }

        // verificare daca exista deja acest nume de utilizator
        while (listaConturi.containsKey(nume)) {
            trimite(RED + "Nume de utilizator existent!" + RESET + " Introdu alt nume: " + YELLOW);
            nume = citeste(); // recitire nume

            // anulare request
            if (nume.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        // daca numele este disponibil, se cere parola
        trimite("Parola dorita: " + YELLOW);
        parola = citeste(); // obtinere parola de la client

        // anulare request
        if (parola.equals(CANCEL)) {
            trimite(YELLOW + "\n> ");
            return;
        }

        // verificare lungime parola
        while (parola.length() < 2) {
            trimite(RED + "Parola este prea scurta! (min 2)" + RESET + " Introdu alta parola: " + YELLOW);
            parola = citeste(); // recitire parola

            // anulare request
            if (parola.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        // daca datele sunt corecte, se memoreaza in fisier
        File fisier = new File(path + "conturi.txt");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fisier, true), StandardCharsets.UTF_8));
        writer.write(nume + SEP + parola + "\n");
        writer.flush();
        writer.close();

        trimite("Cont creat cu succes! Pentru autentificare trimite " + GREEN + "auth" + YELLOW + "\n> ");
    }

    // metoda pentru autenficarea unui utilizator in sistem si instiintarea celorlalti utilizatori
    void autentificare() throws IOException {
        getListaConturi(); // adaugare conturi din fisier in hashmap

        String nume;
        String parola;

        trimite("Nume utilizator: " + YELLOW);
        nume = citeste(); // obtinere nume utilizator de la client

        // anulare request
        if (nume.equals(CANCEL)) {
            trimite(YELLOW + "\n> ");
            return;
        }

        // verificare lungime nume
        while (nume.length() < 1) {
            trimite(RED + "Nu ai introdus nici un nume!" + RESET + " Reintrodu numele: " + YELLOW);
            nume = citeste(); // recitire nume

            // anulare request
            if (nume.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        // verificare daca exista acest nume de utilizator
        while (!listaConturi.containsKey(nume)) {
            trimite(RED + "Nume de utilizator inexistent!" + RESET + " Reintrodu numele: " + YELLOW);
            nume = citeste(); // recitire nume

            // anulare request
            if (nume.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        // daca numele exista, se cere parola
        trimite("Parola: " + YELLOW);
        parola = citeste(); // obtinere parola de la client

        // anulare request
        if (parola.equals(CANCEL)) {
            trimite(YELLOW + "\n> ");
            return;
        }

        // verificare corectitudine parola
        while (!listaConturi.get(nume).equals(parola)) {
            trimite(RED + "Parola nu corespunde acestui cont!" + RESET + " Reintrodu parola: " + YELLOW);
            parola = citeste();

            // anulare request
            if (parola.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        // verifica daca utilizatorul este deja conectat
        if (listaUtilizatoriConectati.contains(nume)) {
            trimite(RED + "Utilizatorul este deja conectat!" + RESET + " Reincearca " + GREEN + "auth" + YELLOW + "\n> ");
            return;
        }

        // daca datele sunt corecte se permite accesul in sistem
        trimite("Te-ai autentificat cu succes, " + CYAN + nume + RESET + "!" + YELLOW + "\n> ");
        idClient = nume; // setare id client curent

        dataOutputStream.writeUTF(IND[0] + SEP + idClient); // trimite id catre client
        log(0, IND[0] + SEP + idClient); // adaugare in log

        listaUtilizatoriConectati.add(idClient); // adaugare client curent in lista de utilizatori conectati
        partajareMesaj("Utilizatorul " + CYAN + idClient + RESET + " s-a " + GREEN + "conectat" + RESET + "!" + YELLOW + "\n> "); // instiintare utilizatori
    }

    // metoda pentru afisarea utilizatorilor conectati in sistem
    void afisareListaUtilizatoriConectati() throws IOException {
        StringBuilder lista = new StringBuilder();
        for (String s : listaUtilizatoriConectati) {
            lista.append(CYAN).append(s).append(RESET).append(" | ");
        }

        trimite("Lista utilizatorilor conectati: " + lista + YELLOW + "\n> ");
    }

    // metoda pentru afisarea abonamentelor unui utilizator
    void afisareAbonamenteUtilizator() throws IOException {
        trimite("Introdu numele utilizatorului: " + YELLOW);
        String raspuns = citeste();

        // anulare request
        if (raspuns.equals(CANCEL)) {
            trimite(YELLOW + "\n> ");
            return;
        }

        getListaConturi(); // actualizare lista conturi

        // verifica daca utilizatorul exista
        while (!listaConturi.containsKey(raspuns)) {
            trimite(RED + "Acest utilizator nu exista!" + RESET + " Trimite alt nume: " + YELLOW);
            raspuns = citeste();

            // anulare request
            if (raspuns.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        getListaAbonamente(); // actualizare lista abonamente

        StringBuilder lista = new StringBuilder();
        if (listaAbonamente.containsKey(raspuns)) {
            for (String abonament : listaAbonamente.get(raspuns)) { // parcurge abonamentele unui utilizator
                // construieste lista de abonamente alte utilizatorului
                lista.append(CYAN).append(abonament).append(RESET).append(" | ");
            }
            trimite("Abonamentele utilizatorului " + CYAN + raspuns + RESET + ": " + lista + YELLOW + "\n> ");
        } else {
            trimite("Utilizatorul " + CYAN + raspuns + RESET + " nu este abonat la nimic!" + YELLOW + "\n> ");
        }
    }

    // metoda pentru afisarea categoriilor din sistem si ale abonamentelor personale
    void afisareListaCategorii() throws IOException {
        getListaCategorii(); // actualizare lista categorii

        // construire string din categorii pentru afisare
        StringBuilder stringCategorii = new StringBuilder();
        for (String s : listaCategorii) {
            stringCategorii.append(CYAN).append(s).append(RESET).append(" | ");
        }

        // construire string din categoriile la care utilizatorul s-a abonat
        getListaAbonamente(); // actualizare lista utilizatori si abonari categorii

        StringBuilder stringAbonari = new StringBuilder();
        ArrayList<String> listaAbonari = listaAbonamente.get(idClient);
        if (listaAbonari != null) {
            for (String s : listaAbonari) {
                stringAbonari.append(CYAN).append(s).append(RESET).append(" | ");
            }
        }

        trimite("Lista categoriilor: " + stringCategorii +
                "\nLista categoriilor la care te-ai abonat: " + stringAbonari + YELLOW + "\n> ");
    }

    // metoda pentru adaugarea unei categorii in fisier
    void adaugaCategorie() throws IOException {
        trimite("Introdu numele categoriei: " + YELLOW);
        String raspuns = citeste();

        // anulare request
        if (raspuns.equals(CANCEL)) {
            trimite(YELLOW + "\n> ");
            return;
        }

        while (raspuns.length() < 3) {
            trimite(RED + "Numele categoriei este prea scurt! (min 3 caractere)" + RESET + " Reintrodu numele: " + YELLOW);
            raspuns = citeste();

            // anulare request
            if (raspuns.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        while (listaCategorii.contains(raspuns)) {
            trimite(RED + "Aceasta categorie exista deja!" + RESET + " Introdu alta categorie: " + YELLOW);
            raspuns = citeste();

            // anulare request
            if (raspuns.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        // daca datele sunt corecte, se memoreaza in fisier
        File fisier = new File(path + "categorii.txt");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fisier, true), StandardCharsets.UTF_8));
        writer.write(raspuns + "\n");
        writer.flush();
        writer.close();

        trimite("Categoria a fost adaugata! Pentru a vizualiza categoriile trimite " + GREEN + "listcat" + YELLOW + "\n> ");
    }

    // metoda pentru abonarea unui client la o categorie
    void abonareCategorie() throws IOException {
        trimite("Introdu numele categoriei la care doresti sa te abonezi: " + YELLOW);
        String raspuns = citeste();

        // anulare request
        if (raspuns.equals(CANCEL)) {
            trimite(YELLOW + "\n> ");
            return;
        }

        getListaCategorii(); // actualizare lista categorii

        // verifica daca categoria exista
        while (!listaCategorii.contains(raspuns)) {
            trimite(RED + "Aceasta categorie nu exista!" + RESET + " Introdu alta categorie: " + YELLOW);
            raspuns = citeste();

            // anulare request
            if (raspuns.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        getListaAbonamente(); // actualizare lista abonamente

        // daca utilizatorul nu se afla in lista cu abonamente
        if (listaAbonamente.get(idClient) == null) {
            listaAbonamente.put(idClient, new ArrayList<>()); // adaugare in lista pentru a-l putea abona
        }

        while (listaAbonamente.get(idClient).contains(raspuns)) { // verifica daca deja este abonat la categoria respectiva
            trimite(RED + "Esti deja abonat la aceasta categorie!" + RESET + " Introdu alta categorie: " + YELLOW);
            raspuns = citeste(); // recitire

            // anulare request
            if (raspuns.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        listaAbonamente.get(idClient).add(raspuns); // adauga abonament utilizator in hashmap

        // scriere utilizatori si categoriile la care s-au abonat in fisier
        File fisier = new File(path + "abonamente.txt");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fisier), StandardCharsets.UTF_8)); // inlocuire date, fara append

        for (String nume : listaAbonamente.keySet()) { // obtinere cheie
            StringBuilder stringCategorii = new StringBuilder();

            for (String categorie : listaAbonamente.get(nume)) { // obtine categoriile din cheia respectiva
                stringCategorii.append(categorie).append(SEP); // construire string categorii
            }

            writer.write(nume + SEP + stringCategorii + "\n"); // scriere in fisier
        }

        writer.flush();
        writer.close(); // inchidere stream trimitere in fisier
        trimite("Te-ai abonat la aceasta categorie! Pentru a vizualiza categoriile trimite " + GREEN + "listcat" + YELLOW + "\n> ");
    }

    // metoda pentru dezabonarea clientului de la o categorie si stergerea mesajelor stocate din categoria respectiva
    void dezabonareCategorie() throws IOException {
        getListaAbonamente(); // actualizare lista abonamente

        // daca utilizatorul nu se afla in lista cu abonamente se incheie metoda
        if (!listaAbonamente.containsKey(idClient)) {
            trimite(RED + "Nu esti abonat la nici o categorie! " + RESET + "Trimite " + CYAN + "subcat" + RESET + " pentru abonare" + YELLOW + "\n> ");
            return;
        }

        trimite("Introdu numele categoriei de la care doresti sa te dezabonezi: ");
        String raspuns = citeste();

        // anulare request
        if (raspuns.equals(CANCEL)) {
            trimite(YELLOW + "\n> ");
            return;
        }

        getListaAbonamente(); // actualizare lista abonamente

        // verifica daca este abonat la respectiva categorie
        while (!listaAbonamente.get(idClient).contains(raspuns)) {
            trimite(RED + "Nu esti abonat la aceasta categorie!" + RESET + " Introdu alta categorie: " + YELLOW);
            raspuns = citeste();

            // anulare request
            if (raspuns.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        listaAbonamente.get(idClient).remove(raspuns); // stergere abonament din lista

        // salvare modificari in fisier abonamente
        File fisier = new File(path + "abonamente.txt");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fisier), StandardCharsets.UTF_8)); // inlocuire date, fara append

        for (String nume : listaAbonamente.keySet()) { // obtinere cheie
            StringBuilder stringCategorii = new StringBuilder();

            for (String categorie : listaAbonamente.get(nume)) { // obtine categoriile din cheia respectiva
                stringCategorii.append(categorie).append(SEP); // construire string categorii
            }

            writer.write(nume + SEP + stringCategorii + "\n"); // scriere in fisier
        }

        writer.flush();
        writer.close(); // inchidere stream trimitere in fisier

        trimite("Te-ai dezabonat de la categoria " + CYAN + raspuns);
        dataOutputStream.writeUTF(IND[2] + SEP + raspuns); // trimitere mesaj pentru stergerea mesajelor stocate care apartin categoriei
        log(0, IND[2] + SEP + raspuns); // adaugare in log
    }

    // metoda pentru solicitarea mesajelor stocate care apartin unei categorii din partea celorlalti clienti
    void solicitaMesajeCategorie() throws IOException {
        trimite("Introdu numele categoriei: " + YELLOW);
        String raspuns = citeste();

        // anulare request
        if (raspuns.equals(CANCEL)) {
            trimite(YELLOW + "\n> ");
            return;
        }

        getListaAbonamente(); // actualizare lista abonamente

        // daca utilizatorul nu se afla in lista cu abonamente se incheie metoda
        if (!listaAbonamente.containsKey(idClient)) {
            trimite(RED + "Nu esti abonat la nici o categorie! " + RESET + "Trimite " + CYAN + "subcat" + RESET + " pentru abonare" + YELLOW + "\n> ");
            return;
        }

        // verifica daca utilizatorul este abonat la aceasta categorie
        while (!listaAbonamente.get(idClient).contains(raspuns)) {
            trimite(RED + "Nu esti abonat la aceasta categorie!" + RESET + " Introdu alta categorie: " + YELLOW);
            raspuns = citeste(); // recitire

            // anulare request
            if (raspuns.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        trimite("S-a trimis solicitarea ta celorlalti utilizatori conectati pentru categoria " + CYAN + raspuns + YELLOW + "\n> ");
        getListaAbonamente(); // actualizare lista abonamente
        String mesajSolicitare = IND[3] + SEP + raspuns + SEP + idClient; // mesajul care se va transmite clientilor

        // parcurge fiecare thread client si partajeaza mesaje in functie de abonament
        for (ClientThread clientThread : listaThreaduri) {
            if (listaAbonamente.get(clientThread.idClient).contains(raspuns) && !clientThread.idClient.equals(idClient)) { // daca clientul respectiv este abonat categoriei si difera de clientul curent
                clientThread.dataOutputStream.writeUTF(mesajSolicitare); // trimite mesaj partajat tuturor celorlalti abonati conectati
                log(0, mesajSolicitare); // adaugare in log
            }
        }
    }

    // metoda pentru redirectionarea raspunsului clientilor la solicitarea unui client de a primii mesajele celorlalti clienti dintr-o categorie
    void trimiteRaspunsSolicitare(String[] mesajDescompus) throws IOException {
        // parcurge fiecare thread client pana la gasirea solicitantului
        for (ClientThread clientThread : listaThreaduri) {
            if (clientThread.idClient.equals(mesajDescompus[1])) { // daca acesta este solicitantul
                String raspunsSolicitare = mesajDescompus[2] + SEP + mesajDescompus[3] + SEP + mesajDescompus[4] + SEP + mesajDescompus[5] + SEP + mesajDescompus[6];

                clientThread.dataOutputStream.writeUTF(raspunsSolicitare); // trimite raspunsul la solicitare
                log(0, raspunsSolicitare); // adaugare in log
            }
        }
    }

    // metoda pentru adaugarea si partajarea unui mesaj cu hash
    void adaugaMesaj() throws IOException {
        String categorie;
        String mesaj;
        String hash;

        trimite("Introdu categoria: " + YELLOW);
        categorie = citeste();

        // anulare request
        if (categorie.equals(CANCEL)) {
            trimite(YELLOW + "\n> ");
            return;
        }

        getListaCategorii(); // actualizare lista categorii

        // verifica daca categoria exista
        while (!listaCategorii.contains(categorie)) {
            trimite(RED + "Aceasta categorie nu exista!" + RESET + " Introdu alta categorie: " + YELLOW);
            categorie = citeste(); // recitire

            // anulare request
            if (categorie.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        getListaAbonamente(); // actualizare lista abonamente

        // verifica daca este abonat la aceasta categorie
        while (!listaAbonamente.get(idClient).contains(categorie)) {
            trimite(RED + "Nu esti abonat la aceasta categorie!" + RESET + " Introdu alta categorie: " + YELLOW);
            categorie = citeste(); // recitire

            // anulare request
            if (categorie.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        trimite("Introdu mesajul: " + YELLOW);
        mesaj = citeste();

        // anulare request
        if (mesaj.equals(CANCEL)) {
            trimite(YELLOW + "\n> ");
            return;
        }

        while (mesaj.length() < 3) {
            trimite(RED + "Mesajul este prea scurt! Minim 3 caractere." + RESET + " Reintrodu mesajul: " + YELLOW);
            mesaj = citeste(); // recitire mesaj

            // anulare request
            if (mesaj.equals(CANCEL)) {
                trimite(YELLOW + "\n> ");
                return;
            }
        }

        hash = codificare(mesaj);
        //trimite("Ai trimis mesajul: " + CYAN + mesaj + RESET + " | Hash: " + CYAN + hash + YELLOW + "\n> ");

        partajareMesajCategorie(hash, categorie, mesaj);
    }

    // metoda pentru partajarea unui mesaj tuturor celorlalti clienti
    void partajareMesaj(String mesaj) throws IOException {
        for (ClientThread clientThread : listaThreaduri) { // parcurgere threaduri clienti
            if (clientThread.idClient != null && !clientThread.idClient.equals(idClient)) { // daca este autentificat si nu este clientul curent
                clientThread.trimite(mesaj);// trimite mesaj
            }
        }
    }

    // metoda pentru partajarea mesajelor intre clienti in functie de abonamente (cu indiciu)
    void partajareMesajCategorie(String hash, String categorie, String mesaj) throws IOException {
        String mesajPartajat = IND[1] + SEP + idClient + SEP + hash + SEP + categorie + SEP + mesaj; // mesajul de partajat clientilor

        getListaAbonamente(); // actualizare lista abonamente

        // parcurge fiecare thread client si partajeaza mesaje in functie de abonament
        for (ClientThread clientThread : listaThreaduri) {
            if (listaAbonamente.get(clientThread.idClient).contains(categorie)) { // daca clientul respectiv este abonat categoriei
                clientThread.dataOutputStream.writeUTF(mesajPartajat); // trimite mesaj partajat tuturor celorlalti abonati conectati
                log(0, mesajPartajat); // adaugare in log
            }
        }
    }

    // metoda pentru trimiterea mesajelor si adaugarea acestora in log
    void trimite(String continut) throws IOException {
        dataOutputStream.writeUTF("\n" + BLACK + GREEN_BACKGROUND + "Dispecer" + RESET + " " + continut);
        log(0, continut); // adaugare in log
    }

    // metoda pentru citirea mesajelor primite si adaugarea acestora in log
    String citeste() throws IOException {
        String continut = dataInputStream.readUTF();
        log(1, continut); // adaugare in log
        return continut;
    }

    // metoda pentru codificare continut prin metoda MD5
    String codificare(String data) {
        String hash;

        MessageDigest md = null; // instanta pentru MD5

        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        md.update(data.getBytes(StandardCharsets.UTF_8)); // adaugare bytes continut pentru digest
        byte[] bytes = md.digest(); // obtinere bytes hash

        // conversie bytes din decimal in hexadecimal
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        // obtinere hash complet in format hex
        hash = sb.toString();

        return hash;
    }

    // metoda pentru scrierea in fisier a mesajelor sosite si transmise
    void log(int directie, String cotinut) throws IOException {
        File fisier = new File(path + "log.txt"); // locatie fisier
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fisier, true), StandardCharsets.UTF_8));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // format timestamp

        String idTemp = idClient;
        String mesajLog; // mesajul log de inregistrat
        String textRuta = " X ";

        if (idTemp == null) { // daca clientul este neautentificat
            idTemp = clientSocket.getInetAddress() + ":" + clientSocket.getPort();
        }

        if (directie == 0) { // output
            textRuta = " Catre ";
        } else if (directie == 1) { // input
            textRuta = " De la ";
        }

        mesajLog = "[" + sdf.format(new Timestamp(System.currentTimeMillis())) + "]" + textRuta + CYAN + idTemp + RESET + ": " + cotinut + "\n";
        System.out.print(mesajLog); // afisare la consola

        mesajLog = mesajLog.replace(RESET, "").replace(BLACK, "").replace(RED, "").replace(GREEN, "").replace(YELLOW, "").replace(CYAN, "").replace(GREEN_BACKGROUND, ""); // inlaturare caractere escape
        writer.write(mesajLog); // scriere in fisier

        writer.flush();
        writer.close(); // inchidere stream trimitere in fisier
    }
}