package de.sigfood;

import java.util.Date;

public class SigfoodThread extends Thread {
	Date d;
	SigfoodApi sigfood;
	SigfoodActivity act;
	MenuFragment ret;
	
	public SigfoodThread(Date date, SigfoodActivity sfa, MenuFragment frg) {
		d = date;
		act = sfa;
		ret = frg;
	}
	
    public void run() {
    	sigfood = new SigfoodApi(d);
        act.runOnUiThread(new Runnable() {
            public void run() {
            	ret.fillspeiseplanReturn(sigfood);
            }
        });
    }
}