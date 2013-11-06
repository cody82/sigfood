package de.sigfood;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageButton;

public class PictureThread extends Thread {
	URL url;
	SigfoodActivity ret;
	ImageButton btn;
	
	public PictureThread(URL u, ImageButton b, SigfoodActivity sfa) {
		url = u;
		ret = sfa;
		btn = b;
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
			bmImg = BitmapFactory.decodeResource(ret.getResources(), R.drawable.picdownloadfailed);
		}
    	final Bitmap bmImg2 = bmImg;	// java is weird
	    
        ret.runOnUiThread(new Runnable() {
            public void run() {
    			btn.setImageBitmap(bmImg2);
            }
        });
    }
}