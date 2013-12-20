package de.sigfood;

import java.util.Date;

import android.view.View;

public class SigfoodThread extends Thread {
	Date d;
	SigfoodApi sigfood;
	SigfoodActivity act;
	
	public SigfoodThread(Date date, SigfoodActivity sfa) {
		d = date;
		act = sfa;
	}
	
    public void run() {
    	try {
	    	sigfood = new SigfoodApi(d);
	        act.runOnUiThread(new Runnable() {
	            public void run() {
	            	act.fillspeiseplanReturn(sigfood);
	            }
	        });
	    } catch(Exception e) {
	        act.runOnUiThread(new Runnable() {
	            public void run() {
	            	View v = (View)act.findViewById(R.id.mainLoading);
	        		v.setVisibility(View.GONE);
	            	v = (View)act.findViewById(R.id.mainNoConnection);
	        		v.setVisibility(View.VISIBLE);
	            }
	        });
	    }
    }
}