package de.sigfood;

// ----------------------------------------
// MealFragment
// Displays details on meals or side dishes
// Handles ratings
// Links to CommentFragment
// ----------------------------------------

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

public class MealFragment extends Fragment {
	
	public MealActivity act;
	public View v;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		act = (MealActivity) getActivity();
		if (v == null) {
			v = inflater.inflate(R.layout.meal, null);
		} else {
			((ViewGroup) v.getParent()).removeView(v);
		}
		act.setMF(this);
		return v;
	}
 
    public static Bundle createBundle(String title) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        return bundle;
    }
	
	SigfoodApi sigfood;
	
	public void setMeal(final MensaEssen e) {		
		LinearLayout parent;
		if (v.findViewById(R.id.mealList) instanceof LinearLayout) parent = (LinearLayout)v.findViewById(R.id.mealList);
		else parent = (LinearLayout)v.findViewById(R.id.meal);

		//View scroller = (View)v.findViewById(R.id.meal);
		parent.setVisibility(View.VISIBLE);
		View note = (View)v.findViewById(R.id.mealNote);
		note.setVisibility(View.GONE);
		
		TextView name = (TextView)parent.findViewById(R.id.mealTitle);
		name.setText(Html.fromHtml(e.hauptgericht.bezeichnung));
		TextView linie = (TextView)parent.findViewById(R.id.mealLine);
		if (e.linie.equalsIgnoreCase("0")) linie.setText("Beilage");
		else linie.setText("Linie " + e.linie);
		
		final ImageButton img = (ImageButton)parent.findViewById(R.id.mealPicture);
		Button btn = (Button)parent.findViewById(R.id.mealUpload);
		ProgressBar load = (ProgressBar)parent.findViewById(R.id.mealPictureLoading);
		img.setVisibility(View.GONE);
		btn.setVisibility(View.GONE);
		load.setVisibility(View.VISIBLE);

		if (e.hauptgericht.bilder.size() > 0) {
			Random rng = new Random();
			int bild_id = e.hauptgericht.bilder.get(rng.nextInt(e.hauptgericht.bilder.size()));
			URL myFileUrl =null;
			try {
				myFileUrl= new URL("http://www.sigfood.de/?do=getimage&bildid=" + bild_id + "&width=480");
			} catch (MalformedURLException e1) {
				Bitmap bmImg = BitmapFactory.decodeResource(act.getResources(), R.drawable.picdownloadfailed);
				img.setImageBitmap(bmImg);
			}
			PictureThread pt = new PictureThread(myFileUrl,img,load,act);
			pt.start();
		} else {
			img.setVisibility(View.GONE);
			if (!e.linie.equalsIgnoreCase("0")) btn.setVisibility(View.VISIBLE);
			load.setVisibility(View.GONE);
		}
		
        img.setTag(e);
        img.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v2)
                {
                        act.takePhoto(v2);
                }
        });		
        btn.setTag(e);
        btn.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v2)
                {
                        act.takePhoto(img);
                }
        });

		final RatingBar bar1 = (RatingBar)parent.findViewById(R.id.mealRating);
		final RatingBar barr = (RatingBar)parent.findViewById(R.id.mealRatingChoose);
		final Button ratingbutton = (Button)parent.findViewById(R.id.mealRatingButton);
		
		bar1.setMax(50);
		bar1.setProgress((int) (e.hauptgericht.bewertung.schnitt*10));
		((TextView) parent.findViewById(R.id.mealRatingText)).setText(e.hauptgericht.bewertung.schnitt+", "+e.hauptgericht.bewertung.anzahl+" Bewertungen ("+e.hauptgericht.bewertung.stddev+" Abw.)");
		
		final Date sfspd = e.datumskopie;
        Calendar today = Calendar.getInstance();
        int hour = today.get(Calendar.HOUR);
        int am = today.get(Calendar.AM_PM);
        today.set(Calendar.HOUR,0); today.set(Calendar.AM_PM,Calendar.AM);        today.set(Calendar.MINUTE,0);        today.set(Calendar.SECOND,0);        today.set(Calendar.MILLISECOND,0);
        Calendar twoago = (Calendar) today.clone();
        twoago.roll(Calendar.DATE, -2);
        Calendar start = Calendar.getInstance();
        start.set(sfspd.getYear()+1900, sfspd.getMonth(), sfspd.getDate(), 0, 0, 0);        start.set(Calendar.MILLISECOND,0);
        
        if (start.before(twoago)) {
                ratingbutton.setEnabled(false);
                ratingbutton.setText("Bewertung nicht mehr möglich");
        } else if (((hour>=11 || am==Calendar.PM) && start.equals(today)) || start.before(today)) {  
            	ratingbutton.setEnabled(true);
            	ratingbutton.setText("Bewerten");                      
                ratingbutton.setOnClickListener(new Button.OnClickListener() {
                        public void onClick(View v2)
                        {
                                if(barr.getVisibility()==View.GONE) {
                                        bar1.setVisibility(View.GONE);
                                        barr.setVisibility(View.VISIBLE);
                                        ratingbutton.setText("Bewertung abgeben");
                                } else {
                                		bar1.setVisibility(View.VISIBLE);
                                	    barr.setVisibility(View.GONE);
                                    	ratingbutton.setEnabled(false);
                                        Log.d("Rating",(int)barr.getRating()+"");
                                        if (bewerten(e.hauptgericht, (int)barr.getRating(), sfspd))
                                                ratingbutton.setText("Bewertet mit "+(int)barr.getRating()+" Stern"+(((int)barr.getRating()==1) ? "" : "en"));
                                }
                        }
                });
        } else {
                ratingbutton.setEnabled(false);
                ratingbutton.setText("Bewertung noch nicht möglich");
        }

		LinearLayout sidedishes = (LinearLayout)v.findViewById(R.id.mealSidedish);
		sidedishes.removeAllViews();
		
		if (e.beilagen.size()>0) {
			v.findViewById(R.id.mealSidedishLabel).setVisibility(View.VISIBLE);
			sidedishes.setVisibility(View.VISIBLE);
			for (final Hauptgericht beilage : e.beilagen) {
				LinearLayout sidedish = (LinearLayout)LayoutInflater.from(act.getBaseContext()).inflate(R.layout.mealsidedish, null);
				TextView titel = (TextView)sidedish.findViewById(R.id.sidedishTitle); 
				titel.setText(Html.fromHtml(beilage.bezeichnung));
	
				final RatingBar bar2 = (RatingBar)sidedish.findViewById(R.id.sidedishRating);
				bar2.setMax(50);
				bar2.setProgress((int) (beilage.bewertung.schnitt * 10));
				
				sidedish.setOnClickListener(new Button.OnClickListener() {  
					public void onClick(View v2)
					{
						MensaEssen bei = new MensaEssen();
						bei.linie = "0";
						bei.hauptgericht = beilage;
						bei.datumskopie = e.datumskopie;
						act.setMeal(bei,e);
					}
				});
				
				sidedishes.addView(sidedish);
			}
		} else {
			v.findViewById(R.id.mealSidedishLabel).setVisibility(View.GONE);
			sidedishes.setVisibility(View.GONE);
		}

		LinearLayout comments = (LinearLayout)parent.findViewById(R.id.mealComment);
		comments.removeAllViews();
		
		if (e.hauptgericht.kommentare.size()>0) {
			v.findViewById(R.id.mealCommentLabel).setVisibility(View.VISIBLE);
			comments.setVisibility(View.VISIBLE);

			LinearLayout comment = (LinearLayout)LayoutInflater.from(act.getBaseContext()).inflate(R.layout.comment, null);
			TextView text = (TextView)comment.findViewById(R.id.commentText); 
			text.setText(Html.fromHtml(e.hauptgericht.kommentare.get(0)));
			
			comments.addView(comment);
		} else {
			v.findViewById(R.id.mealCommentLabel).setVisibility(View.GONE);
			comments.setVisibility(View.GONE);
		}
		
		Button commentbtn = (Button)v.findViewById(R.id.mealCommentButton);
		commentbtn.setOnClickListener(new Button.OnClickListener() {  
			public void onClick(View v2)
			{
				act.getSupportActionBar().setSelectedNavigationItem(1);
			}
		});
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
