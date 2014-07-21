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
				populateFields((String) tv.getText()); //assign force XML reparse to button
				
			}
		}); 
	}
	
	public void getXmlFromServer() //launch GET thread to retrieve XML 
	{
		Log.v("NCSMobile", "Deploying GET thread...");
		Toast.makeText(this, "Refreshing content...", Toast.LENGTH_SHORT).show();
		new MyTask().execute(sDataUrl);
	}
	
	public void pushString(String string) //Called when GET thread completes, handles errors and publishes result
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
	List<LogEntry> logEntries= new ArrayList<LogEntry>(); //list of log entries extracted from XML
	List<Player> players=new ArrayList<Player>(); //list of players extracted from XML
	public void populateFields(String xmlString) //MASSIVE function to extract that^ data from the XML
	{
		//init XML reader
		XmlPullParser parser = Xml.newPullParser();
		String ns=null; //namespace
		try //because things break
		{
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false); //set up parser
			parser.setInput(new StringReader(xmlString)); //set parser input
			Log.v("XML Parser", "Extracting XML data..."); 
			int xmlMode=MODE_CHAT; //initialize mode
			while(parser.getEventType()!=XmlPullParser.END_DOCUMENT) //while you're still reading the document
			{
				switch(parser.getEventType()) //easy way to parse XML while accounting for unknowns
				{						//yes I know sequential loops make more sense.
					case XmlPullParser.START_TAG:  //if it's an open tag
						String n=parser.getName(); //to prevent repeating myself
						if(n.equals("playerlist")) //if we've reached the player block
						{
							xmlMode=MODE_PLAYERS; //set element processor to players mode
						}
						if(n.equals("root")||n.equals("chatlog")||n.equals("playerlist"))
						{
							Log.v("XML Parser/tag","Skipping junktext"); //skip irritating text data between large elements
							parser.next();
						} 
						else
						{
							//process current element
							if(xmlMode==MODE_CHAT) //if element processor set to chat mode
							{
								String owner=parser.getAttributeValue(ns, "owner"); //load basic info
								String time=parser.getAttributeValue(ns, "time"); 
								parser.next();
								parser.require(XmlPullParser.TEXT, ns, null); //ensure correct format
								String content=parser.getText(); //get chat entry content
								logEntries.add(new LogEntry(owner, time, content)); //add to list
								//Log.v("XML Parser/gen", "Entry added to db. (chat)");
							}
							else if(xmlMode==MODE_PLAYERS) //if element processor set to player mode 
							{
								String name = parser.getAttributeValue(ns, "name"); //extract name
								URL smallHead = new URL(parser.getAttributeValue(ns, "smallhead")); //extract URL for small (in-chat) head
								URL bigHead = new URL(parser.getAttributeValue(ns, "bighead")); //extract URL for big (profile) head
								int color = Color.parseColor(parser.getAttributeValue(ns, "color"));  //extract player theme color (used in chat log + profile) 
								boolean onlineNow=false; 
								if(parser.getAttributeValue(ns, "onlinenow").equals("ONLINE")) //find if player is online
								{
									onlineNow=true; //and do something about it
								}
								else
								{
									onlineNow=false;
								}
								players.add(new Player(name, color, smallHead, bigHead, onlineNow)); //add player to list
								Log.v("XML Parser/gen", name +" added to db. (players)");
							}
						}
						break;
					case XmlPullParser.END_TAG:
						parser.next(); //skip extra text
						break;
					case XmlPullParser.TEXT:
						Log.v("XML Parser/text", "Text: " + parser.getText()); //never called 
						break;
					case XmlPullParser.END_DOCUMENT:
						Log.w("XML Parser/doc","END DOCUMENT"); //notify of end of document
						parser.next();
						break;
					case XmlPullParser.START_DOCUMENT:
						Log.v("XML Parser/doc", "START DOCUMENT"); //notify of start of document
						break;
					default: 
						Log.w("XML Parser/err", "Unhandled XML event type."); //in case there's something weird in there.
						break;
					
				}
				parser.next();
			}
			Log.v("XML Parser/gen", "Parsing complete"); //log parse completion
		}
		catch(IOException e)
		{
			Log.w("XML Parser","IOException occurred.");
			Toast.makeText(this, "Error occurred during XML parsing", Toast.LENGTH_SHORT).show();
		}
		catch(XmlPullParserException e)
		{
			Log.w("XML Parser","Parser exception (probably from a require()");
			Toast.makeText(this, "Error occurred during XML parsing", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} 
	}
	
	
	//Subclasses
	private class LogEntry //class to hold a single line of chat text
	{
		String owner; //person who said the line
		String time; //time the thing was said
		String content; //what was said
		
		public LogEntry(String owner, String time, String content) //constructor method
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
		public int getColor() //to streamline colorizing log later.
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
	
	private class Player //class to hold a player's information
	{
		String pName; //player name
		int color; //profile/chat color
		URL sHeadURL; //URL of small (chat) head
		URL bHeadURL; //URL of larger (profile) head
		boolean isOnline; //if the player is currently online
		//boolean isLogged; //for players who are in the whitelist who haven't played since
		//before the oldest log file still existing.
		
		public Player(String pName, int color, URL sHeadURL, URL bHeadURL, boolean isOnline) //constructor
		{
			this.pName=pName;
			this.color=color;
			this.sHeadURL=sHeadURL;
			this.bHeadURL=bHeadURL;
			this.isOnline=isOnline;
		}
		
		//accessor methods
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
	
	private class MyTask extends AsyncTask <String, Void, String> //asynchronous task to download XML from server
	{
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
			pushString(result); //do stuff with the result
		}
	}
	
}
