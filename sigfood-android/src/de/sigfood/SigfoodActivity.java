package de.sigfood;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

		LinearLayout tv = (LinearLayout)this.findViewById(R.id.scroller);
		tv.removeAllViews();
		TextView loadingtext = new TextView(getBaseContext(), null, android.R.attr.textAppearanceMedium); 
		loadingtext.setText("Starting up");
		tv.addView(loadingtext);
		

		Button prev_date = (Button)this.findViewById(R.id.prev_date);
		Button next_date = (Button)this.findViewById(R.id.next_date);
		
		next_date.setOnClickListener(new Button.OnClickListener() {  
			public void onClick(View v)
			{
				if (sigfood != null) {
					if (sigfood.naechstertag != null) {
						fillspeiseplan(sigfood.naechstertag);
					}
				}
			}
		});
		prev_date.setOnClickListener(new Button.OnClickListener() {  
			public void onClick(View v)
			{
				if (sigfood != null) {
					if (sigfood.vorherigertag != null) {
						fillspeiseplan(sigfood.vorherigertag);
					}
				}
			}
		});
		
		fillspeiseplan(null);
	}
	
	SigfoodApi sigfood;
	
	public void fillspeiseplan(Date d) {

		LinearLayout parent = (LinearLayout)this.findViewById(R.id.scroller);
		TextView datum = (TextView)this.findViewById(R.id.datum);

		/* First clear and show loading text */
		datum.setText("Loading...");
		parent.removeAllViews();

		/* This actually downloads the plan, so can be rather costly */
		sigfood = new SigfoodApi(d);

		/* Now start to fill plan and download pictures */
		final Date sfspd = sigfood.speiseplandatum;
		datum.setText(String.format("%tA, %td.%tm.%tY", sfspd, sfspd, sfspd, sfspd));
		
		Button next_date = (Button)this.findViewById(R.id.next_date);
		next_date.setEnabled(sigfood.naechstertag != null);
		Button prev_date = (Button)this.findViewById(R.id.prev_date);
		prev_date.setEnabled(sigfood.vorherigertag != null);

		for (final MensaEssen e : sigfood.essen) {
			LinearLayout essen = (LinearLayout)LayoutInflater.from(getBaseContext()).inflate(R.layout.mensaessen, null);
			TextView t2 = (TextView)essen.findViewById(R.id.hauptgerichtBezeichnung);
			t2.setText(Html.fromHtml(e.hauptgericht.bezeichnung) + "(" + e.hauptgericht.bewertung.schnitt + "/" + e.hauptgericht.bewertung.anzahl + "/" + e.hauptgericht.bewertung.stddev + ")");
			TextView ueberschrift = (TextView)essen.findViewById(R.id.ueberschrift);
			ueberschrift.setText("Linie: " + e.linie);
			final Button ratingbutton = (Button)essen.findViewById(R.id.button1);


			TextView kommentare1 = (TextView)essen.findViewById(R.id.kommentare1);
			String tmp = "";
			for (String s : e.hauptgericht.kommentare) {
				tmp += "\"" + Html.fromHtml(s) + "\"" + "\n";
			}
			kommentare1.setText(tmp);

			final RatingBar bar1 = (RatingBar)essen.findViewById(R.id.ratingBar1);
			bar1.setMax(5);
			bar1.setProgress((int) (e.hauptgericht.bewertung.schnitt + 0.5f));

			ratingbutton.setOnClickListener(new Button.OnClickListener() {  
				public void onClick(View v)
				{
					if(bar1.isIndicator()) {
						ratingbutton.setText("Bewertung jetzt abgeben");
						bar1.setIsIndicator(false);
					}
					else {
						bar1.setIsIndicator(true);
						ratingbutton.setEnabled(false);
						if (bewerten(e.hauptgericht, (int)bar1.getRating(), sfspd))
							ratingbutton.setText("Bewertung abgegeben");
					}
				}
			});

			final Button kommentieren = (Button)essen.findViewById(R.id.kommentieren);
			final LinearLayout kommentar = (LinearLayout)essen.findViewById(R.id.kommentar);
			final EditText kommentar_name = (EditText)essen.findViewById(R.id.kommentar_name);
			final EditText kommentar_text = (EditText)essen.findViewById(R.id.kommentar_text);
			
			kommentieren.setOnClickListener(new Button.OnClickListener() {  
				public void onClick(View v)
				{
					if(kommentar.getVisibility() == View.GONE) {
						kommentar.setVisibility(View.VISIBLE);
						kommentieren.setText("Kommentar abschicken");
					}
					else {
						kommentar.setVisibility(View.GONE);
						String name = kommentar_name.getText().toString();
						String text = kommentar_text.getText().toString();
						if(name.length() > 0 && text.length() > 0) {
							if(kommentieren(e.hauptgericht, sfspd, name, text)) {
								kommentieren.setText("Kommentar abgegeben");
								kommentieren.setEnabled(false);
								return;
							}
						}
						kommentieren.setText("Kommentieren fehlgeschlagen");
					}
				}
			});
			
			
			
			ImageButton btn = (ImageButton)essen.findViewById(R.id.imageButton1);

			for (final Hauptgericht beilage : e.beilagen) {
				TextView beilage_bezeichnung = new TextView(getBaseContext(), null, android.R.attr.textAppearanceMedium); 
				beilage_bezeichnung.setText(Html.fromHtml(beilage.bezeichnung) + "(" + beilage.bewertung.schnitt + "/" + beilage.bewertung.anzahl + "/" + e.hauptgericht.bewertung.stddev + ")");
				essen.addView(beilage_bezeichnung);


				final RatingBar bar2 = new RatingBar(this, null,android.R.attr.ratingBarStyle);
				bar2.setIsIndicator(true);
				bar2.setNumStars(5);
				bar2.setMax(5);
				bar2.setStepSize(1);
				bar2.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
				bar2.setRating((int) (beilage.bewertung.schnitt + 0.5f));
				essen.addView(bar2);

				final Button ratingbutton2 = new Button(this, null,android.R.attr.buttonStyleSmall);
				ratingbutton2.setText("Beilage bewerten");
				ratingbutton2.setOnClickListener(new Button.OnClickListener() {  
					public void onClick(View v)
					{
						if(bar2.isIndicator()) {
							ratingbutton2.setText("Bewertung jetzt abgeben");
							bar2.setIsIndicator(false);
						}
						else {
							bar2.setIsIndicator(true);
							ratingbutton2.setEnabled(false);
							if (bewerten(beilage, (int)bar2.getRating(), sfspd))
								ratingbutton.setText("Bewertung abgegeben");
						}
					}
				});
				essen.addView(ratingbutton2);

				TextView kommentare2 = new TextView(getBaseContext(), null, android.R.attr.textAppearanceSmall); 
				String tmp2 = "";
				for(String s : beilage.kommentare) {
					tmp2 += "\"" + Html.fromHtml(s) + "\"" + "\n";
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

			if (e.hauptgericht.bilder.size() > 0) {
				Random rng = new Random();
				int bild_id = e.hauptgericht.bilder.get(rng.nextInt(e.hauptgericht.bilder.size()));
				URL myFileUrl =null;
				try {
					myFileUrl= new URL("http://www.sigfood.de/?do=getimage&bildid=" + bild_id + "&width=320");
				} catch (MalformedURLException e1) {
					Bitmap bmImg = BitmapFactory.decodeResource(getResources(), R.drawable.picdownloadfailed);
					btn.setImageBitmap(bmImg);
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
					Bitmap bmImg = BitmapFactory.decodeResource(getResources(), R.drawable.picdownloadfailed);
					btn.setImageBitmap(bmImg);
				}
			} else {
				Bitmap bmImg = BitmapFactory.decodeResource(getResources(), R.drawable.nophotoavailable003);
				btn.setImageBitmap(bmImg);
			}


			parent.addView(essen);
		}
	}

	boolean kommentieren(Hauptgericht e, Date tag, String name, String kommentar) {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://www.sigfood.de/");

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
			nameValuePairs.add(new BasicNameValuePair("do", "2"));
			nameValuePairs.add(new BasicNameValuePair("datum",
					                                  String.format("%tY-%tm-%td", tag, tag, tag)));
			nameValuePairs.add(new BasicNameValuePair("gerid", Integer.toString(e.id)));

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse response = httpclient.execute(httppost);
			if (response.getStatusLine() == null) {
				return false;
			} else {
				if (response.getStatusLine().getStatusCode() != 200) {
					return false;
				}
			}

		} catch (ClientProtocolException e1) {
			return false;
		} catch (IOException e1) {
			return false;
		}

		return true;
	}
	
	boolean bewerten(Hauptgericht e, int stars, Date tag) {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://www.sigfood.de/");

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
			nameValuePairs.add(new BasicNameValuePair("do", "1"));
			nameValuePairs.add(new BasicNameValuePair("datum",
					                                  String.format("%tY-%tm-%td", tag, tag, tag)));
			nameValuePairs.add(new BasicNameValuePair("gerid", Integer.toString(e.id)));

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse response = httpclient.execute(httppost);
			if (response.getStatusLine() == null) {
				return false;
			} else {
				if (response.getStatusLine().getStatusCode() != 200) {
					return false;
				}
			}

		} catch (ClientProtocolException e1) {
			return false;
		} catch (IOException e1) {
			return false;
		}

		return true;
	}

	private Uri imageUri;
	final int TAKE_PICTURE = 19238;
	final int PICK_FROM_FILE = 19239;

	ImageButton phototarget;

	public void takePhoto(View v) {
		ImageButton btn = (ImageButton)v;
		phototarget = btn;

		final String[] items = new String[] { "Select from file", "Take picture"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String> (this, android.R.layout.select_dialog_item, items);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select Image");
		builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
			public void onClick( DialogInterface dialog, int item ) {
				if (item == 0) {
					Intent intent = new Intent();

	                intent.setType("image/*");
	                intent.setAction(Intent.ACTION_GET_CONTENT);

	                startActivityForResult(Intent.createChooser(intent, "Complete action using"), PICK_FROM_FILE);
				} else {
					Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
					File photo = new File(Environment.getExternalStorageDirectory(), "sigfood.jpg");
					intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(photo));
					imageUri = Uri.fromFile(photo);
					startActivityForResult(intent, TAKE_PICTURE);

					dialog.cancel();
				}
			}
		} );

		final AlertDialog dialog = builder.create();
		dialog.show();
	}

	public class UploadPhotoTaskParams {
		public UploadPhotoTaskParams(MensaEssen e, Date d, String filepath) {
			this.e=e;
			this.d=d;
			this.filepath=filepath;
		}
		public MensaEssen e;
		public Date d;
		public String filepath;
	}
	public class UploadPhotoTask extends AsyncTask<UploadPhotoTaskParams, Integer, Boolean> implements HttpMultipartClient.ProgressListener{

		@Override
		protected Boolean doInBackground(UploadPhotoTaskParams... arg0) {
			HttpMultipartClient httpMultipartClient = new HttpMultipartClient("www.sigfood.de", "/", 80);
			FileInputStream fis;
			try {
				fis = new FileInputStream(new File(arg0[0].filepath));
				httpMultipartClient.addFile("sigfood.jpg", fis, fis.available());
				httpMultipartClient.addField("do", "4");
				httpMultipartClient.addField("beilagenid", "-1");
				httpMultipartClient.addField("datum", String.format("%tY-%tm-%td", arg0[0].d, arg0[0].d, arg0[0].d));
				httpMultipartClient.addField("gerid", Integer.toString(arg0[0].e.hauptgericht.id));
				httpMultipartClient.setRequestMethod("POST");
				httpMultipartClient.send(this);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return false;
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
				return false;
			}
			return true;
		}
		@Override
	     protected void onProgressUpdate(Integer... progress) {
	         pd.setProgress(progress[0]);
	         pd.setMessage(progress[0].toString() + " Bytes übertragen.");
	     }

		@Override
	     protected void onPostExecute(Boolean result) {
			pd.dismiss();
			if(result)
				Toast.makeText(SigfoodActivity.this, "Upload done" ,Toast.LENGTH_LONG).show();
			else
				Toast.makeText(SigfoodActivity.this, "Failed to load or upload", Toast.LENGTH_SHORT).show();
	     }

		public void transferred(int bytes) {
			this.publishProgress(bytes);
		}

	}
	
	private ProgressDialog pd;
	void uploadPic(MensaEssen e, Date d, String filepath) {
		pd = ProgressDialog.show(this, "Das Foto wird gerade hochgeladen...", "Bitte warten...", false, false);
		pd.setMax(100);
		UploadPhotoTask upload = new UploadPhotoTask();
		upload.execute(new UploadPhotoTaskParams(e,d,filepath));
	}
	
	void uploadPic2(MensaEssen e, Date d, String filepath) {
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://www.sigfood.de/");

		try {

			MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);  
			multipartEntity.addPart("do", new StringBody("4"));
			multipartEntity.addPart("beilagenid", new StringBody("-1"));
			multipartEntity.addPart("datum", new StringBody(String.format("%tY-%tm-%td", d, d, d)));
			multipartEntity.addPart("gerid", new StringBody(Integer.toString(e.hauptgericht.id)));

			File f = new File(filepath);
			multipartEntity.addPart("newimg", new FileBody(f));
			httppost.setEntity(multipartEntity);


			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			if (response.getStatusLine() == null) {
				throw new RuntimeException("nostatusline");
			} else {
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new RuntimeException("badstatuscode");
				}
			}

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
		if (resultCode == Activity.RESULT_OK) {
			Uri selectedImage = null;
			switch (requestCode) {
			case TAKE_PICTURE:
				selectedImage = imageUri;
				break;
			case PICK_FROM_FILE:
				selectedImage = data.getData();
				break;
			}
			if (selectedImage != null) {
				getContentResolver().notifyChange(selectedImage, null);
				ImageView imageView = phototarget;
				String path = "";
				try {
					path = getRealPathFromURI(selectedImage); /* Try to resolve content:// crap URLs */
					if (path == null) { /* Oups, that failed, so... */
						path = selectedImage.getPath(); /* just take the path part of URL */
					}
					Bitmap bitmap = BitmapFactory.decodeFile(path);
					
					int w = bitmap.getWidth();
					int h = bitmap.getHeight();
					float aspect = (float)w/(float)h;
					if(w > 800 || h > 600) {
						File file_resized = new File(Environment.getExternalStorageDirectory(), "sigfood_resized.jpg");
						Bitmap bitmap_resized = Bitmap.createScaledBitmap(bitmap, 800, (int)(600.0f * aspect), false);
					    FileOutputStream out = new FileOutputStream(file_resized);
					    bitmap_resized.compress(Bitmap.CompressFormat.JPEG, 85, out);
					    long oldsize = new File(path).length();
					    Log.i("de.sigfood", "resize: " + w + ", " + h + ", " + oldsize + "->" + file_resized.length());
					    if(file_resized.length() < oldsize)
					    	path = file_resized.getPath();
					}
					
					imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 320, 200, false));
					uploadPic((MensaEssen)phototarget.getTag(),
							  ((MensaEssen)phototarget.getTag()).datumskopie,
							  path);
					//Toast.makeText(this, "Upload done" ,Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					//Toast.makeText(this, "Failed to load or upload" + path, Toast.LENGTH_SHORT).show();
					Log.e("Camera", e.toString());
				}
			}
		}
	}

	/* I seriously haven't got the slightest clue what this does, it's copied from
	 * some howto. The more interesting question is why I even need this crap,
	 * and why there are no more sensible functions in the API, like
	 * JUSTGIVEMETHEFUCKINGPATHOFTHATCONTENTCRAP()
	 */
	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        if (cursor == null) {
        	return null;
        }
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
	}
}
