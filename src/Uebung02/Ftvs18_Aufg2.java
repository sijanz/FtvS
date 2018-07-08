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

    // highest global rpNum
    static int globalRpNum = 0;

    // number of most recent RP
    int rpNum;

    public String runNode(String input) throws SoFTException {
        anzInitRL = 0;            // Noch keine RL initiiert.
        rpNum = 0;
        globalRpNum = 0;
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
        form('n', inhalt + " " + rpNum).send(empfaenger);
        //
        // *** Diese Methode ggf. modifizieren ***
        //
    }

    protected String empfange(char sender)
            throws Anlauf, SoFTException
    // Operator, den die Anwendung aufruft, um eine Nachricht zu empfangen.
    {
        Msg msgFromE = receive("E", time() + 5);
        while (msgFromE != null) {
            switch (msgFromE.getTy()) {
                case 'h':

                    // confirm holding
                    form('c', schritt() + " HOLDING").send("E");

                    msgFromE = receive("E", time() + 1500);

                    if (msgFromE != null) {
                        say("RELEASED");

                        String content = msgFromE.getCo();

                        if (!content.equals(" ")) {

                            workInbox();

                            int step = number(content, 1);
                            int state = number(content, 2);

                            setzeSchritt(step);
                            setzeZustand(state);

                            say("new values are: " + schritt() + " " + zustand());

                            receive("X", time() + 200);

                            throw new Anlauf();

                        } else {
                            say("no reset data send");
                        }


                    } else {

                        // TODO

                        // @debug
                        say("SPECIAL CASE NOT YET IMPLEMENTED");
                    }

                    break;
                case 'n':
                    return msgFromE.getCo();
                default:
                    say("unsupported message type: " + msgFromE.getTy());
                    break;
            }
            msgFromE = receive("E", time() + 5);
        }

        Msg msg = receive(sender, never());  // Empfang ohne Zeitschranke.
        if (msg != null) {

            // rpNum from sender
            int rpNumSender = number(msg.getCo(), 3);

            switch (msg.getTy()) {
                case 'n':

                    // sender has a more recent rpNum
                    if (rpNumSender > rpNum) {
                        rpNum = rpNumSender;
                        sendRp(schritt() + " " + zustand());

                        // this node has a more recent RP-status
                    } else if (rpNumSender < rpNum) {

                        // send a "copy" to E embedded in an a-message
                        form('a', msg.getSe() + " " + time() + " " + msg.getCo()).send("E");
                    }
                    return msg.getCo();               // Nutznachricht wurde empfangen.
                default:
                    say("unsupported message type: " + msg.getTy());
                    break;
            }
        }
        return "";
    }

    void workInbox() throws SoFTException {
        Msg msg = receive("ABCD", time() + 5);
        while (msg != null) {

            // rpNum from sender
            int rpNumSender = number(msg.getCo(), 3);

            if (rpNumSender < rpNum) {
                // send a "copy" to E embedded in an a-message
                form('a', msg.getSe() + " " + time() + " " + msg.getCo()).send("E");
            }

            msg = receive("ABCD", time() + 5);
        }
    }

    /**
     * Sends an RP to E.
     *
     * @param content saved attributes (step + state)
     */
    protected void sendRp(String content) throws SoFTException {
        form('r', content + " " + rpNum + " " + time()).send('E');
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
            rpNum = ++globalRpNum;
            form('r', inhalt + " " + globalRpNum + " " + time()).send('E');
        }
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

    // counter for termination messages
    int terminationCount = 0;

    // counts holding nodes
    int holdingNodes = 0;

    // lists for saving RPs
    // formatting: step state rpNum time
    private ArrayList<String> rpListA = new ArrayList<>();
    private ArrayList<String> rpListB = new ArrayList<>();
    private ArrayList<String> rpListC = new ArrayList<>();
    private ArrayList<String> rpListD = new ArrayList<>();

    // list for saving n-messages
    // formatting: sender time message
    private ArrayList<String> messageList = new ArrayList<>();

    // list for managing RLs
    // formatting: rpNum A B C D
    private ArrayList<String> rlList = new ArrayList<>();

    // array to save errors
    private boolean[] errorArray = {false, false, false, false};

    // array to save in what step the error was observed
    private long[] errorLongArray = {300, 300, 300, 300};

    public String runNode(String input) throws SoFTException {
        initialisierung();


        boolean running = true;

        while (running) {

            // receive messages
            Msg message = receive("ABCD", never());

            if (message != null) {
                switch (message.getTy()) {
                    case 'c':
                        if (++holdingNodes >= (4 - terminationCount)) {

                            ArrayList<String> aMessages = new ArrayList<>();
                            ArrayList<String> bMessages = new ArrayList<>();
                            ArrayList<String> cMessages = new ArrayList<>();
                            ArrayList<String> dMessages = new ArrayList<>();

                            // find RL
                            int rl = calculateRl();

                            // find each RP from RL ( step state time )
                            String aContent = findRptoNum(rl, rpListA);
                            String bContent = findRptoNum(rl, rpListB);
                            String cContent = findRptoNum(rl, rpListC);
                            String dContent = findRptoNum(rl, rpListD);

                            long aTime = number(aContent, 3);
                            long bTime = number(bContent, 3);
                            long cTime = number(cContent, 3);
                            long dTime = number(dContent, 3);

                            // send RP to corresponding node
                            form('r', word(aContent, 1) + " " + word(aContent, 2)).send("A");
                            form('r', word(bContent, 1) + " " + word(bContent, 2)).send("B");
                            form('r', word(cContent, 1) + " " + word(cContent, 2)).send("C");
                            form('r', word(dContent, 1) + " " + word(dContent, 2)).send("D");

                            receive("X", time() + 500);

                            // wait for nodes to send a messages
                            Msg msg = receive("ABCD", time() + 5);

                            while (msg != null) {
                                say("received " + msg.getCo());
                                if (msg.getTy() == 'a') {
                                    storeMessage(msg);
                                }
                                msg = receive("ABCD", time() + 30);
                            }

                            // find messages to resend
                            aMessages = findMessages(aTime, 'A');
                            bMessages = findMessages(bTime, 'B');
                            cMessages = findMessages(cTime, 'C');
                            dMessages = findMessages(dTime, 'D');

                            // resend messages
                            for (String s : aMessages) {
                                form('n', s).send('A');
                            }
                            for (String s : bMessages) {
                                form('n', s).send('B');
                            }
                            for (String s : cMessages) {
                                form('n', s).send('C');
                            }
                            for (String s : dMessages) {
                                form('n', s).send('D');
                            }


                            // all errors should be resolved
                            resetErrorArrays();

                            // release nodes
                            holdingNodes = 0;
                        }
                        break;

                    // error message
                    case 'f':

                        boolean flag = true;
                        for (int i = 0; i < errorArray.length; ++i) {
                            if (errorArray[i]) {
                                flag = false;
                            }
                        }
                        if (flag) {
                            form('h', number(message.getCo(), 1)).send("ABCD");

                        }

                        switch (message.getSe()) {
                            case 'A':
                                errorArray[0] = true;
                                errorLongArray[0] = number(message.getCo(), 1);
                                break;
                            case 'B':
                                errorArray[1] = true;
                                errorLongArray[1] = number(message.getCo(), 1);
                                break;
                            case 'C':
                                errorArray[2] = true;
                                errorLongArray[2] = number(message.getCo(), 1);
                                break;
                            case 'D':
                                errorArray[3] = true;
                                errorLongArray[3] = number(message.getCo(), 1);
                                break;
                        }

                        break;

                    // RP message received
                    case 'r':
                        switch (message.getSe()) {
                            case 'A':
                                rpListA.add(message.getCo());
                                break;
                            case 'B':
                                rpListB.add(message.getCo());
                                break;
                            case 'C':
                                rpListC.add(message.getCo());
                                break;
                            case 'D':
                                rpListD.add(message.getCo());
                                break;
                        }
                        updateRlList(message);
                        break;

                    // NA message received
                    case 'a':
                        storeMessage(message);
                        break;

                    // termination message received
                    case 't':
                        terminationCount++;
                        if (terminationCount >= 4) {
                            running = false;
                        }
                        break;

                    default:
                        say("received unsupported message type: " + message.getTy());
                        break;
                }
            }
        }

        printDebugArrays();

        //
        // *** Aktionen des RL-Verwalters ***
        //
        return anzInitRL + " RL initiiert, " + anzLoeschRL + " RL gelöscht, "
                + anzZurueck + " mal zurückgesetzt, "
                + anzWeite + " mal mit zunehmender Rücksetzweite, "
                + anzAnfang + " mal auf den Anfang.";
    }


    private ArrayList<String> findMessages(long time, char node) {
        ArrayList<String> list = new ArrayList<>();

        // find entries that were sent to node, between minTime and maxTime
        for (String s : messageList) {
            long sTime = number(s, 1);
            if ((word(s, 3).charAt(0) == node) && (sTime > time)) {
                list.add(word(s, 4) + " " + word(s, 5) + " " + word(s, 6));
            }
        }

        return list;
    }


    private String findRptoNum(int num, ArrayList<String> list) {
        String content = "";
        for (String s : list) {
            if (number(s, 3) == num) {
                content = (number(s, 1) + " " + number(s, 2) + " " + number(s, 4));
                break;
            }
        }
        return content;
    }


    private void resetErrorArrays() {
        for (int i = 0; i < errorLongArray.length; ++i) {
            errorLongArray[i] = 300;
            errorArray[i] = false;
        }
    }


    private int calculateRl() {
        boolean[] solutionArray = new boolean[4];
        int rpNum = 0;
        for (String s : rlList) {
            boolean flag = true;
            for (int i = 0; i < 3; ++i) {
                solutionArray[i] = !(errorArray[i] && (number(s, i + 2) == 0));
                if (!solutionArray[i]) {
                    flag = false;
                    break;
                }
            }
            if (flag && (rpNum < number(s, 1))) {
                rpNum = number(s, 1);
            }

        }

        //delete later RPs and RLs
        deleteLaterRLs(rpNum);
        deleteLaterRPs(rpNum, rpListA);
        deleteLaterRPs(rpNum, rpListB);
        deleteLaterRPs(rpNum, rpListC);
        deleteLaterRPs(rpNum, rpListD);

        return rpNum;
    }

    /**
     * @param message
     */
    private void updateRlList(Msg message) {
        char node = message.getSe();
        int rpNum = number(message.getCo(), 3);

        boolean flag = true;
        String copy = "";

        for (String s : rlList) {

            //true once if entry already exists
            if (number(s, 1) == rpNum) {
                String string = "";
                copy = s;
                switch (node) {
                    case 'A':
                        string = word(s, 1) + " 1 " + word(s, 3) + " " + word(s, 4) + " " + word(s, 5);
                        break;
                    case 'B':
                        string = word(s, 1) + " " + word(s, 2) + " 1 " + word(s, 4) + " " + word(s, 5);
                        break;
                    case 'C':
                        string = word(s, 1) + " " + word(s, 2) + " " + word(s, 3) + " 1 " + word(s, 5);
                        break;
                    case 'D':
                        string = word(s, 1) + " " + word(s, 2) + " " + word(s, 3) + " " + word(s, 4) + " 1";
                        break;
                }
                rlList.add(string);
                flag = false;
                break;
            }
        }

        //no matching entry - creating new one
        if (flag) {
            switch (node) {
                case 'A':
                    rlList.add(rpNum + " 1 0 0 0");
                    break;
                case 'B':
                    rlList.add(rpNum + " 0 1 0 0");
                    break;
                case 'C':
                    rlList.add(rpNum + " 0 0 1 0");
                    break;
                case 'D':
                    rlList.add(rpNum + " 0 0 0 1");
                    break;
            }
        } else {
            rlList.remove(copy);
        }

    }

    private void deleteLaterRLs(int rlNum) {
        //store entries to delete
        ArrayList<String> list = new ArrayList<>();

        //search for entries to delete
        for (String s : rlList) {
            if (number(s, 1) > rlNum) {
                list.add(s);
            }
        }

        //delete entries
        for (String s : list) {
            rlList.remove(s);
        }

    }

    /**
     * Deletes all RPs and RLs with an RP-Number bigger than the given parameter
     *
     * @param rpNum RP num to cut at
     */
    private void deleteLaterRPs(int rpNum, ArrayList<String> rpList) {

        //store entries to delete
        ArrayList<String> list = new ArrayList<>();

        //search for entries to delete
        for (String s : rpList) {
            if (number(s, 3) > rpNum) {
                list.add(s);
            }
        }

        //delete entries
        for (String s : list) {
            rpList.remove(s);
        }
    }


    /**
     * Stores messages as time + sender + receiver + step + state + rpNum.
     *
     * @param message the message to be stored
     */
    private void storeMessage(Msg message) {
        char sender = word(message.getCo(), 1).charAt(0);
        char receiver = message.getSe();
        long time = number(message.getCo(), 2);
        int step = number(message.getCo(), 3);
        int state = number(message.getCo(), 4);
        int rpNum = number(message.getCo(), 5);

        messageList.add(time + " " + sender + " " + receiver + " " + step + " " + state + " " + rpNum);
    }

    private void printDebugArrays() {
        System.out.println();
        System.out.println("Step State rpNum Time");
        System.out.println();
        System.out.println("--- RP list A ---");
        for (String s : rpListA) {
            System.out.println(s);
        }
        System.out.println();

        System.out.println("--- RP list B ---");
        for (String s : rpListB) {
            System.out.println(s);
        }
        System.out.println();

        System.out.println("--- RP list C ---");
        for (String s : rpListC) {
            System.out.println(s);
        }
        System.out.println();

        System.out.println("--- RP list D ---");
        for (String s : rpListD) {
            System.out.println(s);
        }
        System.out.println("\n--- NA ---\n");

        System.out.println("--- RL list ---");
        for (String s : rlList) {
            System.out.println(s);
        }


        System.out.println("Time Sender Receiver Step State rpNum");
        System.out.println();
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

        resetErrorArrays();
        rlList.clear();
        rpListA.clear();
        rpListB.clear();
        rpListC.clear();
        rpListD.clear();
        messageList.clear();
        terminationCount = 0;
        holdingNodes = 0;

        // initialize RP-lists with an entry at 0
        rpListA.add("0 10000 0 0");
        rpListB.add("0 10000 0 0");
        rpListC.add("0 10000 0 0");
        rpListD.add("0 10000 0 0");

        // initialize RL-list
        rlList.add("0 1 1 1 1");
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
