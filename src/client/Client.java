package client;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import static client.ANSI.*;

public class Client {
    static Socket socket = null;
    static DataInputStream dataInputStream; // stream de date de la dispecer
    static DataOutputStream dataOutputStream; // stream de date catre dispecer
    final BufferedReader keyboardReader = new BufferedReader(new InputStreamReader(System.in)); // stream citire de la tastatura

    static String idClient = null; // id client
    static HashMap<String, ArrayList<String>> listaMesajeStocate; // memorare mesaje stocate din fisier

    static final String[] IND = {"1dCl13nt", "s4lv4r3", "d3z4b0n4r3", "4llF0r0n3"}; // indiciu de inclus in mesajul catre client ca acesta sa execute diferite operatiuni
    static final String SEP = "~"; // separator mesaj
    static final String CANCEL = "x"; // caracter anulare actiune
    static String path = "src/client/data/"; // locatie stocare date

    public Client(String address, int port) {
        // verifica existenta directorului
        if (Files.notExists(Path.of(path))) {
            path = "client/data/";
        }

        // initializare socket si streamuri de intrare / iesire
        try {
            socket = new Socket(address, port); // creare stream socket si conectare la adresa si port
            System.out.println(RESET + "Conectat la server dispecer");

            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

        } catch (IOException e) {
            System.out.println(e);
            inchideConexiune(false);
        }

        try {
            // pornire thread pentru ascultarea mesajelor primite de la server
            ReaderThread reader = new ReaderThread();
            reader.setDaemon(true);
            reader.start();
            String mesaj;

            // citire mesaje de la consola cat timp este conectat
            while (socket.isConnected()) {
                mesaj = keyboardReader.readLine(); // citire mesaj de la tastatura

                if (mesaj.equals("bye")) {
                    break; // inchidere conexiune la intalnirea mesajului "bye"

                } else if (mesaj.equals("listmes")) {
                    if (idClient != null) { // daca este autentificat
                        afisareMesajeStocate();
                    } else {
                        System.out.println(RED + "Neautorizat!" + YELLOW + "\n> ");
                    }

                } else if (mesaj.equals("delmes")) {
                    if (idClient != null) {
                        stergereMesaj();
                    } else {
                        System.out.println(RED + "Neautorizat!" + YELLOW + "\n> ");
                    }

                } else if (mesaj.equals("autodelmes")) {
                    if (idClient != null) {
                        autoStergereMesaj();
                    } else {
                        System.out.println(RED + "Neautorizat!" + YELLOW + "\n> ");
                    }

                } else if (mesaj.equals("editmes")) {
                    if (idClient != null) {
                        editareMesaj();
                    } else {
                        System.out.println(RED + "Neautorizat!" + YELLOW + "\n> ");
                    }

                } else {
                    dataOutputStream.writeUTF(mesaj); // trimite mesaj catre dispecer
                    log(mesaj);
                }
            }

            inchideConexiune(false); // inchide conexiunea la intalnirea lui "bye"

        } catch (IOException e) {
            System.out.println(e);
            inchideConexiune(false);
        }
    }

    // metoda pentru inchiderea conexiunilor si a streamurilor
    static void inchideConexiune(boolean eroareCitire) {
        try {
            if (dataInputStream != null) {
                dataInputStream.close(); // inchidere stream date de intrare
            }

            if (dataOutputStream != null) {
                dataOutputStream.close(); // inchidere stream date de iesire
            }

            if (socket != null) {
                socket.close(); // inchidere conexiune client
            }

            if (eroareCitire) {
                scrie("Nu se mai primesc mesaje!");
            } else {
                scrie("Conexiunea a fost inchisa!");
            }

            System.exit(1); // evita loop
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // metoda pentru citirea mesajelor stocate si adaugarea in lista
    static void getListaMesajeStocate() throws IOException {
        listaMesajeStocate = new HashMap<>();

        File fisier = new File(path + "mesaje_" + idClient + ".txt");
        fisier.createNewFile(); // creare fisier in caz ca nu exista
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fisier), StandardCharsets.UTF_8));
        String linie;

        while ((linie = reader.readLine()) != null) { // daca linia este nevida
            String[] date = linie.split(SEP); // tokenizare linie in string array
            ArrayList<String> dateMesaj = new ArrayList<>();
            dateMesaj.add(date[1]); // autor
            dateMesaj.add(date[2]); // categoria
            dateMesaj.add(date[3]); // continutul mesajului

            listaMesajeStocate.put(date[0], dateMesaj); // adaugare date din fisier in lista
        }

        reader.close(); // inchidere stream citire
    }

    // metoda pentru afisarea mesajelor stocate
    void afisareMesajeStocate() throws IOException {
        getListaMesajeStocate(); // actualizare lista mesaje stocate

        scrie("Lista mesajelor stocate (" + PURPLE + listaMesajeStocate.size() + RESET + " mesaje)");

        for (String hash : listaMesajeStocate.keySet()) { // obtinere cheie (hash)
            // afisare mesaj memorat la consola
            String mesajPrelucrat = "\n" + BLACK + YELLOW_BACKGROUND + listaMesajeStocate.get(hash).get(1) // categorie
                    + RESET + " Trimis de: " + YELLOW + listaMesajeStocate.get(hash).get(0) // autor
                    + RESET + " | Hash: " + YELLOW + hash + "\n"
                    + RESET + "Mesaj: " + YELLOW + listaMesajeStocate.get(hash).get(2);

            System.out.println(mesajPrelucrat);
            log(mesajPrelucrat);
        }

        System.out.print(YELLOW + "> ");
    }

    // metoda pentru stergerea unui mesaj stocat de client
    void stergereMesaj() throws IOException {
        getListaMesajeStocate(); // actualizare lista mesaje stocate

        scrie("Introdu hash-ul mesajului: " + YELLOW);
        String input = keyboardReader.readLine(); // citire hash de la tastatura

        // anulare actiune
        if (input.equals(CANCEL)) {
            scrie(YELLOW + "\n> ");
            return;
        }

        // verifica daca hash-ul precizat se afla in lista mesajelor stocate
        while (!listaMesajeStocate.containsKey(input)) {
            scrie(RED + "Acest mesaj nu este stocat!" + RESET + " Introdu alt hash: " + YELLOW);
            input = keyboardReader.readLine(); // recitire de la tastatura

            // anulare actiune
            if (input.equals(CANCEL)) {
                scrie(YELLOW + "\n> ");
                return;
            }
        }

        listaMesajeStocate.remove(input); // inlaturare mesaj

        // salvare in fisier
        File fisier = new File(path + "mesaje_" + idClient + ".txt");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fisier), StandardCharsets.UTF_8));

        for (String hash : listaMesajeStocate.keySet()) { // obtinere cheie (hash)
            // scriere mesaje in fisier
            writer.write(hash + SEP + // hash
                    listaMesajeStocate.get(hash).get(0) + SEP + // autor
                    listaMesajeStocate.get(hash).get(1) + SEP + // categorie
                    listaMesajeStocate.get(hash).get(2) + "\n"); // mesaj
        }

        writer.flush();
        writer.close();

        scrie("Mesajul " + PURPLE + input + RESET + " a fost sters!" + YELLOW + "\n> ");
    }

    // metoda pentru stergerea automata a unui mesaj dupa un anumit timp
    void autoStergereMesaj() throws IOException {
        getListaMesajeStocate(); // actualizare lista mesaje stocate

        scrie("Introdu hash-ul mesajului: " + YELLOW);
        String inputHash = keyboardReader.readLine(); // citire hash de la tastatura

        // anulare actiune
        if (inputHash.equals(CANCEL)) {
            scrie(YELLOW + "\n> ");
            return;
        }

        // verifica daca hash-ul precizat se afla in lista mesajelor stocate
        while (!listaMesajeStocate.containsKey(inputHash)) {
            scrie(RED + "Acest mesaj nu este stocat!" + RESET + " Introdu alt hash: " + YELLOW);
            inputHash = keyboardReader.readLine(); // recitire de la tastatura

            // anulare actiune
            if (inputHash.equals(CANCEL)) {
                scrie(YELLOW + "\n> ");
                return;
            }
        }

        scrie("Introdu numarul de secunde: " + YELLOW);
        String inputDelay; // input de la tastatura
        int sleepDelay; // numarul de secunde

        // verifica daca se introduce numar
        while (true) {
            try {
                inputDelay = keyboardReader.readLine();

                // anulare actiune
                if (inputDelay.equals(CANCEL)) {
                    scrie(YELLOW + "\n> ");
                    return;
                }

                sleepDelay = Integer.parseInt(inputDelay); // conversie la integer
                scrie("Mesajul " + PURPLE + inputHash + RESET +
                        " se va sterge in " + CYAN + sleepDelay + "s" + YELLOW + "\n> ");
                break; // iesire din while daca este numar

            } catch (NumberFormatException ex) {
                // daca nu este numar
                scrie(RED + "Nu ai introdus un numar!" + RESET + " Introdu numarul de secunde: " + YELLOW);
            }
        }

        // thread pentru stergerea automata a mesajului dupa o anumita perioada
        int finalSleepDelay = sleepDelay;
        String finalInputHash = inputHash;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(finalSleepDelay * 1000L);

                    getListaMesajeStocate(); // actualizare lista mesaje stocate
                    listaMesajeStocate.remove(finalInputHash); // inlaturare mesaj

                    // salvare in fisier
                    File fisier = new File(path + "mesaje_" + idClient + ".txt");
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fisier), StandardCharsets.UTF_8));

                    for (String hash : listaMesajeStocate.keySet()) { // obtinere cheie (hash)
                        // scriere mesaje in fisier
                        writer.write(hash + SEP + // hash
                                listaMesajeStocate.get(hash).get(0) + SEP + // autor
                                listaMesajeStocate.get(hash).get(1) + SEP + // categorie
                                listaMesajeStocate.get(hash).get(2) + "\n"); // mesaj
                    }

                    writer.flush();
                    writer.close();

                    scrie("Mesajul " + PURPLE + finalInputHash + RESET + " a fost " + PURPLE + "sters automat" + RESET + "!" + YELLOW + "\n> ");

                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // metoda pentru editarea unui mesaj stocat de client
    void editareMesaj() throws IOException {
        getListaMesajeStocate(); // actualizare lista mesaje stocate

        scrie("Introdu hash-ul mesajului: " + YELLOW);
        String inputHash = keyboardReader.readLine(); // citire hash de la tastatura

        // anulare actiune
        if (inputHash.equals(CANCEL)) {
            scrie(YELLOW + "\n> ");
            return;
        }

        // verifica daca hash-ul precizat se afla in lista mesajelor stocate
        while (!listaMesajeStocate.containsKey(inputHash)) {
            scrie(RED + "Acest mesaj nu este stocat!" + RESET + " Introdu alt hash: " + YELLOW);
            inputHash = keyboardReader.readLine(); // recitire de la tastatura

            // anulare actiune
            if (inputHash.equals(CANCEL)) {
                scrie(YELLOW + "\n> ");
                return;
            }
        }

        // citirea noului continut
        scrie("Introdu noul continut: " + YELLOW);
        String inputContinut = keyboardReader.readLine();

        // anulare actiune
        if (inputContinut.equals(CANCEL)) {
            scrie(YELLOW + "\n> ");
            return;
        }

        listaMesajeStocate.get(inputHash).set(2, inputContinut); // inlocuire mesaj

        // salvare in fisier
        File fisier = new File(path + "mesaje_" + idClient + ".txt");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fisier), StandardCharsets.UTF_8));

        for (String hash : listaMesajeStocate.keySet()) { // obtinere cheie (hash)
            // scriere mesaje in fisier
            writer.write(hash + SEP + // hash
                    listaMesajeStocate.get(hash).get(0) + SEP + // autor
                    listaMesajeStocate.get(hash).get(1) + SEP + // categorie
                    listaMesajeStocate.get(hash).get(2) + "\n"); // mesaj
        }

        writer.flush();
        writer.close();

        scrie("Mesajul " + PURPLE + inputHash + RESET + " a fost editat!" + YELLOW + "\n> ");
    }

    // metoda pentru afisarea formatata a mesajelor la consola
    static void scrie(String continut) throws IOException {
        System.out.print("\n" + BLACK + PURPLE_BACKGROUND + "Client" + RESET + " " + continut);
        log(continut); // adaugare in log
    }

    // metoda pentru scrierea in fisier a evenimentelor
    static void log(String cotinut) throws IOException {
        File fisier = new File(path + "log_" + idClient + ".txt");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fisier, true), StandardCharsets.UTF_8));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String mesajLog = "[" + sdf.format(new Timestamp(System.currentTimeMillis())) + "] " + cotinut + "\n"; // continutul de memorat
        mesajLog = mesajLog.replace(RESET, "").replace(BLACK, "").replace(RED, "").replace(YELLOW, "").replace(PURPLE, "").replace(CYAN, "").replace(WHITE, "").replace(YELLOW_BACKGROUND, "").replace(PURPLE_BACKGROUND, "").replace(CYAN_BACKGROUND, ""); // inlaturare caractere escape

        writer.write(mesajLog);
        writer.flush();
        writer.close(); // inchidere stream trimitere in fisier
    }

    public static void main(String args[]) {
        Client client = new Client("127.0.0.1", 5000);
    }
}