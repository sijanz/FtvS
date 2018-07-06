// Übungen zur Vorlesung "Fehlertolerante verteilte Systeme" im SS 2018.
// Aufgabe 2: Ausbreitung von Rücksetzlinien.
package Uebung02;

import SoFTlib.*;

import static SoFTlib.Helper.*;

import java.util.*;


//------------------------------------------------------------------------------
abstract class FtKnoten extends Node
// Knoten, der eine Anwendung ausführt.
{ // Für die Statistik:
    int anzInitRL;   // Anzahl der initiierten RL.
    //
    // *** Ggf. weitere Variablen des Knotens ***
    //
    static int globalRPscore = 0;
    int RPscore; // RP number

    public String runNode(String input) throws SoFTException {
        anzInitRL = 0;            // Noch keine RL initiiert.
        initialisierung(input);   // Anwendung startet.
        lauf();                   // Anwendung läuft.
        return terminierung();    // Knoten beendet seine Arbeit.
        //
        // *** Diese Methode ggf. modifizieren ***
        //
    }

    // Die Anwendung wird durch eine Unterklasse realisiert. In dieser werden
    // die Arbeitsschritte fortlaufend nummeriert (Variable schritt), wobei in
    // jedem Schritt eine Zustandsvariable (zustand) fortgeschaltet wird.

    abstract void initialisierung(String input) // Initialisiere die Anwendung.
            throws SoFTException;

    abstract void lauf() throws SoFTException;  // Anwendung wird ausgeführt.

    abstract int schritt();                     // Lies Variable schritt.

    abstract int zustand();                     // Lies Variable zustand.

    abstract void setzeSchritt(int schritt);    // Schreibe Variable schritt.

    abstract void setzeZustand(int zustand);    // Schreibe Variable zustand.

    // Die folgenden Methoden werden von der Unterklasse Anwendung aufgerufen:

    protected void sende(String inhalt, char empfaenger)
            throws Anlauf, SoFTException
    // Operator, den die Anwendung aufruft, um zunächst lokale Arbeit zu ver-
    // richten und dann eine Nachricht zu senden. Typ 'n' bedeutet: Nutznachricht
    // der Anwendung.
    {
        lokaleArbeit();
        form('n', inhalt + " " + RPscore).send(empfaenger);

        //
        // *** Diese Methode ggf. modifizieren ***
        //

    }

    // Operator, den die Anwendung aufruft, um eine Nachricht zu empfangen.
    protected String empfange(char sender) throws Anlauf, SoFTException {

        boolean flag = true;

        while (flag) {

            Msg msgE = receive("E", time() + 1);
            while (msgE != null) {

                switch (msgE.getTy()) {
                    case 'n':
                        return msgE.getCo();               // Nutznachricht wurde empfangen.
                    case 'f':

                        // @debug
                        say("It's freezing outside");

                        // inform E thread is frozen
                        form('c', time()).send('E');

                        // wait for all threads to freeze and info on reset
                        msgE = receive("E", never());

                        // empty messages from old run
                        emptyInbox();

                        //TODO
                        // reset
                        setzeSchritt(number(msgE.getCo(), 1));
                        setzeZustand(number(msgE.getCo(), 2));


                        // @debug
                        say("unfrozen");
                }

                msgE = receive("E", time() + 1);
            }

            Msg msg = receive(sender, never());  // Empfang ohne Zeitschranke.
            if (msg != null) {

                int score = number(msg.getCo(), 3);

                switch (msg.getTy()) {
                    case 'n':
                        //sender has a new RP-status
                        if (score > RPscore) {
                            RPscore = score;
                            initiateRP(schritt() + " " + zustand());
                            //this node has a never RP-status
                        } else if (score < RPscore) {

                            //send a "copy" to E
                            form('a', msg.getSe() + " " + time() + " " + msg.getCo()).send("E");

                            //form('u', RPscore).send(msg.getSe());
                        }
                        return msg.getCo();               // Nutznachricht wurde empfangen.
                    case 'u':
                        if (score > RPscore) {
                            RPscore = score;
                            initiateRP(schritt() + " " + zustand());

                            //this node has a never RP-status
                        }
                        break;
                }
            } else {
                flag = false;
            }
        }
        return "";
        //
        // *** Diese Methode ggf. modifizieren ***
        //
    }

    private void emptyInbox() throws SoFTException {
        Msg msg = receive("ABCD", time() + 1);
        while (msg != null) {
            msg = receive("ABCD", time() + 1);
        }
    }

    protected void initiiereRL(String inhalt, boolean anlaufend)
            throws Anlauf, SoFTException
    // Operator, den die Anwendung aufruft, um eine Rücksetzlinie zu initiieren.
    // Im Moment des Zurücksetzens wird die RL, auf die zurückgesetzt
    // wurde (angezeigt durch anlaufend == true), nicht sofort wieder erstellt.
    { //
        // *** Diese Methode ggf. modifizieren ***
        //
        if (!anlaufend) {
            RPscore = ++globalRPscore;
            form('r', inhalt + " " + globalRPscore + " " + time()).send('E');

        }
    }

    protected void initiateRP(String content) throws SoFTException {
        form('r', content + " " + RPscore + " " +  time()).send('E');
    }

    protected void lokaleArbeit()
            throws Anlauf, SoFTException
    // Lokale Arbeit der Anwendung, die 12 Millisekunden Zeit benötigt.
    {
        Msg naEmpf = receive(myChar(), time() + 12);
        //
        // *** Diese Methode ggf. modifizieren ***
        //
    }

    protected void fehlermeldung()
            throws Anlauf, SoFTException
    // Sende eine Fehlermeldung bei erkanntem Fehler an den RL-Verwalter.
    {
        form('f', schritt() + " ::::: Fehlermeldung :::::").send('E');
    }

    //
    // *** Ggf. weitere Methoden des Knotens ***
    //

    String terminierung() throws SoFTException
    // Terminierungs-String auf Fehlertoleranz-Ebene bilden.
    {
        String a = (anzInitRL < 10 ? "  " + anzInitRL
                : (anzInitRL < 100 ? " " + anzInitRL : "" + anzInitRL));
        return ",  " + a + " RL initiiert";
    }
}

//
// *** Ggf. weitere Klassen des Knotens ***
//


//------------------------------------------------------------------------------
class Anwendung extends FtKnoten
// Simuliertes Anwendungsprogramm in einem Knoten.
//
// <<<<< Diese Klasse darf nicht verändert werden >>>>>
//
{
    int schritt,    // Zählt die durchgeführten Arbeitsschritte.
            zustand;    // Zustandszahl, die einen Anwendungszustand simuliert.
    boolean anlaufend;  // Anlaufen nach dem Zurücksetzen.
    double erfassung;  // Fehlererfassung des anwendungsabhängigen Absoluttests.
    Injektor injektor;   // Fehlerinjektor.

    // Die folgenden Methoden werden von der Oberklasse FtKnoten aufgerufen:

    void initialisierung(String input) throws SoFTException
    // Initialisierung der Variablen des Anwendungsprogramms.
    {
        schritt = 0;  // Noch kein Arbeitsschritt ausgeführt.
        zustand = 10000;  // Anfangszustand.
        erfassung = real(input, getItemCount(input), 1);
        injektor = new Injektor(myChar(), input);
        RPscore = 0;
    }

    void lauf() throws SoFTException
    // Ausführung des (simulierten) Anwendungsprogramms.
    {
        while (schritt < Ftvs18_Aufg2.korrektEndSchritt) {
            String seInhalt = schritt + " " + zustand,
                    emInhalt = null;
            int emZustand = 10000;
            try {
                if (sender() == myChar()) sende(seInhalt, empfaenger());
                else if (empfaenger() == myChar()) emInhalt = empfange(sender());
                else if (initiator() == myChar()) initiiereRL(seInhalt, anlaufend);
                else lokaleArbeit();
                if (emInhalt != null)  // Lies ggf. empfangene Nachricht.
                {
                    int emSchritt = number(emInhalt, 1, 1);
                    if (emSchritt == schritt) emZustand = number(emInhalt, 1, 2);
                }
                anlaufend = false;
                fortschritt(emZustand);              // Neuer Zustand der Anwendung.
                if (absoluttest()) fehlermeldung();  // Prüfe den neuen Zustand.
            } catch (Anlauf abbruch)    // Nach dem Abbruch wurde die Anwendung zurueck-
            {
                anlaufend = true;  // gesetzt und läuft nun wieder an.
            }
        }
    }

    int schritt() {
        return schritt;
    }

    int zustand() {
        return zustand;
    }

    void setzeSchritt(int schritt) {
        this.schritt = schritt;
    }

    void setzeZustand(int zustand) {
        this.zustand = zustand;
    }

    // Die folgenden Methoden sind lokal in der Klasse Anwendung:

    private void fortschritt(int emZustand)
    // Der Fortschritt des Anwendungsprogramms wird zu Beginn jedes Arbeits-
    // schritts dadurch simuliert, dass ein neuer Zustandswert aus dem bis-
    // herigen Zustandswert und dem Inhalt emZustand einer im vorangehenden
    // Arbeitsschritt empfangenen Nachricht pseudozufällig berechnet wird.
    // Der Fortschritt unterliegt der Fehlerinjektion.
    {
        int i = myIndex() + 1, z = zustand - 10000;
        schritt++;
        emZustand -= 10000;
        int fehler = injektor.fehlerinjektion(schritt);
        if (fehler > 0)
            say(schritt + " ---------- Fehler ---------- Verfälschung " + fehler);
        zustand = ((3 * i + 38) * z + 293 * (i + 3) + emZustand + fehler) % (293 * 293)
                + 10000;
    }

    private int fall()
    // In Abhängigkeit vom momentanen Arbeitsschritt wird pseudozufällig einer
    // von 15  Fällen ausgewählt (0 bis 14). Die Methoden sender(), empf() und
    // initiator() bestimmen, welche Aktion in dem betreffenden Fall ausgeführt
    // wird (niemals mehrere Aktionen gleichzeitig).
    {
        int x = (schritt + 5) % 293,
                y = (2 + x * (schritt + 1)) % 293,
                z = ((1 + y * schritt) % 293) % 31;
        if (z == 0) return 10;
        else return (z - 1) / 2;
    }

    private char sender()
    // In Abhängigkeit vom momentanen Fall wird der Sender einer Nachricht
    // bestimmt (-1, wenn keine Nachricht).
    {
        int[] s = {-1, 0, 0, 1, 2, 3, 2, 2, 1, -1, -1, -1, -1, 1, -1};
        return nodeChr(s[fall()]);
    }

    private char empfaenger()
    // In Abhängigkeit vom momentanen Fall wird der Empfänger einer Nachricht
    // bestimmt (-1, wenn keine Nachricht).
    {
        int[] e = {-1, 1, 2, 2, 3, 2, 1, 0, 0, -1, -1, -1, -1, 2, -1};
        return nodeChr(e[fall()]);
    }

    private char initiator()
    // In Abhängigkeit vom momentanen Fall wird der Initiator einer
    // Rücksetzlinie bestimmt (-1, wenn keine RL).
    {
        int[] i = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, 2};
        return nodeChr(i[fall()]);
    }

    private boolean absoluttest()
    // Dieser anwendungsabhängige Absoluttest liefert true, wenn ein Fehler
    // erkannt wird. Die Fehlererfassung wird durch die Variable erfassung
    // festgelegt (0 < erfassung <= 1).
    {
        int abweichung = (zustand - 10000) % 293;
        return 0 < abweichung && abweichung <= (int) (292 * erfassung);
    }

    // Die folgende Methode erweitert die Terminierungsmethode der Oberklasse:

    String terminierung() throws SoFTException
    // Terminierung an den RL-Verwalter melden und
    // Terminierungs-String auf Anwendungs-Ebene bilden.
    {
        form('t', schritt + " terminiert").send('E');
        boolean korrekt = zustand == Ftvs18_Aufg2.korrektEndZustand[myIndex()];
        return injektor.anzFehler + " Fehler injiziert" + super.terminierung()
                + ",  letzter Schritt " + schritt + ",  Endzustand " + zustand
                + (korrekt ? "  korrekt" : "  falsch");
    }
}


//------------------------------------------------------------------------------
class Anlauf extends Exception {
}
// Wenn ein Knoten vom RL-Verwalter zurückgesetzt wird, hält er diesen
// zunächst an, so dass die von der Anwendung aufgerufene Methode keinen
// Rückgabewert liefert, sondern die Anwendung erneut anläuft.


//------------------------------------------------------------------------------
class Injektor
// Fehlerinjektor, der Anwendungsdaten verfälschen kann.
//
// <<<<< Diese Klasse darf nicht verändert werden >>>>>
//
{
    class Injektion
            // Injektion eines Fehlers in einen Knoten in einem bestimmten Schritt.
            // Die Injektionen bilden für jeden Knoten eine Liste. Nach erfolgter
            // Injektion wird das betreffende Injektions-Element aus der Liste gelöscht.
    {
        int schritt;
        Injektion nf;

        Injektion(int schritt) {
            this.schritt = schritt;
            nf = null;
        }
    }

    Injektion injektion;  // Liste der Fehlerinjektionen.
    Random zufall;     // Pseudozufallszahlen-Generator.
    double fehlerWk;   // Wahrscheinlichkeit eines Fehlers in einem Schritt.
    int anzFehler;  // Für Statistik: Fehlerinjektionen zählen.

    Injektor(char myChar, String input) {
        injektion = null;
        if (word(input, 1, 1).toLowerCase().equals("zu"))
            zufall = new Random(System.nanoTime() % 1000000000 + myChar);
        else
            zufall = new Random(number(input, 1, 1) + myChar);
        fehlerWk = 0.0;
        anzFehler = 0;
        for (int i = 2; i <= getItemCount(input) - 1; i++) {
            String fehlerDef = item(input, i);
            if (word(fehlerDef, 1).charAt(0) == myChar) {
                if (word(fehlerDef, 2).toLowerCase().equals("wk"))
                    fehlerWk = real(fehlerDef, 3);
                else {
                    Injektion fuss = null;
                    int anzFehler = getWordCount(fehlerDef, 1) - 1;
                    for (int wort = 2; wort <= anzFehler + 1; wort++)
                        if (fuss == null)
                            injektion = fuss = new Injektion(number(fehlerDef, wort));
                        else
                            fuss = fuss.nf = new Injektion(number(fehlerDef, wort));
                }
            }
        }
    }

    int fehlerinjektion(int schritt)
    // Liefert einen Fehlerwert zwischen 1 und 292, wenn ein Fehler injiziert
    // wird, oder 0, wenn kein Fehler injiziert wird. Zu Betriebsbeginn und
    // ab dem 201. Schritt werden keine Fehler mehr injiziert.
    {
        if (2 <= schritt && schritt <= 200)
            if (fehlerWk > 0 && zufall.nextDouble() < fehlerWk) {
                anzFehler++;
                return 1 + zufall.nextInt(291);
            } else if (injektion != null && schritt == injektion.schritt) {
                injektion = injektion.nf;
                anzFehler++;
                return 1 + zufall.nextInt(291);
            }
        return 0;
    }
}


//------------------------------------------------------------------------------
class FtVerwalter extends Node
// Zentralisierter Verwalter aller Rücksetzlinien.
{ // Für die Statistik:
    int anzInitRL,    // Anzahl der initiierten Rücksetzlinien.
            anzLoeschRL,  // Anzahl der gelöschten Rücksetzlinien.
            anzZurueck,   // Anzahl der Zurücksetz-Operationen.
            anzWeite,     // Anz. des Zurücks. mit zunehmender Rücksetzweite.
            anzAnfang;    // Anzahl des Zurücksetzens auf den Anfang.
    //
    // *** Ggf. weitere Variablen des RL-Verwalters ***
    //

    private ArrayList<String> RpListA = new ArrayList<>();
    private ArrayList<String> RpListB = new ArrayList<>();
    private ArrayList<String> RpListC = new ArrayList<>();
    private ArrayList<String> RpListD = new ArrayList<>();

    // formatting: in-lists: (time, sender) out-lists: (time, receiver)
    private ArrayList<String> messageList = new ArrayList<>();

    private int terminationCount = 0;
    private int frozenThreads = 0;
    private boolean errorInA = false;
    private boolean errorInB = false;
    private boolean errorInC = false;
    private boolean errorInD = false;

    //TODO
    public String runNode(String input) throws SoFTException {
        initialisierung();
        boolean flag = true;
        while (flag) {

            Msg msg = receive("ABCD", never());

            if (msg != null) {

                switch (msg.getTy()) {
                    case 'a':
                        nTraffic(msg);
                        break;
                    case 'f':
                        form('f', "").send("ABCD");
                        switch (msg.getSe()) {
                            case 'A':
                                errorInA = true;
                                break;
                            case 'B':
                                errorInB = true;
                                break;
                            case 'C':
                                errorInC = true;
                                break;
                            case 'D':
                                errorInD = true;
                                break;
                        }
                        break;
                    case 'r':
                        initRL(msg);
                        break;
                    case 'c':
                        ++frozenThreads;
                        if (frozenThreads >= 4) {
                            RLMethode();
                            frozenThreads = 0;
                        }
                    case 't':
                        ++terminationCount;
                        if (terminationCount >= 4) {
                            flag = false;
                        }
                        break;
                    default:
                        say("received message with unsupported type: " + msg.getTy());
                        break;
                }


            }

        }

        printDebugArrays();

        return anzInitRL + " RL initiiert, " + anzLoeschRL + " RL gelöscht, "
                + anzZurueck + " mal zurückgesetzt, "
                + anzWeite + " mal mit zunehmender Rücksetzweite, "
                + anzAnfang + " mal auf den Anfang.";
    }

    //TODO
    private void RLMethode() throws SoFTException {

        // @debug
        say("system frozen");

        //TODO
        // calculate RL

        //TODO
        //send infos for reset in t
        form('t', "").send("ABCD");

        //all errors resolved
        errorInA = errorInB = errorInC = errorInD = false;
    }


    private void initRL(Msg msg) {
        ++anzInitRL;
        switch (msg.getSe()) {
            case 'A':
                RpListA.add(msg.getCo());
                break;
            case 'B':
                RpListB.add(msg.getCo());
                break;
            case 'C':
                RpListC.add(msg.getCo());
                break;
            case 'D':
                RpListD.add(msg.getCo());
                break;
        }
    }

    private void nTraffic(Msg msg) {
        char sender = word(msg.getCo(), 1).charAt(0);
        char receiver = msg.getSe();
        long time = number(msg.getCo(), 2);
        int step = number(msg.getCo(), 3);
        int state = number(msg.getCo(), 4);
        int rpScore = number(msg.getCo(), 5);

        messageList.add(time + " " + sender + " " + receiver + " " + step + " " + state + " " + rpScore);

    }

    private void printDebugArrays() {
        System.out.println();
        System.out.println("--- RP list A ---");
        for (String s : RpListA) {
            System.out.println(s);
        }
        System.out.println();

        System.out.println("--- RP list B ---");
        for (String s : RpListB) {
            System.out.println(s);
        }
        System.out.println();

        System.out.println("--- RP list C ---");
        for (String s : RpListC) {
            System.out.println(s);
        }
        System.out.println();

        System.out.println("--- RP list D ---");
        for (String s : RpListD) {
            System.out.println(s);
        }
        System.out.println("\n--- NA ---\n");

        System.out.println("--- messageList ---");
        for (String s : messageList) {
            System.out.println(s);
        }
        System.out.println();
    }

    void initialisierung()
    // Initialisierung der globalen Variablen des RL-Verwalters.
    {
        anzInitRL = 0;   // Noch keine Rücksetzlinie initiiert.
        anzLoeschRL = 0;   // Noch keine Rücksetzlinie gelöscht.
        anzZurueck = 0;   // Noch nie zurückgesetzt.
        anzWeite = 0;   // Noch nie mit zunehmender Rücksetzweite.
        anzAnfang = 0;   // Noch nie auf den Anfang.
        //
        // *** Ggf. weitere Initialisierungen ***
        //
    }

    //
    // *** Ggf. weitere Methoden des RL-Verwalters ***
    //
}

//
// *** Ggf. weitere Klassen des RL-Verwalters ***
//


//------------------------------------------------------------------------------
public class Ftvs18_Aufg2 extends SoFT
//
// <<<<< Diese Klasse darf nicht verändert werden >>>>>
//
{
    static final int korrektEndSchritt = 290;
    static final int[] korrektEndZustand = {46625, 41937, 10000, 51313};
    int minAnzFehler = 999999,  // Min. Anzahl der injizierten Fehler.
            maxAnzFehler = 0,       // Max. Anzahl der injizierten Fehler.
            minAnzRL = 999999,  // Min. Anzahl der initiierten RL.
            maxAnzRL = 0,       // Max. Anzahl der initiierten RL.
            minAnzZurueck = 999999,  // Min. Anzahl des Zurücksetzens.
            maxAnzZurueck = 0,       // Max. Anzahl des Zurücksetzens.
            minAnzWeite = 999999,  // Min. Anzahl mit zunehmender Weite.
            maxAnzWeite = 0,       // Max. Anzahl mit zunehmender Weite.
            minAnzAnfang = 999999,  // Min. Anzahl des Zurücks. auf den Anfang.
            maxAnzAnfang = 0;       // Max. Anzahl des Zurücks. auf den Anfang.

    public int result(String input, String[] output)
    //            korrekter    mit Zurück-  mit zunehmender  mit Zurücksetzen
    //            Endzustand   setzen       Rücksetzweite    auf den Anfang
    // result 0:    ja           nein         nein             nein
    // result 1:    ja           ja           nein             nein
    // result 2:    ja           ja           ja               nein
    // result 3:    ja           ja           nein             ja
    // result 3:    ja           ja           ja               ja
    // result 5:    nein         beliebig     beliebig         beliebig
    {
        boolean korrekt = true;
        int anzFehler = 0;
        if (exec() <= 1)  // Beginne mit der Zählung neu beim ersten Lauf.
        {
            minAnzFehler = 999999;
            maxAnzFehler = 0;
            minAnzRL = 999999;
            maxAnzRL = 0;
            minAnzZurueck = 999999;
            maxAnzZurueck = 0;
            minAnzWeite = 999999;
            maxAnzWeite = 0;
            minAnzAnfang = 999999;
            maxAnzAnfang = 0;
        }
        boolean gueltig = true;
        for (int i = 0; i < 4; i++) {
            int endSchritt = number(output[i], 3, 3),
                    endZustand = number(output[i], 4, 2);
            korrekt = korrekt && endSchritt == korrektEndSchritt
                    && endZustand == korrektEndZustand[i];
            anzFehler += number(output[i], 1, 1);
            gueltig = gueltig && output[i].length() > 0;
        }
        if (gueltig) {
            minAnzFehler = Math.min(anzFehler, minAnzFehler);
            maxAnzFehler = Math.max(anzFehler, maxAnzFehler);
            minAnzRL = Math.min(minAnzRL, number(output[4], 1, 1));
            maxAnzRL = Math.max(maxAnzRL, number(output[4], 1, 1));
            minAnzZurueck = Math.min(minAnzZurueck, number(output[4], 3, 1));
            maxAnzZurueck = Math.max(maxAnzZurueck, number(output[4], 3, 1));
            minAnzWeite = Math.min(minAnzWeite, number(output[4], 4, 1));
            maxAnzWeite = Math.max(maxAnzWeite, number(output[4], 4, 1));
            minAnzAnfang = Math.min(minAnzAnfang, number(output[4], 5, 1));
            maxAnzAnfang = Math.max(maxAnzAnfang, number(output[4], 5, 1));
        }
        setSummary(minmax(minAnzFehler, maxAnzFehler) + " F,  "
                + minmax(minAnzRL, maxAnzRL) + " RL,  "
                + minmax(minAnzZurueck, maxAnzZurueck) + " zu,  "
                + minmax(minAnzWeite, maxAnzWeite) + " W,  "
                + minmax(minAnzAnfang, maxAnzAnfang) + " Anf"
                + (gueltig ? "" : "  ***"));
        boolean mitZurueck = number(output[4], 3, 1) > 0,
                mitWeite = number(output[4], 4, 1) > 0,
                aufAnfang = number(output[4], 5, 1) > 0;
        if (korrekt) {
            if (!mitZurueck) return 0;
            else if (!mitWeite && !aufAnfang) return 1;
            else if (mitWeite && !aufAnfang) return 2;
            else if (!mitWeite && aufAnfang) return 3;
            else   /*   mitWeite &&   aufAnfang */  return 4;
        } else return 5;
    }

    String minmax(int x, int y)
    // Liefert nur eine Zahl, wenn x == y. Andernfall werden beide Zahlen
    // x und y zurückgegeben
    {
        if (x == y) return "" + x;
        else if (x < 999999) return x + "-" + y;
        else return "" + y;
    }

    public static void main(String[] unbenutzt) {
        String bezeichnung = "Ftvs18_Aufg2: Ausbreitung von Rücksetzlinien"
                + " - Musterlösung";
        String beschreibung = "Eingabezeile: "
                + " ( <z> | \"zu\" ) { \",\" <k> <s> { <s> } | <k> \"wk\" <w> } "
                + "\",\" <a>\n"
                + "   wobei   <z>  Startwert des Pseudozufallszahlen-Generators\n"
                + "           zu   Zufälliger Startwert des Pseudozufallszahlen-"
                + "Generators\n"
                + "           <k>  Bezeichnung eines Knotens\n"
                + "           <s>  Schritt, in dem ein Fehler aufritt\n"
                + "           <w>  Wahrscheinlichkeit eines Fehlers in einem Schritt\n"
                + "           <a>  Fehlererfassung des Absoluttests\n"
                + "   Beispiele:   337, 0.1                                         "
                + " Res 0       Max.  324msg,  5s,  0 F,  37 RL,  0 zu,  0 W,  0 Anf\n"
                + "                337, A  28, 0.1                                  "
                + " Res 1       Max.  350msg,  6s,  1 F,  39 RL,  1 zu,  0 W,  0 Anf\n"
                + "                337, A 100, 0.1                                  "
                + " Res 1,2     Max.  413msg,  8s,  1 F,  49 RL,  2 zu,  1 W,  0 Anf\n"
                + "                337, A   5, 0.1                                  "
                + " Res     3   Max.  339msg, 10s,  1 F,  37 RL,  1 zu,  0 W,  1 Anf\n"
                + "                337, A 50, B 70, C 150, D 170, 0.1               "
                + " Res   2     Max.  489msg, 10s,  4 F,  49 RL,  4 zu,  1 W,  0 Anf\n"
                + "                337, D 20, C 100 99, B 40, 0.1                   "
                + " Res       4 Max.  482msg, 34s,  4 F,  54 RL,  4 zu,  1 W,  1 Anf\n"
                + "                231, A 65 200, B wk 0.01, D 88 121 118, 0.1      "
                + " Res       4 Max. 1464msg, 36s, 23 F, 162 RL, 19 zu, 14 W,  7 Anf\n"
                + "                zu,  A wk 0.004, B 120, C 120, D wk 0.005, 0.2   "
                + " Res 1,2,3,4 Max. 1537msg,  6s, 19 F, 151 RL, 20 zu, 14 W, 14 Anf";
        new Ftvs18_Aufg2().runSystem(new Node[]
                {new Anwendung(), new Anwendung(), new Anwendung(), new Anwendung(),
                        new FtVerwalter()}, bezeichnung, beschreibung, "Klaus Echtle");
    }
}
