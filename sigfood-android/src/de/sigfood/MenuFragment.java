package de.sigfood;

// -------------------------------------
// MenuFragment
// Displays the menu and basic meal info
// Links to MealFragment
// -------------------------------------

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

public class MenuFragment extends Fragment {
	
	public SigfoodActivity act;
	public View v;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		if (v == null) {
			act = (SigfoodActivity) getActivity();
			v = inflater.inflate(R.layout.main, null);
	
			LinearLayout tv = (LinearLayout)v.findViewById(R.id.mainList);
			tv.removeAllViews();		
	
			Button prev_date = (Button)v.findViewById(R.id.mainPrevDate);
			Button next_date = (Button)v.findViewById(R.id.mainNextDate);
			
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
			
			fillspeiseplan(null);
		} else {
			((ViewGroup) v.getParent()).removeView(v);
		}
		return v;
	}
 
    public static Bundle createBundle(String title) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        return bundle;
    }
	
	SigfoodApi sigfood;
	
	public void fillspeiseplan(Date d) {
		/* First clear and show loading indicator */
		LinearLayout parent = (LinearLayout)v.findViewById(R.id.mainList);
		parent.removeAllViews();

		View scroller = (View)v.findViewById(R.id.mainScroller);
		scroller.setVisibility(View.GONE);
		View loader = (View)v.findViewById(R.id.mainLoading);
		loader.setVisibility(View.VISIBLE);

		/* Start the download via a seperate thread */
		SigfoodThread sft = new SigfoodThread(d,act,this);
		sft.start();
	}
	
	public void fillspeiseplanReturn(SigfoodApi sfa) {
		LinearLayout parent = (LinearLayout)v.findViewById(R.id.mainList);
		TextView datum = (TextView)v.findViewById(R.id.mainDate);

		View scroller = (View)v.findViewById(R.id.mainScroller);
		scroller.setVisibility(View.VISIBLE);
		View loader = (View)v.findViewById(R.id.mainLoading);
		loader.setVisibility(View.GONE);
		
		sigfood = sfa;

		/* Now start to fill plan and download pictures */
		final Date sfspd = sigfood.speiseplandatum;
		datum.setText(String.format("%tA, %td.%tm.%tY", sfspd, sfspd, sfspd, sfspd));
		
		Button next_date = (Button)v.findViewById(R.id.mainNextDate);
		next_date.setEnabled(sigfood.naechstertag != null);
		Button prev_date = (Button)v.findViewById(R.id.mainPrevDate);
		prev_date.setEnabled(sigfood.vorherigertag != null);

		for (final MensaEssen e : sigfood.essen) {
			LinearLayout essen = (LinearLayout)LayoutInflater.from(act.getBaseContext()).inflate(R.layout.mainmeal, null);
			TextView name = (TextView)essen.findViewById(R.id.mainMealTitle);
			name.setText(Html.fromHtml(e.hauptgericht.bezeichnung));
			TextView linie = (TextView)essen.findViewById(R.id.mainMealLine);
			linie.setText("Linie " + e.linie);

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
				PictureThread pt = new PictureThread(myFileUrl,img,load,act);
				pt.start();
			} else {
				Bitmap bmImg = BitmapFactory.decodeResource(getResources(), R.drawable.nophotoavailable003);
				img.setImageBitmap(bmImg);
				load.setVisibility(View.GONE);
			}
			
			essen.setOnClickListener(new Button.OnClickListener() {  
				public void onClick(View v2)
				{
					act.getSupportActionBar().setSelectedNavigationItem(1);
					MealFragment.setMeal(e);
				}
			});
			
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
}
