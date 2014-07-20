package com.example.ncsmobile;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
	TextView tv;
	String sDataUrl="http://71.193.212.135/app/status.xml";
	String xmlText=null;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv=(TextView)findViewById(R.id.textView1);
		((Button)findViewById(R.id.button1)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			
			public void onClick(View v)
			{
				getXmlFromServer();
			}
		});
	}
	public void getXmlFromServer() 
	{
		Log.v("NCSMobile", "Deploying GET thread...");
		new MyTask().execute(sDataUrl);
	}
	public void pushString(String string)
	{
		xmlText=string;
		Toast.makeText(this, "Content loaded", Toast.LENGTH_SHORT).show();
		tv.setText(xmlText); //TODO: DEBUG UGLINESS
	}
	public void popLog(String xmlString)
	{
		//TODO: Create classes for log entries and players
		//TODO: and create and populate ArrayLists of them.
		//TODO: Use those to populate the UI stuff.
		//TODO: also find a way to implement colors.
	}
	private class MyTask extends AsyncTask <String, Void, String>
	{
		@Override
		protected String doInBackground(String... params)
		{
			if(params==null||params.length == 0)
			{
				return null;
			}
			Log.v("NCSMobile", "GET thread launched");
			String url=params[0];
			HttpClient client = new DefaultHttpClient();
			String strResponse=null;
			HttpGet getStuff = new HttpGet(url);
			try
			{
				HttpResponse getResponse = client.execute(getStuff);
				strResponse= EntityUtils.toString(getResponse.getEntity());
				//JSONObject responseObj=new JSONObject(strResponse) //delete this asap
			} catch (ClientProtocolException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			return strResponse;
		}
		
		protected void onPostExecute(String result)
		{
			pushString(result);
		}
	}
	
}
