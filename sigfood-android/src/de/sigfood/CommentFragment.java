package de.sigfood;

// ----------------------------------------
// CommentFragment
// Displays comments on a meal or side dish
// Handles commenting
// ----------------------------------------

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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CommentFragment extends Fragment {
	
	public MealActivity act;
	public View v;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		act = (MealActivity) getActivity();
		if (v == null) {
			v = inflater.inflate(R.layout.comments, null);
		} else {
			((ViewGroup) v.getParent()).removeView(v);
		}
		act.setCF(this);
		return v;
	}
 
    public static Bundle createBundle(String title) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        return bundle;
    }
	
	SigfoodApi sigfood;
	
	public void setComments(final MensaEssen e) {
		if (v==null) return;
		
		View scroller = (View)v.findViewById(R.id.commentsView);
		scroller.setVisibility(View.VISIBLE);
		View note = (View)v.findViewById(R.id.commentsNote);
		note.setVisibility(View.GONE);

		TextView name = (TextView)v.findViewById(R.id.commentsTitle);
		name.setText(Html.fromHtml(e.hauptgericht.bezeichnung));
		((TextView) v.findViewById(R.id.commentsCount)).setText(e.hauptgericht.kommentare.size()+" Kommentar"+((e.hauptgericht.kommentare.size()==1) ? "" : "e"));
		
		final LinearLayout comments = (LinearLayout)v.findViewById(R.id.commentsList);
		comments.removeAllViews();

		for (Kommentar k : e.hauptgericht.kommentare) {
			LinearLayout comment = (LinearLayout)LayoutInflater.from(act.getBaseContext()).inflate(R.layout.comment, null);
			TextView text = (TextView)comment.findViewById(R.id.commentText); 
			text.setText(Html.fromHtml(k.text));
			TextView nick = (TextView)comment.findViewById(R.id.nickText);
			nick.setText(Html.fromHtml(k.nick));
			TextView date = (TextView)comment.findViewById(R.id.dateText); 
			date.setText(Html.fromHtml(k.datum));
			
			comments.addView(comment);
		}
		
		final Button commentbtn = (Button)v.findViewById(R.id.commentsButton);
		final LinearLayout commentform = (LinearLayout)v.findViewById(R.id.commentsForm);
		
		commentbtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v2)
            {
                    if(commentform.getVisibility() == View.GONE) {
                    		commentform.setVisibility(View.VISIBLE);
                    		comments.setVisibility(View.GONE);
                    		commentbtn.setText("Kommentar abschicken");
                    }
                    else {
                            String name = ((EditText)commentform.findViewById(R.id.commentsFormName)).getText().toString();
                            String text = ((EditText)commentform.findViewById(R.id.commentsFormText)).getText().toString();
                            if(name.length() > 0 && text.length() > 0) {
                            		commentbtn.setEnabled(false);
                        			CommentThread rt = new CommentThread(e.hauptgericht,e.datumskopie,name,text,commentbtn,act);
                        			rt.start();
                            }
                    }
            }
		});
	}
}
