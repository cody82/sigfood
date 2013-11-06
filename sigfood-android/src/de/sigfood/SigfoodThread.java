package de.sigfood;

import java.util.Date;

public class SigfoodThread extends Thread {
	Date d;
	SigfoodApi sigfood;
	SigfoodActivity ret;
	
	public SigfoodThread(Date date, SigfoodActivity sfa) {
		d = date;
		ret = sfa;
	}
	
    public void run() {
    	sigfood = new SigfoodApi(d);
        ret.runOnUiThread(new Runnable() {
            public void run() {
            	ret.fillspeiseplanReturn(sigfood);
            }
        });
    }
}