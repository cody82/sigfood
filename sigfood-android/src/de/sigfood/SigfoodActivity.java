package de.sigfood;

// TODO: Replace all Date objects by Calendar? Lots of conversions between the two

// --------------------------------------------------
// SigfoodActivity
// Handles the fragments and picture taking/uploading
// --------------------------------------------------

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

public class SigfoodActivity extends SherlockActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	public Date current = null;
	
	public SharedPreferences preferences;
	public int settings_price;
	public int settings_size;
	public int settings_cache;
	
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
		Button retry = (Button)findViewById(R.id.mainNoConnectionRetryButton);
		
		// TODO: Allow scrolling by swiping
		next_date.setOnClickListener(new Button.OnClickListener() {  
			public void onClick(View v2)
			{
				if (sigfood != null) {
					if (sigfood.naechstertag != null) {
						fillspeiseplan(sigfood.naechstertag,false);
					}
				}
			}
		});
		prev_date.setOnClickListener(new Button.OnClickListener() {  
			public void onClick(View v2)
			{
				if (sigfood != null) {
					if (sigfood.vorherigertag != null) {
						fillspeiseplan(sigfood.vorherigertag,false);
					}
				}
			}
		});
		retry.setOnClickListener(new Button.OnClickListener() {  
			public void onClick(View v2)
			{
				fillspeiseplan(current,false);
			}
		});
		
		if (savedInstanceState != null) {
			current = (Date)savedInstanceState.getSerializable("de.sigfood.plandate");
		} else current = null;
		
		preferences = getSharedPreferences("de.sigfood", 0);
		preferences.registerOnSharedPreferenceChangeListener((SharedPreferences.OnSharedPreferenceChangeListener) this);
		if (!preferences.contains("price")) {
			Editor e = preferences.edit();
			e.putString("menuPriceHighlight","0");
			e.putString("menuPictureSize", "2");
			e.putString("cacheLifeTime", "6");
			e.commit();
		}
		onSharedPreferenceChanged(preferences, null); // set the settings variables and load plan
    }
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		settings_price = Integer.parseInt(sharedPreferences.getString("menuPriceHighlight","0"));
		settings_size = Integer.parseInt(sharedPreferences.getString("menuPictureSize","2"));
		settings_cache = Integer.parseInt(sharedPreferences.getString("cacheLifeTime","6"));
		fillspeiseplan(current,false); // refresh plan on change
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
    		case R.id.bar_main_refresh:
    			fillspeiseplan(current,true);
    			break;
    		default:
    			break;
    	}
    	return true;
    } 
	
	SigfoodApi sigfood;
	SigfoodThread sfthread;
	
	public void fillspeiseplan(Date d, boolean ignorecache) {		
		if (d==null) d = new Date();
		current = d;
		final Date sfspd = d;
		TextView datum = (TextView)findViewById(R.id.mainDate);
		datum.setText(String.format("%tA, %td.%tm.%tY", sfspd, sfspd, sfspd, sfspd));

		if (current!=null && sigfood!=null) {
			Calendar cal = Calendar.getInstance();
	        cal.add(Calendar.DATE, 7);
			Date week = cal.getTime();
	        cal.setTime(d);
	        do {
	        	cal.add(Calendar.DATE, -1);
	        } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);
	        Log.d("Cal",cal.get(Calendar.DAY_OF_WEEK)+" "+Calendar.SATURDAY+" "+Calendar.SUNDAY);
			sigfood.vorherigertag = cal.getTime();
			findViewById(R.id.mainPrevDate).setEnabled(true);
	        cal.add(Calendar.DATE, 2);
	        while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
	        	cal.add(Calendar.DATE, 1);
	        }
			sigfood.naechstertag = cal.getTime();
			findViewById(R.id.mainNextDate).setEnabled(!sigfood.naechstertag.after(week));
		}
		
		/* First clear and show loading indicator */
		LinearLayout parent = (LinearLayout)findViewById(R.id.mainList);
		parent.removeAllViews();

		View v = (View)findViewById(R.id.mainScroller);
		v.setVisibility(View.GONE);
		v = (View)findViewById(R.id.mainLoading);
		v.setVisibility(View.VISIBLE);
		v = (View)findViewById(R.id.mainNoMeals);
		v.setVisibility(View.GONE);
		v = (View)findViewById(R.id.mainNoConnection);
		v.setVisibility(View.GONE);

		/* Start the download via a seperate thread */
		if (sfthread!=null) sfthread.stop = true;
		sfthread = new SigfoodThread(d,this,settings_cache,ignorecache);
		sfthread.start();
	}
	
	public void fillspeiseplanReturn(SigfoodApi sfa) {
		// TODO: Analyze this method. Few warnings about main thread doing too much work, no idea why
		sfthread = null;
		
		LinearLayout parent = (LinearLayout)findViewById(R.id.mainList);
		parent.removeAllViews();

		View scroller = (View)findViewById(R.id.mainScroller);
		scroller.setVisibility(View.VISIBLE);
		View loader = (View)findViewById(R.id.mainLoading);
		loader.setVisibility(View.GONE);
		
		sigfood = sfa;
		
		Button next_date = (Button)findViewById(R.id.mainNextDate);
		next_date.setEnabled(sigfood.naechstertag != null);
		Button prev_date = (Button)findViewById(R.id.mainPrevDate);
		prev_date.setEnabled(sigfood.vorherigertag != null);
		
		int rowcounter = 0;
		LinearLayout currow = null;
		
		int rows = 0;
		ProgressBar test = null;
		test = (ProgressBar)findViewById(R.id.menuRowCount2);
		if (test!=null) rows=2;
		test = null;
		test = (ProgressBar)findViewById(R.id.menuRowCount3);
		if (test!=null) rows=3;
		if (rows<=0) rows=1;
		
		Display display = getWindowManager().getDefaultDisplay();
		Resources resources = getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    int border = 100 * (int)(metrics.densityDpi / 160f);
		int picWidth;
		if (settings_size==1) picWidth = (display.getWidth() / rows - border) / 2;
		else picWidth = display.getWidth() / rows - border;
		int loadHeight = (int)((float)picWidth/(float)16)*9;

		if (sigfood.essen.size()>0) {
			for (final MensaEssen e : sigfood.essen) {
				if (currow==null && rows>1) {
					if (rows==2) currow = (LinearLayout)LayoutInflater.from(getBaseContext()).inflate(R.layout.mainrow2, null);
					if (rows==3) currow = (LinearLayout)LayoutInflater.from(getBaseContext()).inflate(R.layout.mainrow3, null);
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
				info.setText(getString(R.string.line) + " " + e.linie + ((e.hauptgericht.preis_stud==0f || e.hauptgericht.preis_bed==0f || e.hauptgericht.preis_gast==0f) ? "" : "\n" + price));
	
				final RatingBar bar1 = (RatingBar)essen.findViewById(R.id.mainMenuRating);
				bar1.setMax(50);
				bar1.setProgress((int) (e.hauptgericht.bewertung.schnitt*10));
	
				ImageView img = (ImageView)essen.findViewById(R.id.mainMealPicture);
				LinearLayout load = (LinearLayout)essen.findViewById(R.id.mainMealPictureLoading);
				LayoutParams params = (LayoutParams) load.getLayoutParams();
				params.height = loadHeight;
				
				if (settings_size==0) {
					img.setVisibility(View.GONE);
					load.setVisibility(View.GONE);
				} else {
					int bild_id = -1;
					if (e.hauptgericht.bilder.size() > 0) {
						bild_id = e.hauptgericht.bilder.get(e.hauptgericht.bilder.size()-1);
					}
					PictureThread pt;
					if (settings_size==1) pt = new PictureThread(bild_id,picWidth,img,load,this,true);
					else pt = new PictureThread(bild_id,picWidth,img,load,this);
					pt.start();
				}
				
				essen.setOnClickListener(new Button.OnClickListener() {  
					public void onClick(View v2)
					{
						startMeal(e);
					}
				});
				
				if (rows==1 || currow==null) parent.addView(essen);
				else {
					if (rowcounter==0) ((LinearLayout)currow.findViewById(R.id.menuField1)).addView(essen);
					else if (rowcounter==1) ((LinearLayout)currow.findViewById(R.id.menuField2)).addView(essen);
					else if (rowcounter==2) ((LinearLayout)currow.findViewById(R.id.menuField3)).addView(essen);
					rowcounter++;
					if (rowcounter>=rows) {
						rowcounter=0;
						parent.addView(currow);
						currow=null;
					}
				}
			}
			if (currow!=null) {
				parent.addView(currow);
			}
		} else {
        	scroller.setVisibility(View.GONE);
        	View v = (View)findViewById(R.id.mainNoMeals);
    		v.setVisibility(View.VISIBLE);
		}
			
		//LinearLayout foot = (LinearLayout)LayoutInflater.from(getBaseContext()).inflate(R.layout.mainfragmentfooter, null);
		TextView updatetime = (TextView)findViewById(R.id.mainUpdateTime);
		updatetime.setText(String.format(getString(R.string.lastRefresh)+": %td.%tm.%tY, %tH:%tM", sfa.abrufdatum, sfa.abrufdatum, sfa.abrufdatum, sfa.abrufdatum, sfa.abrufdatum));
		//parent.addView(foot);
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
	
	@Override
	protected void onDestroy() {
		// delete images in cache when quitting app
	    super.onDestroy();
	    try {
	    	File cache = new File(getCacheDir().getPath());
	    	for (final File cacheFile : cache.listFiles()) {
	            if (cacheFile.isFile()) {
	            	String name = cacheFile.getName();
	            	if (name.substring(name.length()-4, name.length()).equals(".jpg")) {
	            		// is a picture - remove if older than 1 day
	            		long created = cache.lastModified();
		                if (created < (new Date()).getTime()-24*60*60*1000) cacheFile.delete();
	            	} else {
	            		// is a sigfood file - remove if older than 1 week
	            		long created = cache.lastModified();
		                if (created < (new Date()).getTime()-7*24*60*60*1000) cacheFile.delete();
	            	}
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
}
