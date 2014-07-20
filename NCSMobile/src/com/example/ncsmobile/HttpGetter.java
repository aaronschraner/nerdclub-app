/*package com.example.ncsmobile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public class HttpGetter extends AsyncTask<URL, Void, String>
{

	@Override
	protected String doInBackground(URL... params)
	{
		// TODO Auto-generated method stub
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = null;
		try
		{
			httpGet = new HttpGet(params[0].toURI());
		} catch (URISyntaxException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try
		{
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if(statusCode==200)
			{
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while((line=reader.readLine())!=null)
				{
					builder.append(line);
				}
				Log.v("GET", "Success");
			}
			else
			{
				Log.e("GET","Failed. :p");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "Konnichiwa";
	}
	protected void onPreExecute()
	{
		Log.w("GET", "refreshing data...");
	}
	protected void onPostExecute(String result)
	{
		
	}
}
*/