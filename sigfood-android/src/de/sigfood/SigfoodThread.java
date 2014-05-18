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
	SigfoodApi sigfood = null;
	SigfoodActivity act;
	int c;
	boolean i;
	
	boolean stop = false;
	
	public SigfoodThread(Date date, SigfoodActivity sfa, int cachettl, boolean ignorecache) {
		d = date;
		if (d==null) d = new Date();
		act = sfa;
		c = cachettl;
		i = ignorecache;
	}
	
    public void run() {
    	boolean reload = false;
    	File cache = null;
    	
    	try {
	    	cache = new File(act.getCacheDir().getPath()+"/"+d.getYear()+"-"+d.getMonth()+"-"+d.getDate());
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
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }
    	
	    try {
	    	if (reload || i || sigfood==null) {
	    		sigfood = new SigfoodApi(d);
	    		if (cache!=null) {
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
    		}
	    	if (stop) return;
	    	
	        act.runOnUiThread(new Runnable() {
	            public void run() {
	            	act.fillspeiseplanReturn(sigfood);
	            }
	        });
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	if (stop) return;
	    	
	    	if (sigfood!=null) {
		        act.runOnUiThread(new Runnable() {
		            public void run() {
		            	act.fillspeiseplanReturn(sigfood);
		            }
		        });
	    	} else {
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
}