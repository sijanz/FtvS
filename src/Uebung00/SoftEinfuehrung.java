package Uebung00;

import SoFTlib.*;
import static SoFTlib.Helper.*;

abstract class AbstractNode extends Node {

	// Bildung der Signatur modulo mod
	long mod = 10000;

	// Geheimer Schlüssel-Faktor von Knoten A
	long geheimFaktor = 2317;

	// Oeffentlicher Schluessel-Faktor von Knoten A
	long oeffFaktor = 3917;

	// Oeffentliches Produkt von Knoten A
	long oeffProdukt = geheimFaktor * oeffFaktor % mod;

	public long komprimiere(String inhalt) {
		long k = 0;
		if (inhalt != null) {
			for (int i = 0; i < inhalt.length(); i++) {
				k = 128 * k + inhalt.charAt(i) % 128;
				k %= 8357;
			}
		}
		return k;
	}

	/**
	 * Erstellt eine Signatur f�r einen Nachrichhteninhalt
	 * 
	 * @param sender
	 *            Bezeichnung des Senders
	 * @param inhalt
	 *            Der zu signierende Inhalt einer Nachricht
	 * @return Signatur
	 */
	public String signiere(String sender, String inhalt) {
		long signatur = (komprimiere(inhalt) * geheimFaktor) % mod;

		// TODO
		return String.format("%d", signatur);
	}

	/**
	 * Abstrakte Methode zur �berpr�fung einer Signatur. Wird nur in Knoten B verwendet und dort
	 * sinnvoll �berschrieben.
	 * 
	 * @param nachricht
	 *            Vollst�ndiger Inhalt einer Nachricht inkl. der darin enthaltenen Signatur in der
	 *            Form "<Daten>;<Signatur>"
	 * @return Bei erfolgreicher Pr�fung wird die <Signatur> zur�ckgegeben, sonst null.
	 */
	public abstract String pruefe(String nachricht);
}

class Sender extends AbstractNode {

	public String runNode(String input) throws SoFTException {
		// Erstes Wort des eingegebenen Input-Strings auslesen
		String Daten = words(input, 1, 1, 1);
		// Nachricht formen ("<Daten>;<Signatur>") und an Knoten B senden
		Msg m1 = form('n', Daten + ";" + signiere("A", Daten)).send("B");

		for (int i = 1; i <= 5; i++) {
			m1.send("B");
			Msg m = receive("B", 'q', 100 * i);
			if (m != null) {
				return (i == 1 ? "0" : "1");
			}
		}
		return "1";
	}

	@Override
	public String pruefe(String inhalt) {
		return "";
	}
}

class Empfaenger extends AbstractNode {

	public String runNode(String input) throws SoFTException {
		Msg m;
		while (time() < 600) {
			m = receive("A", 'n', 600);
			if (m != null) {
				if (pruefe(m.getCo()) != null) {
					form('q', "").send("A");
				}
			}
		}
		return "0";
	}

	@Override
	public String pruefe(String nachricht) {
		String signatur = words(nachricht, 2, 1, 1);

		// Pr�ft, ob die Nachricht tats�chlich von Knoten A stammt.
		if (signatur != null && signatur.length() == 1) {
			int s = (int) signatur.charAt(0);
			return (s >= 97 && s % 2 == 1 ? signatur : null);
		} else {
			return null;
		}
	}
}

public class SoftEinfuehrung extends SoFT {

	/**
	 * Ermittelt das Ergebnis einer vollst�ndigen einzelnen Simulation, basierend auf den
	 * individuellen Ausgaben aller Knoten, die benutzt wurden.
	 * 
	 * Hier: <br>
	 * Result 0 = Erste Nachricht von A ist ok. <br>
	 * Result 1 = Erste Nachricht vom Sender A kommt nicht an, zweite ist ok. <br>
	 * Result 2 = Erste Nachricht vom Sender A kommt falsch an, zweite ist ok. <br>
	 * Result 3 = Empf�nger B erh�lt keine korrekte Nachricht von A. <br>
	 * Result 4 = Empf�nger B entscheidet sich f�r einen falschen Inhalt. <br>
	 * Result 5 = keine Bedeutung.
	 */
	public int result(String input, String[] output) {
		if (number(words(output[1], 1, 1, 1)) <= 1 && !words(output[1], 1, 2, 2).equals(input))
			return 4;
		else
			return number(words(output[0], 1, 1, 1)) + number(words(output[1], 1, 1, 1));
	}

	public static void main(String[] args) {
		new SoftEinfuehrung().runSystem(new Node[] { new Sender(), new Empfaenger() },
				"Einf�hrungs�bung", "Einf�hrungs�bung f�r die Einarbeitung mit SoFT", "Simon Janzon");
	}
}
