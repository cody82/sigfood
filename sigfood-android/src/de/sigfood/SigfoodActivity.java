package de.sigfood;

// --------------------------------------------------
// SigfoodActivity
// Handles the fragments and picture taking/uploading
// --------------------------------------------------

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

public class SigfoodActivity extends SherlockActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	public Date current = null;
	
	public SharedPreferences preferences;
	public int settings_price;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu);
	
		LinearLayout tv = (LinearLayout)findViewById(R.id.mainList);
		tv.removeAllViews();
        
        //ActionBar bar = getSupportActionBar();
        //bar.setDisplayShowTitleEnabled(false);
        
		Button prev_date = (Button)findViewById(R.id.mainPrevDate);
		Button next_date = (Button)findViewById(R.id.mainNextDate);
			
		next_date.setOnClickListener(new Button.OnClickListener() {  
			public void onClick(View v2)
			{
				if (sigfood != null) {
					if (sigfood.naechstertag != null) {
						fillspeiseplan(sigfood.naechstertag);
					}
				}
			}
		});
		prev_date.setOnClickListener(new Button.OnClickListener() {  
			public void onClick(View v2)
			{
				if (sigfood != null) {
					if (sigfood.vorherigertag != null) {
						fillspeiseplan(sigfood.vorherigertag);
					}
				}
			}
		});
		
		if (savedInstanceState != null) current = (Date)savedInstanceState.getSerializable("de.sigfood.plandate");
		else current = null;
		
		preferences = getSharedPreferences("de.sigfood", 0);
		preferences.registerOnSharedPreferenceChangeListener((SharedPreferences.OnSharedPreferenceChangeListener) this);
		if (!preferences.contains("price")) {
			Editor e = preferences.edit();
			e.putString("price","0");
			e.commit();
		}
		onSharedPreferenceChanged(preferences, null); // set the settings variables and load plan
    }
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		settings_price = Integer.parseInt(sharedPreferences.getString("price","0"));
		fillspeiseplan(current); // refresh plan on change
    }
   
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putSerializable("de.sigfood.plandate", current);
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getSupportMenuInflater();
        mi.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.bar_main_settings:
    			Intent intent = new Intent(this, SigfoodSettings.class);
    			startActivity(intent);
    			break;
    		default:
    			break;
    	}
    	return true;
    } 
	
	SigfoodApi sigfood;
	
	public void fillspeiseplan(Date d) {
		current = d;
		
		/* First clear and show loading indicator */
		LinearLayout parent = (LinearLayout)findViewById(R.id.mainList);
		parent.removeAllViews();

		View scroller = (View)findViewById(R.id.mainScroller);
		scroller.setVisibility(View.GONE);
		View loader = (View)findViewById(R.id.mainLoading);
		loader.setVisibility(View.VISIBLE);

		/* Start the download via a seperate thread */
		SigfoodThread sft = new SigfoodThread(d,this);
		sft.start();
	}
	
	public void fillspeiseplanReturn(SigfoodApi sfa) {
		LinearLayout parent = (LinearLayout)findViewById(R.id.mainList);
		TextView datum = (TextView)findViewById(R.id.mainDate);

		View scroller = (View)findViewById(R.id.mainScroller);
		scroller.setVisibility(View.VISIBLE);
		View loader = (View)findViewById(R.id.mainLoading);
		loader.setVisibility(View.GONE);
		
		sigfood = sfa;

		/* Now start to fill plan and download pictures */
		final Date sfspd = sigfood.speiseplandatum;
		datum.setText(String.format("%tA, %td.%tm.%tY", sfspd, sfspd, sfspd, sfspd));
		
		Button next_date = (Button)findViewById(R.id.mainNextDate);
		next_date.setEnabled(sigfood.naechstertag != null);
		Button prev_date = (Button)findViewById(R.id.mainPrevDate);
		prev_date.setEnabled(sigfood.vorherigertag != null);
		
		int rowcounter = 0;
		LinearLayout current = null;
		
		int rows = 0;
		ProgressBar test = null;
		test = (ProgressBar)findViewById(R.id.menuRowCount2);
		if (test!=null) rows=2;
		test = null;
		test = (ProgressBar)findViewById(R.id.menuRowCount3);
		if (test!=null) rows=3;
		if (rows<=0) rows=1;

		for (final MensaEssen e : sigfood.essen) {
			if (current==null && rows>1) {
				if (rows==2) current = (LinearLayout)LayoutInflater.from(getBaseContext()).inflate(R.layout.mainrow2, null);
				if (rows==3) current = (LinearLayout)LayoutInflater.from(getBaseContext()).inflate(R.layout.mainrow3, null);
			}
			LinearLayout essen = (LinearLayout)LayoutInflater.from(getBaseContext()).inflate(R.layout.mainmeal, null);
			TextView name = (TextView)essen.findViewById(R.id.mainMealTitle);
			name.setText(Html.fromHtml(e.hauptgericht.bezeichnung));
			
			TextView info = (TextView)essen.findViewById(R.id.mainMealInfo);
			DecimalFormat currencyFormatter = new DecimalFormat("0.00€");
			String price;
			if (settings_price==1) price = currencyFormatter.format(e.hauptgericht.preis_bed);
			else if (settings_price==2) price = currencyFormatter.format(e.hauptgericht.preis_gast);
			else price = currencyFormatter.format(e.hauptgericht.preis_stud);
			info.setText("Linie " + e.linie + ((e.hauptgericht.preis_stud==0f || e.hauptgericht.preis_bed==0f || e.hauptgericht.preis_gast==0f) ? "" : "\n" + price));

			final RatingBar bar1 = (RatingBar)essen.findViewById(R.id.mainMenuRating);
			bar1.setMax(50);
			bar1.setProgress((int) (e.hauptgericht.bewertung.schnitt*10));

			ImageView img = (ImageView)essen.findViewById(R.id.mainMealPicture);
			ProgressBar load = (ProgressBar)essen.findViewById(R.id.mainMealPictureLoading);

			if (e.hauptgericht.bilder.size() > 0) {
				Random rng = new Random();
				int bild_id = e.hauptgericht.bilder.get(rng.nextInt(e.hauptgericht.bilder.size()));
				URL myFileUrl =null;
				try {
					myFileUrl= new URL("http://www.sigfood.de/?do=getimage&bildid=" + bild_id + "&width=320");
				} catch (MalformedURLException e1) {
					Bitmap bmImg = BitmapFactory.decodeResource(getResources(), R.drawable.picdownloadfailed);
					img.setImageBitmap(bmImg);
				}
				PictureThread pt = new PictureThread(myFileUrl,img,load,this);
				pt.start();
			} else {
				Bitmap bmImg = BitmapFactory.decodeResource(getResources(), R.drawable.nophotoavailable003);
				img.setImageBitmap(bmImg);
				load.setVisibility(View.GONE);
			}
			
			essen.setOnClickListener(new Button.OnClickListener() {  
				public void onClick(View v2)
				{
					startMeal(e);
				}
			});
			
			if (rows==1 || current==null) parent.addView(essen);
			else {
				if (rowcounter==0) ((LinearLayout)current.findViewById(R.id.menuField1)).addView(essen);
				else if (rowcounter==1) ((LinearLayout)current.findViewById(R.id.menuField2)).addView(essen);
				else if (rowcounter==2) ((LinearLayout)current.findViewById(R.id.menuField3)).addView(essen);
				rowcounter++;
				if (rowcounter>=rows) {
					rowcounter=0;
					parent.addView(current);
					current=null;
				}
			}
		}
		if (current!=null) parent.addView(current);
	}

	protected void startMeal(MensaEssen e) {
		Intent intent = new Intent(this, MealActivity.class);
		intent.putExtra("de.sigfood.mealinfo", e);
		startActivity(intent);
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
}
