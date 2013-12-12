package de.sigfood;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class PictureThread extends Thread {
	URL url;
	Activity act;
	ImageView img;
	ImageButton btn;
	ProgressBar load;
	boolean crop;
	boolean doubleSize = false;
	
	public PictureThread(URL u, ImageView i, ProgressBar l, Activity sfa) {
		url = u;
		act = sfa;
		img = i;
		load = l;
		crop = true;
	}
	public PictureThread(URL u, ImageView i, ProgressBar l, Activity sfa, boolean ds) {
		url = u;
		act = sfa;
		img = i;
		load = l;
		crop = true;
		doubleSize = true;
	}
	public PictureThread(URL u, ImageButton i, ProgressBar l, Activity sfa) {
		url = u;
		act = sfa;
		btn = i;
		load = l;
		crop = false;
	}
	
    public void run() {
    	Bitmap bmImg;
    	InputStream is = null;
	    try {
			HttpURLConnection conn= (HttpURLConnection)url.openConnection();
			conn.setDoInput(true);
			conn.connect();
			is = conn.getInputStream();
			bmImg = BitmapFactory.decodeStream(is);
		} catch (IOException e1) {
			bmImg = BitmapFactory.decodeResource(act.getResources(), R.drawable.picdownloadfailed);
		}
		
		int newh;
		if (crop) newh = bmImg.getWidth()/16*9;
		else newh = bmImg.getWidth();
		if (newh>bmImg.getHeight()) newh = bmImg.getHeight();
    	final Bitmap bmImg2;
    	if (doubleSize) {
    		Bitmap bmImg3 = Bitmap.createBitmap(bmImg, 0, (bmImg.getHeight()-newh)/2, bmImg.getWidth(), newh);
    		bmImg2 = Bitmap.createScaledBitmap(bmImg3, bmImg3.getWidth()*2, bmImg3.getHeight()*2, true); 
    	} else bmImg2 = Bitmap.createBitmap(bmImg, 0, (bmImg.getHeight()-newh)/2, bmImg.getWidth(), newh);
	    
        act.runOnUiThread(new Runnable() {
            public void run() {
    			if (img!=null) {
    				img.setImageBitmap(bmImg2);
    				img.setVisibility(View.VISIBLE);
    			}
    			if (btn!=null) {
    				btn.setImageBitmap(bmImg2);
    				btn.setVisibility(View.VISIBLE);
    			}
    			load.setVisibility(View.GONE);
            }
        });
    }
}