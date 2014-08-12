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
import android.util.Log;

public class XmlDownloaderTask extends AsyncTask <String, Void, String> //asynchronous task to download XML from server
{
	public MainActivity hostActivity = null;
	public NotifierService hostService=null;
	public XmlDownloaderTask(MainActivity a)
	{
		this.hostActivity=a;
	}
	public XmlDownloaderTask(NotifierService s)
	{
		this.hostService=s;
	}
	
	@Override
	protected String doInBackground(String... params) 
	{
		if(params==null||params.length == 0)
		{
			return null; //cancel if nothing useful passed
		}
		Log.v("GET", "GET thread launched");
		String url=params[0]; //URL to get XML from 
		HttpClient client = new DefaultHttpClient(); //init download client
		String strResponse=null; //response container
		HttpGet getStuff = new HttpGet(url); //the get request to use
		boolean ok=true; //changed to false if errors occur
		try
		{
			HttpResponse getResponse = client.execute(getStuff); //download content
			strResponse= EntityUtils.toString(getResponse.getEntity()); //convert content to XML String
		} catch (ClientProtocolException e)
		{
			e.printStackTrace();
			ok=false;
		} catch (IOException e)
		{
			e.printStackTrace();
			ok=false;
		}finally{
			//Here because it doesn't work without it.
		}
		if(ok) //if nothing bad happened
		{
			Log.v("GET", "GET completed successfully");
		}
		else
		{
			Log.e("GET", "GET FAILED");
		}
		return strResponse;
	}
	
	protected void onPostExecute(String result)
	{
		if(hostActivity !=null)
			hostActivity.pushString(result); //do stuff with the result
		else if(hostService!=null)
			hostService.pushString(result);
		
	}
}
