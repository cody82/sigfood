package de.sigfood;

import java.util.Date;

public class SigfoodThread extends Thread {
	Date d;
	SigfoodApi sigfood;
	SigfoodActivity act;
	
	public SigfoodThread(Date date, SigfoodActivity sfa) {
		d = date;
		act = sfa;
	}
	
    public void run() {
    	sigfood = new SigfoodApi(d);
        act.runOnUiThread(new Runnable() {
            public void run() {
            	act.fillspeiseplanReturn(sigfood);
            }
        });
    }
}