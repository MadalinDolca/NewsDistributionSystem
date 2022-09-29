package client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static client.ANSI.*;
import static client.Client.*;

// thread pentru ascultarea si afisarea mesajelor primite de la server dispecer
class ReaderThread extends Thread {
    public boolean active;

    public ReaderThread() {
        active = true;
    }

    public void run() {
        // asculta mesaje de la server cat timp este conectat
        while (socket.isConnected()) { // while(active)
            try {
                String mesaj = dataInputStream.readUTF(); // citire mesaj de la server dispecer
                String[] mesajDescompus = mesaj.split(SEP); // descompunere mesaj

                if (mesajDescompus[0].equals(IND[0])) { // daca contine indiciu ID client
                    idClient = mesajDescompus[1]; // memorare id client

                } else if (mesajDescompus[0].equals(IND[1])) { // daca contine indiciu pentru a-l memora
                    inregistrareMesajPartajat(mesajDescompus); // afisare mesaj prelucrat si memorare

                } else if (mesajDescompus[0].equals(IND[2])) { // daca contine indiciu pentru stergerea mesajelor dintr-o categorie
                    stergeMesajeCategorie(mesajDescompus); // stergere mesaje din categorie

                } else if (mesajDescompus[0].equals(IND[3]) && !mesajDescompus[2].equals(idClient)) { // daca contine indiciu pentru solicitarea mesajelor dintr-o categorie din partea unui client si difera de acest client
                    transmiteMesajeCategorie(mesajDescompus); // raspunde solicitarii prin trimiterea mesajelor

                } else {
                    System.out.print(mesaj); // afisare mesaj de la server
                    log(mesaj); // adaugare in log
                    //System.out.print("> ");
                }
            } catch (IOException e) {
                active = false;
                inchideConexiune(true);
            }
        }
    }

    // metoda pentru afisarea si memorarea in fisier a mesajului partajat descompus
    void inregistrareMesajPartajat(String[] mesajDescompus) throws IOException {
        String emitator = mesajDescompus[1];
        String hash = mesajDescompus[2];
        String categorie = mesajDescompus[3];
        String mesaj = mesajDescompus[4];

        // afisare mesaj prelucrat
        String mesajPrelucrat = "\n" + BLACK + CYAN_BACKGROUND + categorie
                + RESET + " Trimis de: " + CYAN + emitator
                + RESET + " | Hash: " + CYAN + hash + "\n"
                + RESET + "Mesaj: " + CYAN + mesaj;
        System.out.println(mesajPrelucrat);
        log(mesajPrelucrat);

        // salvate mesaj
        File fisier = new File(path + "mesaje_" + idClient + ".txt");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fisier, true), StandardCharsets.UTF_8));
        writer.write(hash + SEP + emitator + SEP + categorie + SEP + mesaj + "\n");
        writer.flush();
        writer.close();
    }

    // metoda pentru stergerea mesajelor stocate care apartin unei categorii
    void stergeMesajeCategorie(String[] mesajDescompus) throws IOException {
        getListaMesajeStocate(); // actualizare lista mesaje stocate
        ArrayList<String> listaCheiMesaje = new ArrayList<>(); // memorare chei mesaje din categoria dorita pentru eliminare

        // parcurgere lista mesaje stocate pentru obtinere chei
        for (String key : listaMesajeStocate.keySet()) {
            // daca mesajul apartine categoriei se memoreaza cheia
            if (listaMesajeStocate.get(key).get(1).equals(mesajDescompus[1])) {
                listaCheiMesaje.add(key); // memorare cheie
            }
        }

        // parcurgere lista chei pentru stergerea mesajelor
        for (String key : listaCheiMesaje) {
            listaMesajeStocate.remove(key); // stergere mesaj
        }

        // salvare modificari in fisier
        File fisier = new File(path + "mesaje_" + idClient + ".txt");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fisier), StandardCharsets.UTF_8));

        for (String key : listaMesajeStocate.keySet()) { // obtinere cheie (hash)
            // scriere mesaje in fisier
            writer.write(key + SEP + // hash
                    listaMesajeStocate.get(key).get(0) + SEP + // autor
                    listaMesajeStocate.get(key).get(1) + SEP + // categorie
                    listaMesajeStocate.get(key).get(2) + "\n"); // mesaj
        }

        writer.flush();
        writer.close();

        scrie("Mesajele din categoria " + PURPLE + mesajDescompus[1] + RESET + " au fost sterse!" + PURPLE + "\n> ");
    }

    // metoda pentru transmiterea mesajelor stocate care apartin unei categorii solicitantului
    void transmiteMesajeCategorie(String[] mesajDescompus) throws IOException {
        String categorie = mesajDescompus[1];
        String solicitant = mesajDescompus[2];
        int numarMesaje = 0;

        scrie("Ai primit o solicitare de mesaje de la " + PURPLE + solicitant + RESET + " pentru categoria " + PURPLE + categorie + RESET);
        getListaMesajeStocate(); // actualizare lista mesaje stocate

        for (String key : listaMesajeStocate.keySet()) { // obtinere cheie (hash)
            if (listaMesajeStocate.get(key).get(1).equals(categorie)) { // daca mesajul apartine categoriei
                numarMesaje++;
                String raspunsSolicitare = IND[3] + SEP + solicitant + SEP + IND[1] + SEP + idClient + SEP + key + SEP + categorie + SEP + listaMesajeStocate.get(key).get(2); // mesajul de transmis clientului
                dataOutputStream.writeUTF(raspunsSolicitare); // trimitere mesaj
            }
        }

        scrie("Ai raspuns solicitarii lui " + PURPLE + solicitant + RESET
                + " pentru categoria " + PURPLE + categorie + RESET
                + " cu " + PURPLE + numarMesaje + RESET
                + " mesaje" + YELLOW + "\n> ");
    }
}