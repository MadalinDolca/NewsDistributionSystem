package dispecer;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static dispecer.ANSI.*;

public class Dispecer {
    public static ServerSocket serverSocket = null; // initializare server socket
    public static ArrayList<ClientThread> listaThreaduri = new ArrayList<>(); // lista pentru memorarea threadurilor socketurilor pentru clienti
    public static HashMap<String, String> listaConturi; // lista conturilor inregistrate
    public static ArrayList<String> listaCategorii = new ArrayList<>(); // lista categorii memorate
    public static HashMap<String, ArrayList<String>> listaAbonamente = new HashMap<>(); // lista utilizatori si categoriile la care s-au abonat
    public static ArrayList<String> listaUtilizatoriConectati = new ArrayList<>(); // lista utilizatorilor conectati in sistem

    static final String[] IND = {"1dCl13nt", "s4lv4r3", "d3z4b0n4r3", "4llF0r0n3"}; // indiciu de inclus in mesajul catre client ca acesta sa execute diferite operatiuni
    static final String SEP = "~"; // separator mesaj
    static final String CANCEL = "x"; // caracter pentru anulare request
    static String path = "src/dispecer/data/"; // locatie stocare date

    public Dispecer(int port) {
        // verifica existenta directorului
        if (Files.notExists(Path.of(path))) {
            path = "dispecer/data/";
        }

        // porneste serverul si asteapta conexiuni
        try {
            serverSocket = new ServerSocket(port); // deschidere server socket
            System.out.println(GREEN + "Server pornit! " + RESET + "Port: " + serverSocket.getLocalPort());
            System.out.println("Asteptare client...");

            // thread pentru citirea mesajelor dispecerului de la tastatura
            DispecerThread dispecerThread = new DispecerThread();
            dispecerThread.start();

            // asteptare conexiune din partea clientului
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept(); // asteapta conexiunile la socket si le accepta
                System.out.println(GREEN + "Client conectat! " + RESET + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

                ClientThread clientThread = new ClientThread(clientSocket); // alocare thread pentru fiecare client conectat
                listaThreaduri.add(clientThread); // adaugare thread in lista
                clientThread.start(); // lansare thread pentru clientul conectat
            }

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    // metoda citire inregistrari din fisier linie cu linie si tokenizare linie in functie de delimitator
    static void getListaConturi() throws IOException {
        listaConturi = new HashMap<>();
        File fisier = new File(path + "conturi.txt");
        fisier.createNewFile(); // creare fisier in caz ca nu exista
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fisier), StandardCharsets.UTF_8));
        String linie;

        while ((linie = reader.readLine()) != null) { // daca linia este nevida
            String[] date = linie.split(SEP); // tokenizare linie in string array
            listaConturi.put(date[0], date[1]); // adaugare date array in hashmap lista conturi
        }

        reader.close(); // inchidere stream citire
    }

    // metoda pentru adaugarea categoriilor din fisier in array list
    static void getListaCategorii() throws IOException {
        File fisier = new File(path + "categorii.txt");
        fisier.createNewFile(); // creare fisier in caz ca nu exista
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fisier), StandardCharsets.UTF_8));
        String linie;

        listaCategorii.clear(); // golire lista curenta

        while ((linie = reader.readLine()) != null) { // daca linia este nevida
            listaCategorii.add(linie); // adauga date din fisier in array list
        }

        reader.close();
    }

    // metoda pentru obtinerea tuturor membrilor si a categoriile la care s-au abonat
    static void getListaAbonamente() throws IOException {
        File fisier = new File(path + "abonamente.txt");
        fisier.createNewFile(); // creare fisier in caz ca nu exista
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fisier), StandardCharsets.UTF_8));
        String linie;

        while ((linie = reader.readLine()) != null) { // daca linia este nevida
            String[] date = linie.split(SEP); // tokenizare linie in string array
            ArrayList<String> categorii = new ArrayList<>();

            for (int i = 1; i < date.length; i++) {
                categorii.add(date[i]); // adaugare categorii in arraylist
            }

            listaAbonamente.put(date[0], categorii); // adaugare date array in hashmap lista abonamente
        }

        reader.close();
    }

    public static void main(String[] args) {
        Dispecer dispecer = new Dispecer(5000);
    }
}