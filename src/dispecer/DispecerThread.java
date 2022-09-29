package dispecer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static dispecer.ANSI.*;
import static dispecer.Dispecer.*;

// thread pentru citirea mesajelor dispecerului de la tastatura
public class DispecerThread extends Thread {

    final BufferedReader keyboardReader = new BufferedReader(new InputStreamReader(System.in)); // stream citire de la tastatura

    public void run() {
        String input;
        try {
            while (true) {
                input = keyboardReader.readLine(); // citire mesaj de la tastatura

                switch (input) {
                    case "users":
                        afiseazaDateUtilizatori();
                        break;

                    case "online":
                        afiseazaUtilizatoriOnline();
                        break;

                    case "listcat":
                        afiseazaCategorii();
                        break;

                    case "listsub":
                        afiseazaAbonamente();
                        break;

//                    case "bye":
//                        inchideServer();
//                        break;

                    case "help":
                        System.out.print("Lista comenzilor:" +
                                "\n" + GREEN + "users" + RESET + "     Afisare date conturi" +
                                "\n" + GREEN + "online" + RESET + "    Afisare utilizatori online" + "\n" +

                                "\n" + CYAN + "listcat" + RESET + "   Afisare lista categorii" +
                                "\n" + CYAN + "listsub" + RESET + "   Afisare lista abonamente" + "\n" +

                                "\n" + RED + CANCEL + RESET + "         Anulare request" + YELLOW + "\n> ");
                        //"\n" + RED + "bye" + RESET + "       Inchidere server"

                        break;

                    default:
                        System.out.print(RED + "Comanda inexistenta!" + RESET + " Incearca comanda " + GREEN + "help" + YELLOW + "\n> ");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // metoda pentru afiseaza datelor utilizatorilor
    void afiseazaDateUtilizatori() throws IOException {
        getListaConturi(); // actualizare lista

        StringBuilder lista = new StringBuilder();
        for (String key : listaConturi.keySet()) {
            lista.append("\n").append(RESET + "Nume: " + CYAN).append(key).append(RESET).append(" Parola: ").append(CYAN).append(listaConturi.get(key));
        }

        System.out.print(RESET + "\nDatele conturilor stocate:" + lista + YELLOW + "\n> ");
    }

    // metoda pentru afisarea utilizatorilor online
    void afiseazaUtilizatoriOnline() {
        StringBuilder lista = new StringBuilder();
        for (String s : listaUtilizatoriConectati) {
            lista.append(CYAN).append(s).append(RESET).append(" | ");
        }

        System.out.print(RESET + "\nLista utilizatorilor conectati: " + lista + YELLOW + "\n> ");
    }

    // metoda pentru afisarea categoriilor
    void afiseazaCategorii() throws IOException {
        getListaCategorii(); // actualizare lista

        StringBuilder lista = new StringBuilder();
        for (String s : listaCategorii) {
            lista.append(CYAN).append(s).append(RESET).append(" | ");
        }

        System.out.print(RESET + "\nLista categoriilor: " + lista + YELLOW + "\n> ");
    }

    // metoda pentru afisarea abonamentelor tuturor clientilor
    void afiseazaAbonamente() throws IOException {
        getListaAbonamente(); // actualizare lista

        StringBuilder lista = new StringBuilder();
        StringBuilder abonamente = new StringBuilder();

        for (String nume : listaAbonamente.keySet()) {
            for (String abonament : listaAbonamente.get(nume)) { // parcurge abonamentele unui utilizator
                abonamente.append(CYAN).append(abonament).append(RESET).append(" | ");
            }

            lista.append("\n").append(RESET + "Utilizator: " + CYAN).append(nume).append(RESET).append(" Abonamente: ").append(abonamente);
            abonamente = new StringBuilder(); // resetare lista abonamente
        }

        System.out.print(RESET + "\nLista abonamentelor:" + lista + YELLOW + "\n> ");
    }

    // metoda pentru inchiderea serverului
    static void inchideServer() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
