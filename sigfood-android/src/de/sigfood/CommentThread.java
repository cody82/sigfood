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

public class CommentThread extends Thread {
	Hauptgericht e;
	Date t;
	String n;
	String k;
	Button b;
	Activity act;
	
	public CommentThread(Hauptgericht meal, Date tag, String name, String kommentar, Button button, Activity a) {
		e = meal;
		t = tag;
		n = name;
		k = kommentar;
		b = button;
		act = a;
	}
	
    public void run() {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://www.sigfood.de/");

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
			nameValuePairs.add(new BasicNameValuePair("do", "2"));
			nameValuePairs.add(new BasicNameValuePair("datum",
					                                  String.format("%tY-%tm-%td", t, t, t)));
			nameValuePairs.add(new BasicNameValuePair("gerid", Integer.toString(e.id)));
			nameValuePairs.add(new BasicNameValuePair("kommentar", k));
			nameValuePairs.add(new BasicNameValuePair("nick", n));

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse response = httpclient.execute(httppost);
			if (response.getStatusLine() == null) {
				act.runOnUiThread(new Runnable() {
		            public void run() {
		            	b.setText("Kommentar fehlgeschlagen");
		            	b.setEnabled(true);
		            }
				});
			} else {
				if (response.getStatusLine().getStatusCode() != 200) {
					act.runOnUiThread(new Runnable() {
			            public void run() {
			            	b.setText("Kommentar fehlgeschlagen");
			            	b.setEnabled(true);
			            }
					});
				}
			}

		} catch (ClientProtocolException e1) {
			act.runOnUiThread(new Runnable() {
	            public void run() {
	            	b.setText("Kommentar fehlgeschlagen");
	            	b.setEnabled(true);
	            }
			});
		} catch (IOException e1) {
			act.runOnUiThread(new Runnable() {
	            public void run() {
	            	b.setText("Kommentar fehlgeschlagen");
	            	b.setEnabled(true);
	            }
			});
		}
		
		act.runOnUiThread(new Runnable() {
            public void run() {
            	b.setText("Kommentar abgesendet");
            }
		});
    }
}