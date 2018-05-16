/**
 * Fehlertolerante verteilte Systeme, Sommersemester 2018
 * Valentin Fitz (fitz@dc.uni-due.de)
 * Verlaesslichkeit von Rechensystemen, Universitaet Duisburg-Essen
 * <p>
 * �bungsblatt 01, Codegeruest
 * <p>
 * Bemerkung: Bitte benutzen Sie zur Implementierung Ihrer L�sung dieses Codegeruest. Sie d�rfen die
 * vorgegebenen Klassen nach belieben erweitern, sollten jedoch darauf achten, dass Ihre L�sung dem Rahmen
 * der Spezifikation des Aufgabenblattes entspricht. Sie d�rfen ebenfalls bei Bedarf eigene Hilfsklassen
 * schreiben. Vermeiden Sie es jedoch bitte, Ihre L�sung in mehrere Dateien aufzuteilen.
 */

package Uebung01;

import SoFTlib.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static SoFTlib.Helper.*;

/**
 * Aufgabe 1a
 */
class Auftragsknoten extends Node {

    Random rando = new Random();
    private static int AuftragsID = 0;

    // Liste der Verteiler
    List<String> verteiler = Arrays.asList("B", "C", "D");

    // Liste der Auftraege
    List auftrage;


    public String runNode(String input) throws SoFTException {
        for (int i = 0; i <= 9; i++) {
            boolean flag = true;
            while (flag) {
                long zeit = time();
                if (zeit >= 300 * i) {
                    sendeAuftraege();
                    flag = false;
                } else {
                    receiveS();
                }
            }
        }


        return "";
    }


    /**
     * Erzeugt neue Auftraege und schreibt sie in die Liste der Auftraege
     *
     * @return neue Auftraege
     */
    private String erzeugeAuftraege() {
        int anzahlAuftraege = (rando.nextInt((6 - 1) + 1) + 1);

        String a = "(";

        for (int i = 0; i < anzahlAuftraege; ++i) {
            if (i > 0) {
                a += " ; ";
            }
            a += ++AuftragsID + " 0 " + (rando.nextInt((6 - 1) + 1) + 1);
        }
        return a + ")";
    }

    //TODO
    private void receiveS() throws SoFTException {
        receive("E", 1);
        receive("F", 1);
        receive("G", 1);
        receive("H", 1);
        receive("I", 1);
        receive("J", 1);
    }


    private void sendeAuftraege() {
        // erzeuge neue Auftraege
        String auftraege = erzeugeAuftraege();

        // generiere Nachricht
        Msg nachricht = form('a', auftraege);

        // schicke Nachrichten
        String receivers = "";
        for (String s : verteiler) {
            receivers += s;
        }

        try {
            nachricht.send(receivers);
        } catch (SoFTException e) {
            e.printStackTrace();
        }
    }

    private void empfangenStatusnachrichten() {

    }


    private boolean checkFehlverhalten(String nachricht) {
        return false;
    }


    private void sendeRekonfigurationsnachricht() {

    }


    private void sendeTerminierungsnachricht() {

    }

    private void terminate() {

    }
}

/**
 * Aufgabe 1b
 */
class Verteiler extends Node {

    private abstrakterRechner[] rechner;

    public Verteiler(abstrakterRechner[] rechner) {
        this.rechner = rechner;
    }

    public String runNode(String input) throws SoFTException {

        return "";
    }
}

/**
 * Diese abstrakte Klasse soll lediglich dazu dienen, eine Schnittstelle mit Informationen
 * bereitzustellen, welche von anderen Knoten gelesen werden darf.
 */
abstract class abstrakterRechner extends Node {
    private int geschwindigkeit;

    public abstrakterRechner(int geschwindigkeit) {
        this.geschwindigkeit = geschwindigkeit;
    }

    public int getGeschwindigkeit() {
        return geschwindigkeit;
    }
}

/**
 * Aufgabe 1c
 */
class Rechner extends abstrakterRechner {

    public Rechner(int geschwindigkeit) {
        super(geschwindigkeit);
    }

    public String runNode(String input) throws SoFTException {

        return "";
    }
}

public class FTVS_SS18_U01 extends SoFT {

    public int result(String input, String[] output) {
        return 5;
    }

    public static void main(String[] args) {
        abstrakterRechner[] rechner = new abstrakterRechner[]{new Rechner(40), new Rechner(10),
                new Rechner(25), new Rechner(15), new Rechner(35), new Rechner(10)};
        Verteiler[] verteiler = new Verteiler[]{new Verteiler(rechner), new Verteiler(rechner),
                new Verteiler(rechner)};
        Node[] system = new Node[]{new Auftragsknoten(), verteiler[0], verteiler[1], verteiler[2],
                rechner[0], rechner[1], rechner[2], rechner[3], rechner[4], rechner[5]};
        new FTVS_SS18_U01().runSystem(system, "Uebungsblatt 1", "Redundanz und Rekonfiguration",
                "01");
    }
}
