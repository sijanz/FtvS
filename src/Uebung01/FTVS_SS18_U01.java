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

    // random generator
    private Random random = new Random();

    // ID of the the current order
    private static int orderID = 0;

    // list of distributors
    private String distributors = "BCD";

    // list of orders
    private ArrayList<String> orders = new ArrayList<>();

    // list of status messages
    private ArrayList<Msg> statusMessages = new ArrayList<>();


    /**
     * Main method, runs the Auftragsknoten.
     *
     * @param input optional input
     * @return 0 if executed properly
     */
    public String runNode(String input) throws SoFTException {
        for (int i = 0; i <= 9; i++) {
            boolean flag = true;
            while (flag) {
                if (time() >= 300 * i) {
                    sendOrders();
                    flag = false;
                } else {
                    receiveStatusMessages();
                }
            }
        }

        // give other threads time to finish before calling terminate
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // generate and send message to terminate other computers
        form('t', "").send(distributors + "EFGHIJ");

        return "0";
    }


    /**
     * Creates new orders and stores them in the list
     *
     * @return the created orders
     */
    private String createOrders() {

        // determine number of orders randomly
        int numberOfOrders = (random.nextInt((6 - 1) + 1) + 1);

        StringBuilder a = new StringBuilder("(");

        for (int i = 0; i < numberOfOrders; ++i) {
            if (i > 0) {
                a.append(" ; ");
            }

            // determine workload randomly
            int workload = random.nextInt((6 - 1) + 1) + 1;

            a.append(++orderID).append(" 0 ").append(workload);

            // store order in list
            String order = "(" + orderID + " 0 " + workload + ")";
            orders.add(order);
        }
        return a + ")";
    }


    /**
     * Sends creates orders to the distributors.
     */
    private void sendOrders() {

        // create new orders
        String orders = createOrders();

        // generate message and send it to distributors
        try {
            form('a', orders).send(distributors);
        } catch (SoFTException e) {
            e.printStackTrace();
        }
    }


    /**
     * Receives status messages and stores them.
     */
    private void receiveStatusMessages() throws SoFTException {

        // FIXME: adapt timeout
        // receive messages
        Msg statusE = receive("E", time() + 50);
        Msg statusF = receive("F", time() + 50);
        Msg statusG = receive("G", time() + 50);
        Msg statusH = receive("H", time() + 50);
        Msg statusI = receive("I", time() + 50);
        Msg statusJ = receive("J", time() + 50);

        // store message
        statusMessages.add(statusE);
        statusMessages.add(statusF);
        statusMessages.add(statusG);
        statusMessages.add(statusH);
        statusMessages.add(statusI);
        statusMessages.add(statusJ);

        processStatusMessages();
    }


    private void processStatusMessages() throws SoFTException {
        for (Msg m : statusMessages) {
            if (checkForFaults(m)) {
                sendReconfigurationMessage(m.getSe());
                Msg nachricht = form('a', m.getCo());
                nachricht.send(distributors);
            } else {
                // TODO
            }
        }
    }


    // TODO
    private boolean checkForFaults(Msg message) {
        return false;
    }


    /**
     * Creates and sends a reconfiguration message to the distributors.
     *
     * @param computer the faulty computer
     */
    private void sendReconfigurationMessage(char computer) {
        try {
            form('r', computer).send(distributors);
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

    // stores the orders
    private ArrayList<String> orderList = new ArrayList<>();

    public Verteiler(abstrakterRechner[] rechner) {
        this.rechner = rechner;
    }

    public String runNode(String input) throws SoFTException {
        while (true) {

            // FIXME: adapt timeout
            // receive a, t or r messages
            Msg order = receive("A", 'a', time() + 50);
            Msg reconfiguration = receive("A", 'r', time() + 50);
            Msg terminate = receive("A", 't', time() + 50);

            // reconfiguration message received
            if (reconfiguration != null) {

                // @debug
                say(reconfiguration.getCo());

                reconfigurate(reconfiguration);
            }

            // order received
            if (order != null) {
                distributeOrder(order);
            }

            // termination message received
            if (terminate != null) {
                return "0";
            }
        }
    }


    /**
     * Distributes message to the computers.
     * @param message the message to distribute
     */
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

        // temporary list of computers
        ArrayList<abstrakterRechner> computerList = new ArrayList<>();

        // add active computers to temporary list
        for (abstrakterRechner ar : rechner) {
            if (ar.getStatus()) {
                computerList.add(ar);
            }
        }

        while (!orderList.isEmpty()) {

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
                if (s != null) {
                    int u = Integer.parseInt(words(s, 1, 3, 3));
                    if (u == max) {
                        computerList.remove(sendOrder(s, computerList));
                        if (computerList.isEmpty()) {
                            for (abstrakterRechner ar : rechner) {
                                if (ar.getStatus()) {
                                    computerList.add(ar);
                                }
                            }
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

        for (abstrakterRechner as : rechnerList) {
            if (as.getGeschwindigkeit() < howStrongIsBoi && as.getStatus()) {
                howStrongIsBoi = as.getGeschwindigkeit();
                strongestBoi = as;
            }
        }

        if (strongestBoi != null) {

            // send order
            Msg order = form('a', message);
            try {
                order.send(strongestBoi.myChar());
            } catch (SoFTException e) {
                e.printStackTrace();
            }
        }
        return strongestBoi;
    }


    /**
     * Marks a computer as inactive.
     *
     * @param message the reconfiguration message
     */
    private void reconfigurate(Msg message) {
        for (abstrakterRechner ar : rechner) {
            if (ar.myChar() == message.getCo().charAt(0)) {
                ar.setStatus(false);
            }
        }
    }
}


/**
 * Diese abstrakte Klasse soll lediglich dazu dienen, eine Schnittstelle mit Informationen
 * bereitzustellen, welche von anderen Knoten gelesen werden darf.
 */
abstract class abstrakterRechner extends Node {
    private int geschwindigkeit;
    private boolean status;
    public abstrakterRechner(int geschwindigkeit, boolean status) {
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
}

/**
 * Aufgabe 1c
 */
class Rechner extends abstrakterRechner {

    public Rechner(int geschwindigkeit, boolean status) {
        super(geschwindigkeit, status);
    }

    public String runNode(String input) throws SoFTException {
        while (true) {

            // FIXME: adapt timeout
            // receive messages
            Msg terminate = receive("A", 't', time() + 50);
            Msg order = receive("BCD", 'a', time() + 50);

            // order received
            if (order != null) {
                work();
                sendStatusMessage(order);
            }

            // termination message received
            if (terminate != null) {
                return "0";
            }
        }
    }


    /**
     * Simulates work by putting the thread to sleep.
     */
    private void work() {
        try {
            Thread.sleep(this.getGeschwindigkeit());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendStatusMessage(Msg order) {
        String content = order.getCo();

        // TODO: decrease steps
        try {
            form('s', content).send("A");
        } catch (SoFTException e) {
            e.printStackTrace();
        }
    }
}

public class FTVS_SS18_U01 extends SoFT {

    // TODO
    public int result(String input, String[] output) {
        return 0;
    }

    public static void main(String[] args) {
        abstrakterRechner[] rechner = new abstrakterRechner[]{new Rechner(40, true), new Rechner(10, true),
                new Rechner(25, true), new Rechner(15, true), new Rechner(35, false), new Rechner(10, false)};
        Verteiler[] verteiler = new Verteiler[]{new Verteiler(rechner), new Verteiler(rechner),
                new Verteiler(rechner)};
        Node[] system = new Node[]{new Auftragsknoten(), verteiler[0], verteiler[1], verteiler[2],
                rechner[0], rechner[1], rechner[2], rechner[3], rechner[4], rechner[5]};
        new FTVS_SS18_U01().runSystem(system, "Uebungsblatt 1", "Redundanz und Rekonfiguration",
                "01");
    }
}
