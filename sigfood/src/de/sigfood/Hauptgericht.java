package de.sigfood;

import java.util.ArrayList;



public class Hauptgericht {
	public int id;
	public String bezeichnung;
	public ArrayList<String> kommentare = new ArrayList<String>();
	public Bewertung bewertung = new Bewertung();
	public ArrayList<Integer> bilder = new ArrayList<Integer>();
}