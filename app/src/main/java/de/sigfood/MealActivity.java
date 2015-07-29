package de.sigfood;

// --------------------------------------------------
// SigfoodActivity
// Handles the fragments and picture taking/uploading
// --------------------------------------------------

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.buzzingandroid.tabswipe.TabSwipeActivity;

public class MealActivity extends TabSwipeActivity {
	public MensaEssen startMeal;
	public MensaEssen backMeal;
	public MealFragment mf;
	public CommentFragment cf;
	
	public SharedPreferences preferences;
	public int settings_price;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowTitleEnabled(false);  
        bar.setDisplayShowHomeEnabled(false);
        
        preferences = getSharedPreferences("de.sigfood", 0);
        settings_price = Integer.parseInt(preferences.getString("price","0"));
        
        // Add each Fragment as Tab
        addTab(R.string.mealTab, MealFragment.class, MealFragment.createBundle(getString(R.string.mealTab)));
        addTab(R.string.commentsTab, CommentFragment.class, CommentFragment.createBundle(getString(R.string.commentsTab)));
        
        //mf=(MealFragment) getTab(0);
        //cf=(CommentFragment) getTab(1);
        
        Intent intent = getIntent();
        startMeal = (MensaEssen)intent.getSerializableExtra("de.sigfood.mealinfo");
    }

	public void setMF(MealFragment mealFragment) {
		mf = mealFragment;
		if (mf!=null && cf!=null) setMeal(startMeal);
	}
	public void setCF(CommentFragment commentFragment) {
		cf = commentFragment;
		if (mf!=null && cf!=null) setMeal(startMeal);
	}
    
    public void setMeal(MensaEssen e) {
		backMeal = null;
        mf.setMeal(e);
        cf.setComments(e);
    }    
    public void setMeal(MensaEssen e, MensaEssen b) {
		backMeal = b;
        mf.setMeal(e);
        cf.setComments(e);
    }

	@Override
	public void onBackPressed() {
		// Handle back presses. Here, we do this based on location and variables instead of using the stack
	    if (getSupportActionBar().getSelectedNavigationIndex()==1) {
	    	getSupportActionBar().setSelectedNavigationItem(0);
	    } else {
	    	if (backMeal!=null) {
	    		setMeal(backMeal);
	    		backMeal = null;
	    	} else super.onBackPressed();
	    }
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
	
	private ProgressDialog pd;
	void uploadPic(MensaEssen e, Date d, String filepath) {
		pd = ProgressDialog.show(this, getString(R.string.uploadMessage), getString(R.string.uploadPleaseWait), false, false);
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
			throw new RuntimeException(e1);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}

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
				e1.printStackTrace();
				return false;
			} catch (IOException e2) {
				e2.printStackTrace();
				return false;
			}
			return true;
		}
		@Override
	     protected void onProgressUpdate(Integer... progress) {
	         pd.setProgress(progress[0]);
	         pd.setMessage(progress[0].toString() + " " + getString(R.string.uploadBytesSent));
	     }

		@Override
	     protected void onPostExecute(Boolean result) {
			pd.dismiss();
			if(result)
				Toast.makeText(MealActivity.this, R.string.uploadSuccess ,Toast.LENGTH_LONG).show();
			else
				Toast.makeText(MealActivity.this, R.string.uploadFailed, Toast.LENGTH_SHORT).show();
	     }

		public void transferred(int bytes) {
			this.publishProgress(bytes);
		}

	}

	// -------------------------------------------------------------
	// Code for taking and uploading pictures (used in MealFragment)
	// -------------------------------------------------------------
	
	private Uri imageUri;
	final int TAKE_PICTURE = 19238;
	final int PICK_FROM_FILE = 19239;

	ImageButton phototarget;

	public void takePhoto(View v) {
		ImageButton btn = (ImageButton)v;
		phototarget = btn;

		final String[] items = new String[] { getString(R.string.uploadChooseFile), getString(R.string.uploadTakePicture)};
		ArrayAdapter<String> adapter = new ArrayAdapter<String> (this, android.R.layout.select_dialog_item, items);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.uploadTitle);
		builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
			public void onClick( DialogInterface dialog, int item ) {
				if (item == 0) {
					Intent intent = new Intent();

	                intent.setType("image/*");
	                intent.setAction(Intent.ACTION_GET_CONTENT);

	                startActivityForResult(Intent.createChooser(intent, getString(R.string.uploadChooseApp)), PICK_FROM_FILE);
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
				this.getContentResolver().notifyChange(selectedImage, null);
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
						Bitmap bitmap_resized = Bitmap.createScaledBitmap(bitmap, 800, (int)(800.0f / aspect), false);
					    FileOutputStream out = new FileOutputStream(file_resized);
					    bitmap_resized.compress(Bitmap.CompressFormat.JPEG, 85, out);
					    long oldsize = new File(path).length();
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
