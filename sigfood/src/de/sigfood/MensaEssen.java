package de.sigfood;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class MensaEssen implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String linie;
	public Hauptgericht hauptgericht;
	public ArrayList<Hauptgericht> beilagen = new ArrayList<Hauptgericht>();
	public Date datumskopie;
}
