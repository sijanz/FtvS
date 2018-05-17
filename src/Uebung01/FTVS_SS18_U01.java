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

import static SoFTlib.Helper.*;

import java.util.*;

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

        // give other threads time to finish before calling terminate
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
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
        Msg statusE = receive("E", time() + 50);
        Msg statusF = receive("F", time() + 50);
        Msg statusG = receive("G", time() + 50);
        Msg statusH = receive("H", time() + 50);
        Msg statusI = receive("I", time() + 50);
        Msg statusJ = receive("J", time() + 50);

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
    private ArrayList<String> orderList = new ArrayList<>();

    public Verteiler(abstrakterRechner[] rechner) {
        this.rechner = rechner;
    }

    public String runNode(String input) throws SoFTException {
        while (true) {

            // FIXME: adapt timeout
            // receive a, t or r messages
            Msg order = receive("A", 'a', time() + 50);
            Msg rekonfiguration = receive("A", 'r', time() + 50);
            Msg terminate = receive("A", 't', time() + 50);

            // wenn Rekonfigurationsnachricht empfangen
            if (rekonfiguration != null) {

                // @debug
                say(rekonfiguration.getCo());

                reconfigurate();
            }

            // wenn Auftragsnachricht empfangen
            if (order != null) {
                distributeOrder(order);
                order = null;
            }

            // wenn Terminierungsnachricht empfangen
            if (terminate != null) {
                return "0";
            }
        }
    }

    private void distributeOrder(Msg message) {
        String tmp = message.getCo();

        // get rid of brackets
        tmp = tmp.replace("(", "");
        tmp = tmp.replace(")", "");

        int wordCount = getItemCount(tmp);

        // add orders to order list
        for (int i = 1; i <= wordCount; ++i) {
            orderList.add(words(tmp, i, 1, 3));
        }

        ArrayList<abstrakterRechner> rechnerList = new ArrayList<>();

        for(abstrakterRechner ar : rechner) {
            if(ar.getStatus()) {
                rechnerList.add(ar);
            }
        }

        while(!orderList.isEmpty()) {

            // get maximum workload
            int max = 0;
            for (String s : orderList) {
                int u = Integer.parseInt(words(s, 1, 3, 3));
                if (u > max) {
                    max = u;
                }
            }

            // get order with minimum workout, send it and then delete it
            for (String s : orderList) {
                if(s != null) {
                    int u = Integer.parseInt(words(s, 1, 3, 3));
                    if (u == max) {
                        rechnerList.remove(sendOrder(s, rechnerList));

                        // FIXME
                        // @debug
                        for (abstrakterRechner as : rechnerList) {
                            System.out.println(as.getID());
                        }
                        System.out.println("");

                        if (rechnerList.isEmpty()) {
                            for(abstrakterRechner ar : rechner) {
                                if(ar.getStatus()) {
                                    rechnerList.add(ar);
                                }
                            }
                            System.out.println("erneuert");
                        }
                        orderList.remove(s);
                        break;
                    }
                }
            }
        }
    }

    // TODO
    private abstrakterRechner sendOrder(String message, ArrayList<abstrakterRechner> rechnerList) {

        abstrakterRechner strongestBoi = null;
        int howStrongIsBoi = 41;

        for(abstrakterRechner as : rechnerList) {
            if(as.getGeschwindigkeit() < howStrongIsBoi && as.getStatus()) {
                howStrongIsBoi = as.getGeschwindigkeit();
                strongestBoi = as;
            }
        }

        if (strongestBoi != null) {

            //senden
            Msg order = form('a', message);
            try {
                order.send(strongestBoi.getID());
            } catch (SoFTException e) {
                e.printStackTrace();
            }
        }
        return strongestBoi;
    }

    private void reconfigurate() {

    }
}

/**
 * Diese abstrakte Klasse soll lediglich dazu dienen, eine Schnittstelle mit Informationen
 * bereitzustellen, welche von anderen Knoten gelesen werden darf.
 */
abstract class abstrakterRechner extends Node {
    private String id;
    private int geschwindigkeit;
    private boolean status;

    public abstrakterRechner(String id, int geschwindigkeit, boolean status) {
        this.id = id;
        this.geschwindigkeit = geschwindigkeit;
        this.status = status;
    }

    public int getGeschwindigkeit() {
        return geschwindigkeit;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean getStatus() {
        return status;
    }

    public String getID() {
        return id;
    }
}

/**
 * Aufgabe 1c
 */
class Rechner extends abstrakterRechner {

    public Rechner(String id, int geschwindigkeit, boolean status) {
        super(id, geschwindigkeit, status);
    }

    public String runNode(String input) throws SoFTException {
        while (true) {

            // FIXME: adapt timeout
            Msg terminate = receive("A", 't', time() + 50);
            Msg order = receive("BCD", 'a', time() + 50);

            if (order != null) {
                work();
            }

            if (terminate != null) {
                return "0";
            }
        }
    }

    private void work() {

    }
}

public class FTVS_SS18_U01 extends SoFT {

    public int result(String input, String[] output) {

        return 0;
    }

    public static void main(String[] args) {
        abstrakterRechner[] rechner = new abstrakterRechner[]{new Rechner("E", 40, true), new Rechner("F", 10, true),
                new Rechner("G", 25, true), new Rechner("H", 15, true), new Rechner("I", 35, false), new Rechner("J", 10, false)};
        Verteiler[] verteiler = new Verteiler[]{new Verteiler(rechner), new Verteiler(rechner),
                new Verteiler(rechner)};
        Node[] system = new Node[]{new Auftragsknoten(), verteiler[0], verteiler[1], verteiler[2],
                rechner[0], rechner[1], rechner[2], rechner[3], rechner[4], rechner[5]};
        new FTVS_SS18_U01().runSystem(system, "Uebungsblatt 1", "Redundanz und Rekonfiguration",
                "01");
    }
}
