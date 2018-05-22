/*
  Fehlertolerante verteilte Systeme, Sommersemester 2018
  Valentin Fitz (fitz@dc.uni-due.de)
  Verlaesslichkeit von Rechensystemen, Universitaet Duisburg-Essen
  <p>
  �bungsblatt 01, Codegeruest
  <p>
  Bemerkung: Bitte benutzen Sie zur Implementierung Ihrer L�sung dieses Codegeruest. Sie d�rfen die
  vorgegebenen Klassen nach belieben erweitern, sollten jedoch darauf achten, dass Ihre L�sung dem Rahmen
  der Spezifikation des Aufgabenblattes entspricht. Sie d�rfen ebenfalls bei Bedarf eigene Hilfsklassen
  schreiben. Vermeiden Sie es jedoch bitte, Ihre L�sung in mehrere Dateien aufzuteilen.
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

    // ID of the last order
    private static int orderID = 0;

    // list of distributors
    private String distributors = "BCD";

    // list of current orders
    private ArrayList<String> orders = new ArrayList<>();

    //list of orderID and time when it was last updated
    private long[][] updatedOrders = new long[60][2];


    /**
     * Main method, runs the Auftragsknoten.
     *
     * @param input optional input
     * @return 0 if executed properly
     */
    public String runNode(String input) throws SoFTException {

        // amount of bundles of orders to be created, default is 10
        int amountOfOrderBundles = 10;

        // send orders every 300 ms while receiving status messages
        for (int i = 0; i < amountOfOrderBundles; i++) {
            boolean flag = true;
            while (flag) {
                if (time() >= 300 * i) {
                    sendBundles();
                    flag = false;
                } else {
                    receiveStatusMessages();
                }
            }
        }

        // wait until list of orders is empty
        boolean flag = true;
        while (flag) {
            receiveStatusMessages();
            if (orders.isEmpty()) {
                flag = false;
            }
        }

        // generate and send message to terminate other computers
        form('t', "").send(distributors + "EFGHIJ");

        // update N
        ++FTVS_SS18_U01.N;

        // update L
        FTVS_SS18_U01.L += time();
        return "0";
    }


    /**
     * Creates new bundles and stores them in the list
     *
     * @return the created orders
     */
    private String createBundle() {

        // determine number of orders randomly
        int numberOfOrders = (random.nextInt((6 - 1) + 1) + 1);

        // update A
        FTVS_SS18_U01.A += numberOfOrders;

        // create bundle of orders
        StringBuilder a = new StringBuilder();
        for (int i = 0; i < numberOfOrders; ++i) {
            if (i > 0) {
                a.append(" ; ");
            }

            // set random workload and append it to the bundle
            int workload = random.nextInt((6 - 1) + 1) + 1;
            a.append(++orderID).append(" 0 ").append(workload);

            // store order in list
            String order = orderID + " 0 " + workload;
            orders.add(order);

            // create new entry for new order
            for (int x = 0; x < updatedOrders.length; ++x) {
                if (updatedOrders[x][0] == 0) {
                    updatedOrders[x][0] = (long) orderID;
                    updatedOrders[x][1] = time();
                    break;
                }
            }
        }
        return a + "";
    }


    /**
     * Sends bundles of orders to the distributors.
     */
    private void sendBundles() {

        // create new bundle
        String bundle = createBundle();

        // generate message and send it to distributors
        try {
            form('a', bundle).send(distributors);

            // update N
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
        Msg statusE = receive("E", time() + 5);
        Msg statusF = receive("F", time() + 5);
        Msg statusG = receive("G", time() + 5);
        Msg statusH = receive("H", time() + 5);
        Msg statusI = receive("I", time() + 5);
        Msg statusJ = receive("J", time() + 5);

        // store messages
        if (statusE != null) {
            processStatusMessage(statusE);
        }
        if (statusF != null) {
            processStatusMessage(statusF);
        }
        if (statusG != null) {
            processStatusMessage(statusG);
        }
        if (statusH != null) {
            processStatusMessage(statusH);
        }
        if (statusI != null) {
            processStatusMessage(statusI);
        }
        if (statusJ != null) {
            processStatusMessage(statusJ);
        }

        // check for timeout
        checkTimeout();
    }


    /**
     * Checks for order timeouts in case of no-message-error.
     */
    private void checkTimeout() {
        StringBuilder resendMessage = new StringBuilder();

        // iterate through updatedOrders list
        for (int i = 0; i < updatedOrders.length; ++i) {

            // check if position i is occupied and last update more than 500ms old
            if (updatedOrders[i][0] != 0 && time() > updatedOrders[i][1] + 500) {

                // find full order
                for (String s : orders) {
                    if (number(s, 1) == updatedOrders[i][0]) {

                        // append order to resendMessage
                        if (!resendMessage.toString().equals("")) {
                            resendMessage.append(" ; ");
                        }
                        resendMessage.append(s);
                        break;
                    }
                }

                // update time, order was last updated
                updatedOrders[i][1] = time();
            }
        }

        // send resendMessage
        try {
            if (!resendMessage.toString().equals("")) {
                form('a', resendMessage.toString()).send(distributors);

                // update N
                ++FTVS_SS18_U01.N;
            }
        } catch (SoFTException e) {
            e.printStackTrace();
        }
    }


    /**
     * Keeps orders-list up to date.
     *
     * @param msg message with info to update list
     */
    private void updateOrders(Msg msg) {

        // content of message
        String content = msg.getCo();

        int ID = number(content, 1);
        int progress = number(content, 2);
        int workload = number(content, 3);

        String toAdd = null;
        String toDelete = null;

        // search order with given ID and mark it for deletion
        for (String o : orders) {
            if (number(o, 1) == ID) {
                toDelete = o;

                // mark updated entry for adding
                if (progress < workload) {
                    toAdd = content;
                }
            }
        }

        // remove marked entry
        orders.remove(toDelete);
        if (toAdd != null) {

            // add marked entry
            orders.add(toAdd);

            // update time if order isn't completed yet
            for (int i = 0; i < updatedOrders.length; ++i) {
                if (updatedOrders[i][0] == ID) {
                    updatedOrders[i][1] = time();
                    break;
                }
            }

        } else {

            // update E
            ++FTVS_SS18_U01.E;

            // delete entry if order is completed
            for (int i = 0; i < updatedOrders.length; ++i) {
                if (updatedOrders[i][0] == ID) {
                    updatedOrders[i][0] = updatedOrders[i][1] = 0;
                    break;
                }
            }
        }
    }


    /**
     * Checks status message for faults.
     *
     * @param status the message to process
     */
    private void processStatusMessage(Msg status) {

        // fault found
        if (checkForFaults(status.getCo())) {
            try {

                // send reconfiguration message to distributors
                form('r', status.getSe()).send(distributors);

                // update N
                ++FTVS_SS18_U01.N;

                // resends order
                resendOrder(number(status.getCo(), 1));

                // update R
                ++FTVS_SS18_U01.R;
            } catch (SoFTException e) {
                e.printStackTrace();
            }

            // message is not faulty
        } else {

            // update orders list
            updateOrders(status);
        }
    }


    /**
     * Checks if a given message is faulty.
     *
     * @param message the message to check
     * @return true if faulty, false otherwise
     */
    private boolean checkForFaults(String message) {
        int ID = number(message, 1);
        int progress = number(message, 2);
        int workload = number(message, 3);
        boolean flag = false;

        // checks for errors in progress and workload
        for (String s : orders) {
            if (number(s, 1) == ID) {
                flag = true;
                if (progress == number(s, 2) + 1 && workload == number(s, 3)) {
                    return false;
                }
            }
        }
        return flag;
    }


    /**
     * Resends orders.
     *
     * @param ID the ID of the order to resend
     */
    private void resendOrder(int ID) {

        // search for order in orders list for last status of order
        for (String s : orders) {
            if (number(s, 1) == ID) {
                try {

                    // send new order
                    form('a', s).send(distributors);

                    // update N
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

    // list containing ID of order and the corresponding Rechner
    private ArrayList<String> orderArray = new ArrayList<>();

    Verteiler(abstrakterRechner[] rechner) {
        this.rechner = rechner;
    }


    public String runNode(String input) throws SoFTException {
        while (true) {

            // receive a, t or r messages
            Msg order = receive("A", 'a', time() + 5);
            Msg reconfiguration = receive("A", 'r', time() + 5);
            Msg terminate = receive("A", 't', time() + 5);

            // reconfiguration message received
            if (reconfiguration != null) {
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
        String content = message.getCo();

        int wordCount = getItemCount(content);

        // add orders to order list
        for (int i = 1; i <= wordCount; ++i) {
            orderList.add(words(content, i, 1, 3));
        }

        // temporary list of Rechner
        ArrayList<abstrakterRechner> computerList = new ArrayList<>();

        // add active Rechner to temporary list
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
            for (String o : orderList) {
                if (o != null) {
                    int w = number(o, 3);
                    if (w == max) {
                        abstrakterRechner AR = sendOrder(o, computerList);

                        // add ID and Rechner to ordersMap
                        if (orderArray != null && AR != null) {
                            orderArray.add(number(o, 1) + " " + AR.myChar());
                        }

                        // remove Rechner so that it is unable to receive another order
                        computerList.remove(AR);

                        // fill list if empty
                        if (computerList.isEmpty()) {
                            for (abstrakterRechner ar : rechner) {
                                if (ar.getStatus()) {
                                    computerList.add(ar);
                                }
                            }
                        }

                        // order has been processed and can be removed
                        orderList.remove(o);

                        break;
                    }
                }
            }
        }
    }


    /**
     * Sends order to Rechner.
     *
     * @param message     the message to send
     * @param rechnerList the list of Rechner
     * @return abstrakterRechner
     */
    private abstrakterRechner sendOrder(String message, ArrayList<abstrakterRechner> rechnerList) {

        abstrakterRechner fastestRechner = null;
        int computingTime = Integer.MAX_VALUE;

        // find fastest Rechner
        for (abstrakterRechner as : rechnerList) {
            if (as.getGeschwindigkeit() < computingTime && as.getStatus() && !isFaulty(number(message, 1), as.myChar())) {
                computingTime = as.getGeschwindigkeit();
                fastestRechner = as;
            }
        }

        // send order to fastestRechner
        if (fastestRechner != null) {
            Msg order = form('a', message);
            try {
                order.send(fastestRechner.myChar());

                // update N
                ++FTVS_SS18_U01.N;
            } catch (SoFTException e) {
                e.printStackTrace();
            }
        }
        return fastestRechner;
    }


    /**
     * Checks if an entry in the ordersMap is faulty.
     *
     * @param ID   the ID to check
     * @param NAME the name of the Rechner to check
     * @return true if faulty, false otherwise
     */
    private boolean isFaulty(int ID, char NAME) {
        for (String o : orderArray) {
            int id = number(o, 1);
            char name = word(o, 2).charAt(0);

            if (id == ID && name == NAME) {
                return true;
            }
        }
        return false;
    }


    /**
     * Marks a Rechner as inactive.
     *
     * @param message the reconfiguration message
     */
    private void reconfigurate(Msg message) {
        int difference = Integer.MAX_VALUE;
        abstrakterRechner AR = null;
        int speedOfFaultyRechner = 0;

        // set Rechner inactive
        for (abstrakterRechner ar : rechner) {
            if (ar.myChar() == message.getCo().charAt(0)) {
                speedOfFaultyRechner = ar.getGeschwindigkeit();
                ar.setStatus(false);
                break;
            }
        }

        // select inactive Rechner with lowest difference in speed and activate it
        for (abstrakterRechner ar : rechner) {
            if (!ar.getStatus()) {
                if (ar.getGeschwindigkeit() > speedOfFaultyRechner) {
                    if (ar.getGeschwindigkeit() - speedOfFaultyRechner < difference) {
                        difference = ar.getGeschwindigkeit() - speedOfFaultyRechner;
                        AR = ar;
                    }
                } else {
                    if (speedOfFaultyRechner - ar.getGeschwindigkeit() < difference) {
                        difference = ar.getGeschwindigkeit() - speedOfFaultyRechner;
                        AR = ar;
                    }
                }
            }
        }

        // activate Rechner
        if (AR != null) {
            AR.setStatus(true);
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

    // lists of orders
    private ArrayList<String> ordersB = new ArrayList<>();
    private ArrayList<String> ordersC = new ArrayList<>();
    private ArrayList<String> ordersD = new ArrayList<>();

    // list of accepted orders
    private ArrayList<String> orderList = new ArrayList<>();

    // list of orders to be removed
    private ArrayList<String> removeList = new ArrayList<>();

    Rechner(int geschwindigkeit, boolean status) {
        super(geschwindigkeit, status);
    }

    public String runNode(String input) throws SoFTException {
        while (true) {

            // receive terminate message
            Msg terminate = receive("A", 't', time() + 5);

            // receive and save orders
            Msg b = receive("B", 'a', time() + 5);
            if (b != null) {
                ordersB.add(b.getCo());
            }
            Msg c = receive("C", 'a', time() + 5);
            if (c != null) {
                ordersC.add(c.getCo());
            }
            Msg d = receive("D", 'a', time() + 5);
            if (d != null) {
                ordersD.add(d.getCo());
            }

            //check 2 of 3
            findOrders();

            // work orders
            for (String m : orderList) {
                int ID = number(m, 1);
                int progress = number(m, 2);
                int workload = number(m, 3);
                removeList.add(m);

                while (progress < workload) {
                    work();

                    // update progress
                    ++progress;
                    String content = Integer.toString(ID) + " " + Integer.toString(progress) + " " + Integer.toString(workload);

                    // send updated order
                    try {
                        form('s', content).send("A");

                        // update N
                        ++FTVS_SS18_U01.N;
                    } catch (SoFTException e) {
                        e.printStackTrace();
                    }
                }
            }

            // clear remove list
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


    /**
     * Accepts order if 2 of 3 Verteiler sent it
     */
    private void findOrders() {
        for (String b : ordersB) {
            for (String c : ordersC) {
                acceptOrder(b, c);
            }
        }
        updateOrders();

        for (String b : ordersB) {
            for (String d : ordersD) {
                acceptOrder(b, d);
            }
        }
        updateOrders();

        for (String c : ordersC) {
            for (String d : ordersD) {
                acceptOrder(c, d);
            }
        }
        updateOrders();
    }


    /**
     * If a == b accepts order and marks it for removal of ordersB, ordersC and ordersD list.
     *
     * @param a first order
     * @param b second order
     */
    private void acceptOrder(String a, String b) {
        if (a.equals(b)) {
            orderList.add(a);
            removeList.add(a);
        }
    }


    /**
     * Removes marked orders from lists and clears removeList.
     */
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


    public int result(String input, String[] output) {

        // run 200 times
        if (exec() == 50) {

            // print out results
            System.out.println("A: " + A);
            System.out.println("E: " + E);
            System.out.println("R: " + R);
            System.out.println("L: " + L);
            System.out.println("N: " + N);

            System.out.println();

            System.out.println("A0: " + A / 100);
            System.out.println("E0: " + E / 100);
            System.out.println("R0: " + R / 100);
            System.out.println("L0: " + L / 100);
            System.out.println("N0: " + N / 100);

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
