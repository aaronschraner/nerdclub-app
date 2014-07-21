package com.example.ncsmobile;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
	TextView tv; //view to hold fetched XML (Debug)
	Button button2; //Force call parse XML (Debug)
	String sDataUrl="http://71.193.212.135/app/status.xml"; //URL to grab data from
	String xmlText=null; //variable to put downloaded XML data into.
	public static final int MODE_CHAT=45; //mode for when parser is scanning chat entries
	public static final int MODE_PLAYERS=46; //mode for when parser is scanning players
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv=(TextView)findViewById(R.id.textView1); //textview for holding XML (debug)
		button2=(Button)findViewById(R.id.button2); //button to parse DL'd XML (debug)
		((Button)findViewById(R.id.button1)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v)
			{
				getXmlFromServer(); //assign XML fetching function to button
			}
		});
		((Button)button2).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v)
			{
				populateFields((String) tv.getText());
				
			}
		}); // */
	}
	
	public void getXmlFromServer() 
	{
		Log.v("NCSMobile", "Deploying GET thread...");
		Toast.makeText(this, "Refreshing content...", Toast.LENGTH_SHORT).show();
		new MyTask().execute(sDataUrl);
	}
	
	public void pushString(String string)
	{
		xmlText=string;
		if(string==null)
		{
			Toast.makeText(this, "Failed to fetch content", Toast.LENGTH_SHORT).show();
			
		}
		else
		{
			Toast.makeText(this, "Content loaded", Toast.LENGTH_SHORT).show();
			populateFields(string);
		}
		tv.setText(xmlText); //TODO: DEBUG UGLINESS
	}
	List<LogEntry> logEntries= new ArrayList<LogEntry>();
	List<Player> players=new ArrayList<Player>();
	public void populateFields(String xmlString)
	{
		//init XML reader
		XmlPullParser parser = Xml.newPullParser();
		String ns=null;
		try 
		{
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(new StringReader(xmlString));
			Log.v("Xml Parser", "Extracting XML data...");
			int xmlMode=MODE_CHAT;
			while(parser.getEventType()!=XmlPullParser.END_DOCUMENT)
			{
				boolean skip;
				skip=true;
				switch(parser.getEventType())
				{
					case XmlPullParser.START_TAG: 
						//Log.v("XML Parser/tag","Start tag: " + parser.getName());
						String n=parser.getName();
						if(n.equals("playerlist"))
						{
							xmlMode=MODE_PLAYERS;
						}
						if(n.equals("root")||n.equals("chatlog")||n.equals("playerlist"))
						{
							Log.v("XML Parser/tag","Skipping junktext");
							parser.next();
						} 
						else
						{
							if(xmlMode==MODE_CHAT)
							{
								String owner=parser.getAttributeValue(ns, "owner");
								String time=parser.getAttributeValue(ns, "time");
								parser.next();
								parser.require(XmlPullParser.TEXT, ns, null);
								String content=parser.getText();
								logEntries.add(new LogEntry(owner, time, content));
								Log.v("XML Parser/gen", "Log Entry added to db. (chat)");
							}
							else if(xmlMode==MODE_PLAYERS)
							{
								String name = parser.getAttributeValue(ns, "name");
								URL smallHead = new URL(parser.getAttributeValue(ns, "smallhead"));
								URL bigHead = new URL(parser.getAttributeValue(ns, "bighead"));
								int color = Color.parseColor(parser.getAttributeValue(ns, "color"));
								boolean onlineNow=false;
								if(parser.getAttributeValue(ns, "onlinenow").equals("ONLINE"))
								{
									onlineNow=true;
								}
								else
								{
									onlineNow=false;
								}
								players.add(new Player(name, color, smallHead, bigHead, onlineNow));
								Log.v("XML Parser/gen", "Entry added to db. (players)");
							}
						}
						break;
					case XmlPullParser.END_TAG:
						//Log.v("XML Parser/tag", "End tag: " + parser.getName());
						parser.next();
						break;
					case XmlPullParser.TEXT:
						Log.v("XML Parser/text", "Text: " + parser.getText());
						break;
					case XmlPullParser.END_DOCUMENT:
						Log.w("XML Parser/doc","END DOCUMENT");
						parser.next();
						break;
					case XmlPullParser.START_DOCUMENT:
						Log.v("XML Parser/doc", "START DOCUMENT");
						break;
					default: 
						Log.w("XML Parser/err", "Unhandled XML event type.");
						break;
					
				}
				if(skip)
					parser.next();
			}
			//parser.require(XmlPullParser.END_TAG, ns, "");
			//parser.nextTag();
			Log.w("XML Parser/gen", "Parsing complete");
		}
		catch(IOException e)
		{
			Log.w("XML Parser","IOException occurred.");
		}
		catch(XmlPullParserException e)
		{
			Log.w("XML Parser","Parser exception (probably from a require()");
			e.printStackTrace();
		} 
		
		
		
	}
	
	
	//Subclasses
	private class LogEntry
	{
		String owner;
		String time;
		String content;
		public LogEntry(String owner, String time, String content)
		{
			this.owner=owner;
			this.time=time;
			this.content=content;
		}
		//accessor methods
		public String getOwner()
		{
			return owner;
		}
		public String getTime()
		{
			return time;
		}
		public String getContent()
		{
			return content;
		}
		public int getColor()
		{
			for (Player p : players)
			{
				if(p.getName().equals(owner))
				{
					return p.getColor();
				}
			}
			return 0;
		}

	}
	
	private class Player
	{
		String pName;
		int color;
		URL sHeadURL;
		URL bHeadURL;
		boolean isOnline;
		//boolean isLogged;
		public Player(String pName, int color, URL sHeadURL, URL bHeadURL, boolean isOnline)
		{
			this.pName=pName;
			this.color=color;
			this.sHeadURL=sHeadURL;
			this.bHeadURL=bHeadURL;
			this.isOnline=isOnline;
		}
		public String getName()
		{
			return pName;
		}
		public int getColor()
		{
			return color;
		}
		public URL getSmallHead()
		{
			return sHeadURL;
		}
		public URL getBigHead()
		{
			return bHeadURL;
		}
		public boolean isOnline()
		{
			return isOnline;
		}
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
			Log.v("GET", "GET thread launched");
			String url=params[0];
			HttpClient client = new DefaultHttpClient();
			String strResponse=null;
			HttpGet getStuff = new HttpGet(url);
			boolean ok=true;
			try
			{
				HttpResponse getResponse = client.execute(getStuff);
				strResponse= EntityUtils.toString(getResponse.getEntity());
			} catch (ClientProtocolException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				ok=false;
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				ok=false;
			}finally{
				//Here because it doesn't work without it.
			}
			if(ok)
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
			pushString(result);
		}
	}
	
}
