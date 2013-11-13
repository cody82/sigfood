package de.sigfood;

import java.io.Serializable;

public class Bewertung implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public float schnitt;
	public int anzahl;
	public float stddev;
}