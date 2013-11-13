package de.sigfood;

import java.io.Serializable;
import java.util.ArrayList;

public class Hauptgericht implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public int id;
	public String bezeichnung;
	public ArrayList<String> kommentare = new ArrayList<String>();
	public Bewertung bewertung = new Bewertung();
	public ArrayList<Integer> bilder = new ArrayList<Integer>();
}