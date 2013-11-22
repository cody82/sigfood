package de.sigfood;

import java.io.Serializable;
import java.util.ArrayList;

public class Hauptgericht implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public int id;
	public String bezeichnung;
	public ArrayList<Kommentar> kommentare = new ArrayList<Kommentar>();
	public Bewertung bewertung = new Bewertung();
	public ArrayList<Integer> bilder = new ArrayList<Integer>();
	public float preis_stud;
	public float preis_bed;
	public float preis_gast;
}