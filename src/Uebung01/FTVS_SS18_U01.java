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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Aufgabe 1a
 */
class Auftragsknoten extends Node {

    // Zufallsgenerator
    private Random random = new Random();

    // ID des aktuellen Auftrags
    private static int auftragsID = 0;

    // Liste der Verteiler
    private String verteiler = "BCD";

    // Liste der Auftraege
    private ArrayList<String> auftraege = new ArrayList<>();

    // Liste der Statusnachrichten
    private ArrayList<Msg> statusnachrichten = new ArrayList<>();


    /**
     * Hauptmethode.
     *
     * @param input optionale Eingabe
     * @return
     */
    public String runNode(String input) throws SoFTException {
        for (int i = 0; i <= 9; i++) {
            boolean flag = true;
            while (flag) {
                if (time() >= 300 * i) {
                    sendeAuftraege();
                    flag = false;
                } else {
                    empfangeStatusnachrichten();
                }
            }
        }

        // generiere Terminierungsnachricht und sende sie an die restlichen Rechner
        Msg terminate = form('t', "");
        terminate.send(verteiler + "EFGHIJ");

        return "0";
    }


    /**
     * Erzeugt neue Auftraege und schreibt sie in die Liste der Auftraege.
     *
     * @return neue Auftraege
     */
    private String erzeugeAuftraege() {
        int anzahlAuftraege = (random.nextInt((6 - 1) + 1) + 1);

        StringBuilder a = new StringBuilder("(");

        for (int i = 0; i < anzahlAuftraege; ++i) {
            if (i > 0) {
                a.append(" ; ");
            }
            int aufwand = random.nextInt((6 - 1) + 1) + 1;
            a.append(++auftragsID).append(" 0 ").append(aufwand);

            String auftrag = "(" + auftragsID + " 0 " + aufwand + ")";
            auftraege.add(auftrag);
        }
        return a + ")";
    }


    /**
     * Sendet erzeugte Auftrage zu den Verteilern.
     */
    private void sendeAuftraege() {

        // erzeuge neue Auftraege
        String auftraege = erzeugeAuftraege();

        // generiere Nachricht
        Msg nachricht = form('a', auftraege);

        // schicke Nachrichten
        try {
            nachricht.send(verteiler);
        } catch (SoFTException e) {
            e.printStackTrace();
        }
    }


    /**
     * Empfängt Statusnachrichten von den Rechnern und speichert diese.
     */
    private void empfangeStatusnachrichten() throws SoFTException {

        // FIXME: adapt timeout
        // empfange Nachrichten von Rechnern
        Msg statusE = receive("E", time() + 1);
        Msg statusF = receive("F", time() + 1);
        Msg statusG = receive("G", time() + 1);
        Msg statusH = receive("H", time() + 1);
        Msg statusI = receive("I", time() + 1);
        Msg statusJ = receive("J", time() + 1);

        // füge Statusnachrichten zur Liste hinzu
        statusnachrichten.add(statusE);
        statusnachrichten.add(statusF);
        statusnachrichten.add(statusG);
        statusnachrichten.add(statusH);
        statusnachrichten.add(statusI);
        statusnachrichten.add(statusJ);

        verarbeiteStatusnachrichten();
    }


    private void verarbeiteStatusnachrichten() throws SoFTException {
        for (Msg m : statusnachrichten) {
            if (checkFehlverhalten(m)) {
                sendeRekonfigurationsnachricht(m.getSe());
                Msg nachricht = form('a', m.getCo());
                nachricht.send(verteiler);
            } else {
                // TODO
            }
        }
    }


    // TODO
    private boolean checkFehlverhalten(Msg nachricht) {
        return false;
    }


    /**
     * Erzeugt und sendet Rekonfigurationsnachrichten and die Verteiler.
     *
     * @param rechner der defekte Rechner
     */
    private void sendeRekonfigurationsnachricht(char rechner) {

        // generiere Rekonfigurationsnachricht
        Msg nachricht = form('r', rechner);

        // schicke Nachricht
        try {
            nachricht.send(verteiler);
        } catch (SoFTException e) {
            e.printStackTrace();
        }
    }
}


/**
 * Aufgabe 1b
 */
class Verteiler extends Node {

    private abstrakterRechner[] rechner;
    private Msg auftrag;
    private Msg rekonfiguration;
    private Msg terminate;

    public Verteiler(abstrakterRechner[] rechner) {
        this.rechner = rechner;
    }

    public String runNode(String input) throws SoFTException {
        while (time() < 3000) {
            auftrag = receive("A", 'a', time() + 1);
            rekonfiguration = receive("A", 'r', time() + 1);
            terminate = receive("A", 't', time() + 1);

            // wenn Terminierungsnachricht empfangen
            if (terminate != null) {
                return "0";
            }

            // wenn Auftragsnachricht empfangen
            if (auftrag != null) {

                // @debug
                System.out.println(auftrag.getCo());

                verteileAuftrag();
            }

            // wenn Rekonfigurationsnachricht empfangen
            if (rekonfiguration != null) {

                // @debug
                System.out.println(rekonfiguration.getCo());

                rekonfiguriere();
            }
        }

        return "0";
    }

    private void verteileAuftrag() {

    }

    private void rekonfiguriere() {

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

        return "0";
    }
}

public class FTVS_SS18_U01 extends SoFT {

    public int result(String input, String[] output) {
        return 0;
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
