package de.sigfood;

import java.io.IOException;
import java.util.ArrayList;
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

import android.app.Activity;
import android.widget.Button;

public class RatingThread extends Thread {
	Hauptgericht e;
	int s;
	Date t;
	Button b;
	Activity act;
	
	public RatingThread(Hauptgericht meal, int stars, Date tag, Button button, Activity a) {
		e = meal;
		s = stars;
		t = tag;
		b = button;
		act = a;
	}
	
    public void run() {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://www.sigfood.de/");

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
			nameValuePairs.add(new BasicNameValuePair("do", "1"));
			nameValuePairs.add(new BasicNameValuePair("datum",
					                                  String.format("%tY-%tm-%td", t, t, t)));
			nameValuePairs.add(new BasicNameValuePair("gerid", Integer.toString(e.id)));

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse response = httpclient.execute(httppost);
			if (response.getStatusLine() == null) {
				act.runOnUiThread(new Runnable() {
		            public void run() {
		            	b.setText("Bewertung fehlgeschlagen");
		            }
				});
			} else {
				if (response.getStatusLine().getStatusCode() != 200) {
					act.runOnUiThread(new Runnable() {
			            public void run() {
			            	b.setText("Bewertung fehlgeschlagen");
			            }
					});
				}
			}

		} catch (ClientProtocolException e1) {
			act.runOnUiThread(new Runnable() {
	            public void run() {
	            	b.setText("Bewertung fehlgeschlagen");
	            }
			});
		} catch (IOException e1) {
			act.runOnUiThread(new Runnable() {
	            public void run() {
	            	b.setText("Bewertung fehlgeschlagen");
	            }
			});
		}

		act.runOnUiThread(new Runnable() {
            public void run() {
            	b.setText("Bewertet mit "+s+" Stern"+((s==1) ? "" : "en"));
            }
		});
    }
}