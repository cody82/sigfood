package de.sigfood;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class PictureThread extends Thread {
	int id;
	int width;
	Activity act;
	ImageView img;
	ImageButton btn;
	ProgressBar load;
	boolean crop;
	boolean doubleSize = false;
	
	public PictureThread(int b, int w, ImageView i, ProgressBar l, Activity sfa) {
		id = b;
		width = w;
		act = sfa;
		img = i;
		load = l;
		crop = true;
	}
	public PictureThread(int b, int w, ImageView i, ProgressBar l, Activity sfa, boolean ds) {
		id = b;
		width = w;
		act = sfa;
		img = i;
		load = l;
		crop = true;
		doubleSize = true;
	}
	public PictureThread(int b, int w, ImageButton i, ProgressBar l, Activity sfa) {
		id = b;
		width = w;
		act = sfa;
		btn = i;
		load = l;
		crop = false;
	}
	
    public void run() {
    	Bitmap bmImg;
    	Bitmap bmImg2 = null;
    	InputStream is = null;
    	boolean reload = false;
    	boolean failed = false;
    	File cache = null;
    	
    	if (id==-1) {
			failed = true;
			bmImg = BitmapFactory.decodeResource(act.getResources(), R.drawable.nophotoavailable003);
			bmImg2 = Bitmap.createScaledBitmap(bmImg, width, (int)((float)bmImg.getHeight()/(float)bmImg.getWidth()*width), true);
    	} else {
	    	try {
		    	try {
		    		cache = new File(act.getCacheDir().getPath()+"/img_"+id+"_"+width+".jpg");
			    	if (!cache.isFile()) reload=true;
			    	else if (!cache.canRead()) reload=true;
			    	
			    	if (!reload) {
		    			long created = cache.lastModified();
		                if (created < (new Date()).getTime()-24*60*60*1000) reload=true;
		                else {
		                	bmImg2 = BitmapFactory.decodeFile(act.getCacheDir().getPath()+"/img_"+id+"_"+width+".jpg");
		                }
		    		}
			    } catch(Exception e) {
			    	e.printStackTrace();
			    }
		    } catch(Exception e) {
		    	e.printStackTrace();
		    }
    	}

		if (reload) {
		    try {
				URL url =null;
				url= new URL("http://www.sigfood.de/?do=getimage&bildid=" + id + "&width=" + width);
				HttpURLConnection conn= (HttpURLConnection)url.openConnection();
				conn.setDoInput(true);
				conn.connect();
				is = conn.getInputStream();
				bmImg = BitmapFactory.decodeStream(is);
			} catch (IOException e1) {
				failed = true;
				bmImg = BitmapFactory.decodeResource(act.getResources(), R.drawable.picdownloadfailed);
				bmImg2 = Bitmap.createScaledBitmap(bmImg, width, (int)((float)bmImg.getHeight()/(float)bmImg.getWidth()*width), true);
			}

			if (is!=null && !failed) {
				int newh;
				if (crop) newh = bmImg.getWidth()/16*9;
				else newh = bmImg.getWidth();
				if (newh>bmImg.getHeight()) newh = bmImg.getHeight();
		    	if (doubleSize && !failed) {
		    		Bitmap bmImg3 = Bitmap.createBitmap(bmImg, 0, (bmImg.getHeight()-newh)/2, bmImg.getWidth(), newh);
		    		bmImg2 = Bitmap.createScaledBitmap(bmImg3, bmImg3.getWidth()*2, bmImg3.getHeight()*2, true); 
		    	} else bmImg2 = Bitmap.createBitmap(bmImg, 0, (bmImg.getHeight()-newh)/2, bmImg.getWidth(), newh);
		    	
		    	if (cache!=null) {
			    	try {
			    		if (cache.createNewFile()) {
			    			if (cache.canWrite()) {
			    				bmImg2.compress(CompressFormat.JPEG, 100, new FileOutputStream(cache));
			    			}
			    		}
				    } catch(Exception e) {
				    	e.printStackTrace();
				    }
		    	}
			}
	    }
	    
		final Bitmap bmFinal = bmImg2;
        act.runOnUiThread(new Runnable() {
            public void run() {
    			if (img!=null) {
    				if (bmFinal!=null) img.setImageBitmap(bmFinal);
    				img.setVisibility(View.VISIBLE);
    			}
    			if (btn!=null) {
    				if (bmFinal!=null) btn.setImageBitmap(bmFinal);
    				btn.setVisibility(View.VISIBLE);
    			}
    			load.setVisibility(View.GONE);
            }
        });
    }
}