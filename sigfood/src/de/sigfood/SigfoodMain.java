package de.sigfood;


public class SigfoodMain {

	public static void main(String[] args) {

        SigfoodApi sigfood = new SigfoodApi();
        
        for(MensaEssen e : sigfood.essen) {
        	System.out.println(e.hauptgericht.bezeichnung);
        }
	}

}
