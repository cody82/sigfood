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
	
	public PictureThread(URL u, ImageView i, ProgressBar l, Activity sfa) {
		url = u;
		act = sfa;
		img = i;
		load = l;
	}
	public PictureThread(URL u, ImageButton i, ProgressBar l, Activity sfa) {
		url = u;
		act = sfa;
		btn = i;
		load = l;
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
    	final Bitmap bmImg2 = bmImg;	// java is weird
	    
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