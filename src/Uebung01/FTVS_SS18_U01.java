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

    //TODO needs testing
    //list when order with orderID was last updated
    private long[][] updatedOrders = new long[60][2];

    /**
     * Main method, runs the Auftragsknoten.
     *
     * @param input optional input
     * @return 0 if executed properly
     */
    public String runNode(String input) throws SoFTException {

        int amountOfOrderBundles = 10;

        for (int i = 0; i < amountOfOrderBundles; i++) {
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

        boolean flag = true;
        while (flag) {
            receiveStatusMessages();
            if (orders.isEmpty()) {
                flag = false;
            }
        }

        // generate and send message to terminate other computers
        form('t', "").send(distributors + "EFGHIJ");
        ++FTVS_SS18_U01.N;

        FTVS_SS18_U01.L += time();
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
        FTVS_SS18_U01.A += numberOfOrders;

        StringBuilder a = new StringBuilder("");

        for (int i = 0; i < numberOfOrders; ++i) {
            if (i > 0) {
                a.append(" ; ");
            }

            // determine workload randomly
            int workload = random.nextInt((6 - 1) + 1) + 1;

            a.append(++orderID).append(" 0 ").append(workload);

            // store order in list
            String order = orderID + " 0 " + workload;
            orders.add(order);
            //TODO needs testing
            //create new entry for new order
            for(int x = 0; x < updatedOrders.length; ++x) {
               if(updatedOrders[x][0] == 0) {
                   updatedOrders[x][0] = (long) orderID;
                   updatedOrders[x][1] = time();
                   break;
                }
            }
        }
        return a + "";
    }


    /**
     * Sends creates orders to the distributors.
     */
    private void sendOrders() {

        // create new orders
        String order = createOrders();

        // generate message and send it to distributors
        try {
            form('a', order).send(distributors);
            ++FTVS_SS18_U01.N;
        } catch (SoFTException e) {
            e.printStackTrace();
        }
    }


    /**
     * Receives status messages and stores them.
     */
    private void receiveStatusMessages() throws SoFTException {

        // receive messages
        Msg statusE = receive("E", time() + 10);
        Msg statusF = receive("F", time() + 10);
        Msg statusG = receive("G", time() + 10);
        Msg statusH = receive("H", time() + 10);
        Msg statusI = receive("I", time() + 10);
        Msg statusJ = receive("J", time() + 10);

        // store messages
        if (statusE != null) { processStatusMessage(statusE); }
        if (statusF != null) { processStatusMessage(statusF); }
        if (statusG != null) { processStatusMessage(statusG); }
        if (statusH != null) { processStatusMessage(statusH); }
        if (statusI != null) { processStatusMessage(statusI); }
        if (statusJ != null) { processStatusMessage(statusJ); }

        checkTimeout();
    }

    //TODO needs testing
    //checks for order timeouts, in case of no-message-error
    private void checkTimeout() {
        for(int i = 0; i < updatedOrders.length; ++i) {
            if(updatedOrders[i][1] > time() + 100) {
                //resend order
                //TODO needs testing
                for(String s : orders) {
                    if (number(s, 1) == updatedOrders[i][0]) {
                        try {
                            form('a', s).send(distributors);
                            ++FTVS_SS18_U01.N;
                        } catch (SoFTException e) {
                            e.printStackTrace();
                        }
                    }
                }
                updatedOrders[i][1] = time();
            }
        }
    }


    private void updateOrders(Msg msg) {
        String s = msg.getCo();
        int ID = number(s, 1);
        int steps = number(s, 2);
        int workload = number(s, 3);

        String toAdd = null;
        String toDelete = null;
        for (String o : orders) {
            if (number(o, 1) == ID) {
                toDelete = o;
                if (steps < workload) {
                    toAdd = s;
                }
            }
        }
        orders.remove(toDelete);
        if (toAdd != null) {
            orders.add(toAdd);

            //TODO needs testing
            //update time if order isn't completed yet
            for(int i = 0; i < updatedOrders.length; ++i) {
                if(updatedOrders[i][0] == ID) {
                    updatedOrders[i][1] = time();
                    break;
                }
            }

        } else {

            ++FTVS_SS18_U01.E;

            //TODO needs testing
            //delete entry if order is completed
            for(int i = 0; i < updatedOrders.length; ++i) {
                if(updatedOrders[i][0] == ID) {
                    updatedOrders[i][0] = updatedOrders[i][1] = 0;
                    break;
                }
            }
        }
    }


    private void processStatusMessage(Msg status) {
        if (checkForFaults(status.getCo())) {
            try {
                form('r', status.getSe()).send(distributors);
                ++FTVS_SS18_U01.N;
                sendLastMessage(number(status.getCo(), 1));
                ++FTVS_SS18_U01.R;
            } catch (SoFTException e) {
                e.printStackTrace();
            }
        } else {
            updateOrders(status);
        }
    }

    private boolean checkForFaults(String message) {
        int ID = number(message, 1);
        int steps = number(message, 2);
        int workload = number(message, 3);
        boolean flag = false;

        for (String s : orders) {
            if (number(s, 1) == ID) {
                flag = true;
                if (steps == number(s, 2) + 1 && workload == number(s, 3)) {
                    return false;
                }
            }
        }
        return flag;
    }

    private void sendLastMessage(int ID) {
        for (String s : orders) {
            if (number(s, 1) == ID) {
                try {
                    form('a', s).send(distributors);
                    ++FTVS_SS18_U01.N;
                } catch (SoFTException e) {
                    e.printStackTrace();
                }
                break;
            }
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

    Verteiler(abstrakterRechner[] rechner) {
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
     *
     * @param message the message to distribute
     */
    private void distributeOrder(Msg message) {
        String tmp = message.getCo();

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
                ++FTVS_SS18_U01.N;
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
        int dif = 100;
        abstrakterRechner id = null;
        int faultisSpeed = 0;

        for (abstrakterRechner ar : rechner) {
            if (ar.myChar() == message.getCo().charAt(0)) {
                faultisSpeed = ar.getGeschwindigkeit();
                ar.setStatus(false);
                break;
            }
        }
        for (abstrakterRechner ar : rechner) {
            if (!ar.getStatus()) {
                if (ar.getGeschwindigkeit() > faultisSpeed) {
                    if (ar.getGeschwindigkeit() - faultisSpeed < dif) {
                        dif = ar.getGeschwindigkeit() - faultisSpeed;
                        id = ar;
                    }
                } else {
                    if (faultisSpeed - ar.getGeschwindigkeit() < dif) {
                        dif = ar.getGeschwindigkeit() - faultisSpeed;
                        id = ar;
                    }
                }
            }
        }
        if (id != null) {
            id.setStatus(true);
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

    abstrakterRechner(int geschwindigkeit, boolean status) {
        this.geschwindigkeit = geschwindigkeit;
        this.status = status;
    }

    int getGeschwindigkeit() {
        return geschwindigkeit;
    }

    void setStatus(boolean status) {
        this.status = status;
    }

    boolean getStatus() {
        return status;
    }
}

/**
 * Aufgabe 1c
 */
class Rechner extends abstrakterRechner {

    private ArrayList<String> ordersB = new ArrayList<>();
    private ArrayList<String> ordersC = new ArrayList<>();
    private ArrayList<String> ordersD = new ArrayList<>();
    private ArrayList<String> orderList = new ArrayList<>();
    private ArrayList<String> removeList = new ArrayList<>();
    private String remove;

    Rechner(int geschwindigkeit, boolean status) {
        super(geschwindigkeit, status);
    }

    public String runNode(String input) throws SoFTException {
        while (true) {

            // FIXME: adapt timeout
            // receive messages
            Msg terminate = receive("A", 't', time() + 50);

            //save orders
            // FIXME: differentiate between distributors
            Msg b = receive("B", 'a', time() + 50);
            if (b != null) {
                ordersB.add(b.getCo());
            }
            Msg c = receive("C", 'a', time() + 50);
            if (c != null) {
                ordersC.add(c.getCo());
            }
            Msg d = receive("D", 'a', time() + 50);
            if (d != null) {
                ordersD.add(d.getCo());
            }

            //check 2 of 3
            findOrders();

            //work orders
            for (String m : orderList) {
                int ID = number(m, 1);
                int steps = number(m, 2);
                int workload = number(m, 3);
                removeList.add(m);

                while (steps < workload) {
                    work();
                    ++steps;
                    String content = Integer.toString(ID) + " " + Integer.toString(steps) + " " + Integer.toString(workload);

                    try {
                        form('s', content).send("A");
                        ++FTVS_SS18_U01.N;
                    } catch (SoFTException e) {
                        e.printStackTrace();
                    }
                }
            }


            for (String r : removeList) {
                orderList.remove(r);
            }
            removeList.clear();


            // termination message received
            if (terminate != null) {
                return "0";
            }
        }
    }

    private void findOrders() {
        for (String b : ordersB) {
            for (String c : ordersC) {
                if (b.equals(c)) {
                    orderList.add(b);
                    removeList.add(b);
                }
            }
        }
        updateOrders();

        for (String b : ordersB) {
            for (String d : ordersD) {
                if (b.equals(d)) {
                    orderList.add(b);
                    removeList.add(b);
                }
            }
        }
        updateOrders();

        for (String c : ordersC) {
            for (String d : ordersD) {
                if (c.equals(d)) {
                    orderList.add(c);
                    removeList.add(c);
                }
            }
        }
        updateOrders();
    }

    private void updateOrders() {
        for (String i : removeList) {
            ordersB.remove(i);
            ordersC.remove(i);
            ordersD.remove(i);
        }
        removeList.clear();
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
}

public class FTVS_SS18_U01 extends SoFT {

    static int A = 0;
    static int E = 0;
    static int R = 0;
    static long L = 0;
    static int N = 0;


    // TODO
    public int result(String input, String[] output) {

        // run 200 times
        if (exec() == 1) {

            // print out results
            System.out.println("A: " + A);
            System.out.println("E: " + E);
            System.out.println("R: " + R);
            System.out.println("L: " + L);
            System.out.println("N: " + N);

            // terminate program
            System.exit(0);
        }
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
