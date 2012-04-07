package de.sigfood;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SigfoodActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        
        SigfoodApi sigfood = new SigfoodApi();
        LinearLayout tv = (LinearLayout)this.findViewById(R.id.scroller);

        TextView datum = (TextView)this.findViewById(R.id.datum);
        datum.setText(sigfood.essen.get(0).tag);
        
        LinearLayout parent = tv;//(ScrollView)this.findViewById(R.id.scrollView1);//(ViewGroup) findViewById(R.id.vertical_container);

    	
        for(MensaEssen e : sigfood.essen) {
        		LinearLayout essen = (LinearLayout)LayoutInflater.from(getBaseContext()).inflate(R.layout.mensaessen, null);
        		TextView t2 = (TextView)essen.findViewById(R.id.hauptgerichtBezeichnung);
        		t2.setText(Html.fromHtml(e.hauptgericht.bezeichnung) + "(" + e.hauptgericht.bewertung.schnitt + "/" + e.hauptgericht.bewertung.anzahl + ")");
        		TextView ueberschrift = (TextView)essen.findViewById(R.id.ueberschrift);
        		ueberschrift.setText("Linie: " + e.linie);
        		
        		TextView kommentare1 = (TextView)essen.findViewById(R.id.kommentare1);
        		String tmp = "";
        		for(String s : e.hauptgericht.kommentare) {
        			tmp += "\"" + s + "\"" + "\n";
        		}
        		kommentare1.setText(tmp);
      		
        		RatingBar bar1 = (RatingBar)essen.findViewById(R.id.ratingBar1);
        		bar1.setMax(5);
        		bar1.setProgress((int) (e.hauptgericht.bewertung.schnitt + 0.5f));
        		ImageButton btn = (ImageButton)essen.findViewById(R.id.imageButton1);
        		
        		for(Hauptgericht beilage : e.beilagen) {
        			TextView beilage_bezeichnung = new TextView(getBaseContext(), null, android.R.attr.textAppearanceMedium); 
        			beilage_bezeichnung.setText(Html.fromHtml(beilage.bezeichnung) + "(" + beilage.bewertung.schnitt + "/" + beilage.bewertung.anzahl + ")");
        			essen.addView(beilage_bezeichnung);
        			

            		RatingBar bar2 = new RatingBar(this, null,android.R.attr.ratingBarStyle);
            		bar2.setNumStars(5);
            		bar2.setMax(5);
            		bar2.setStepSize(1);
            		bar2.setRating((int) (beilage.bewertung.schnitt + 0.5f));
        			essen.addView(bar2);
        			
            		TextView kommentare2 = new TextView(getBaseContext(), null, android.R.attr.textAppearanceSmall); 
            		String tmp2 = "";
            		for(String s : beilage.kommentare) {
            			tmp2 += "\"" + s + "\"" + "\n";
            		}
            		kommentare2.setText(tmp2);
        			essen.addView(kommentare2);
        		}
        		
        		
        		
        		btn.setTag(e);
        		btn.setOnClickListener(new Button.OnClickListener() {  
        	        public void onClick(View v)
                    {
        	        	takePhoto(v);
                    }
                 });

        		if(e.hauptgericht.bilder.size() > 0) {
        			int bild_id = e.hauptgericht.bilder.get(0);
	        		URL myFileUrl =null;
	        		try {
	        			myFileUrl= new URL("http://www.sigfood.de/?do=getimage&bildid=" + bild_id + "&width=320");
	        		} catch (MalformedURLException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		}
	        		try {
	        			HttpURLConnection conn= (HttpURLConnection)myFileUrl.openConnection();
	        			conn.setDoInput(true);
	        			conn.connect();
	        			//int length = conn.getContentLength();
	        			//int[] bitmapData =new int[length];
	        			//byte[] bitmapData2 =new byte[length];
	        			InputStream is = conn.getInputStream();
	
	        			Bitmap bmImg = BitmapFactory.decodeStream(is);
		        		btn.setImageBitmap(bmImg);
	        		} catch (IOException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		}
        		}

        		
        		parent.addView(essen);
        }
    }
    
    private Uri imageUri;
    final int TAKE_PICTURE = 19238;
    
    ImageButton phototarget;
    
    public void takePhoto(View v) {
    	ImageButton btn = (ImageButton)v;
    	phototarget = btn;
    	
    	
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        File photo = new File(Environment.getExternalStorageDirectory(),  "sigfood.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    void uploadPic(MensaEssen e, Uri uri) {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://www.sigfood.de/");

        try {

            MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);  
            multipartEntity.addPart("do", new StringBody("4"));
            multipartEntity.addPart("beilagenid", new StringBody("-1"));
            multipartEntity.addPart("datum", new StringBody(e.tag));
            multipartEntity.addPart("gerid", new StringBody(Integer.toString(e.hauptgericht.id)));
            
            File f = new File(Environment.getExternalStorageDirectory(),  "sigfood.jpg");
            multipartEntity.addPart("newimg", new FileBody(f));
            httppost.setEntity(multipartEntity);

            
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            
        } catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
        	throw new RuntimeException(e1);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
        	throw new RuntimeException(e1);
        }

    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case TAKE_PICTURE:
            if (resultCode == Activity.RESULT_OK) {
                Uri selectedImage = imageUri;
                getContentResolver().notifyChange(selectedImage, null);
                ImageView imageView = phototarget;//(ImageView) findViewById(R.id.ImageView);
                ContentResolver cr = getContentResolver();
                Bitmap bitmap;
                try {
                     bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, selectedImage);

                    imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 320, 200, false));
                    uploadPic((MensaEssen)phototarget.getTag(), imageUri);
                    Toast.makeText(this, selectedImage.toString(),Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
                    Log.e("Camera", e.toString());
                }
            }
        }
    }


}