package de.sigfood;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import android.view.View;

public class SigfoodThread extends Thread {
	Date d;
	SigfoodApi sigfood;
	SigfoodActivity act;
	int c;
	
	public SigfoodThread(Date date, SigfoodActivity sfa, int cachettl) {
		d = date;
		if (d==null) d = new Date();
		act = sfa;
		c = cachettl;
	}
	
    public void run() {
    	try {
    		boolean reload = false;
	    	File cache = new File(act.getCacheDir().getPath()+"/"+d.getYear()+"-"+d.getMonth()+"-"+d.getDate());
	    	if (!cache.isFile()) reload=true;
	    	else if (!cache.canRead()) reload=true;
	    	
	    	if (!reload) {
    			FileInputStream fis = new FileInputStream(cache);
                ObjectInputStream ois = new ObjectInputStream(fis);
                sigfood = (SigfoodApi) ois.readObject();
                ois.close();
                fis.close();
                if (sigfood.abrufdatum.getTime() < (new Date()).getTime()-c*60*60*1000) reload=true;
    		}
	    	
	    	if (reload) {
	    		sigfood = new SigfoodApi(d);
	    		cache.delete();
	    		if (cache.createNewFile()) {
	    			if (cache.canWrite()) {
	    				FileOutputStream fos = new FileOutputStream(cache);
	    		        ObjectOutputStream oos = new ObjectOutputStream(fos);
	    		        oos.writeObject(sigfood);
	    		        oos.close();
	    		        fos.close();
	    			}
	    		}
    		}
	        act.runOnUiThread(new Runnable() {
	            public void run() {
	            	act.fillspeiseplanReturn(sigfood);
	            }
	        });
	    } catch(Exception e) {
	    	e.printStackTrace();
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