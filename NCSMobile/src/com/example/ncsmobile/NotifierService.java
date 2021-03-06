package com.example.ncsmobile;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

public class NotifierService extends Service
{
	private boolean mainActivityBound=false; //true if an activity is bound.
	private static NotifierService sInstance; //for external method access
	private NotificationCompat.Builder notificationBuilder; //for notifications
	private static final int ONLINE_NOTIFY=311; //for accessing the online notification
	public static final int DELAY_REFRESH = 1000; 
	public static final int MODE_CHAT=45; //mode for when parser is scanning chat entries
	public static final int MODE_PLAYERS=46; //mode for when parser is scanning players
	private static final int FLAG_UPDATE_NORMAL = 15;
	public static final int RINGTONE_LONG=520;
	public static final int RINGTONE_SHORT=521;
	boolean notifierPlaying=false;
	public List<Player> players = new ArrayList<Player>(); //list of players
	public List<LogEntry> logEntries = new ArrayList<LogEntry>(); //chat log
	String sDataUrl="http://71.193.212.135/app/status.xml"; //URL to grab data from
	static MediaPlayer mp;
	boolean dirty=true; //to help optimization
	Handler notificationHandler = new Handler(); //used for notifications
	Runnable notifierTask = new Runnable() //task to make notifications and autoupdate
	{
		@Override
		public void run()
		{
			autoUpdateFunction(); //do the auto-updating stuff
			if(autoUpdate)
				notificationHandler.postDelayed(notifierTask,DELAY_REFRESH); //then do it again after a delay 
		}
	};
	int cplaying=0; //holds resource of current sound playing. 0 means not playing.
	static boolean autoUpdate=true;
	//End variables and constants
	public void notifyOnline(Player player) //Launch a notification that someone has joined the server
	{
		Log.d("Notifier service", "Notifying that " + player.getName() + "is online."); //debug
		notificationBuilder.setContentTitle(player.getName() + " joined the server!");
		notificationBuilder.setContentText("Swipe away to silence."); //just kidding you can't.
		notificationBuilder.setLargeIcon(player.getSmallHead()); //set icon to the head of the player who joined
		Intent notifyIntent = new Intent(this,NotifierService.class); //intent to launch notifier
		PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, ONLINE_NOTIFY, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT); //more info for intent
		notificationBuilder.setContentIntent(notifyPendingIntent); 
		notificationBuilder.setDeleteIntent(PendingIntent.getActivity(this,ONLINE_NOTIFY,notifyIntent,PendingIntent.FLAG_CANCEL_CURRENT));
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(ONLINE_NOTIFY,notificationBuilder.build());
	}
	public void notifyOnline() //just shows that the app is running
	{
		Log.d("Notifier service", "Notifying that app is launched");
		notificationBuilder.setContentTitle("NCS Mobile running in background.");
		notificationBuilder.setContentText("Players will appear here as they join.");
		notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
		Intent notifyIntent = new Intent(this,NotifierService.class);
		PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notificationBuilder.setContentIntent(notifyPendingIntent);
		notificationBuilder.setDeleteIntent(PendingIntent.getActivity(this,0,notifyIntent,PendingIntent.FLAG_CANCEL_CURRENT));
		
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(ONLINE_NOTIFY,notificationBuilder.build());
	}
	private final IBinder nBinder = new NotifierBinder();
	@Override
	public IBinder onBind(Intent intent)
	{
		Log.v("Notifier service", "OnBind called within notifierService.");
		//MainActivity.getInstance().setUIRefreshing(autoUpdate);
		//launchAutoUpdater();
		mainActivityBound=true;
		return nBinder;
	}
	public class NotifierBinder extends Binder
	{
		NotifierService getService()
		{
			return NotifierService.this;
		}
	}
	@Override
	public void onCreate()
	{
		sInstance=this; //set static notifier instance
		notificationBuilder = new NotificationCompat.Builder(this);
		Log.v("Notifier service", "NotifierService onCreate called");
		notifyOnline();
		autoUpdate=true;
		
	}

	public static NotifierService getInstance() //accessor method for static notifier
	{
		return sInstance;
	}
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i("Notifier service", "Recieved start ID " + startId + "; " + intent.toString());
		return START_STICKY;
	}
	@Override
	public void onDestroy()
	{
		Intent killIntent = new Intent(this,NotifierService.class);
		PendingIntent killPendingIntent = PendingIntent.getActivity(this, 0, killIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		Log.v("Notifier service", "NotifierService killed");
		Toast.makeText(this, "Notifier service killed", Toast.LENGTH_SHORT).show();
		
	}
	@Override
	public boolean onUnbind(Intent intent)
	{
		mainActivityBound=false;
		return false;
	}
	public String xmlText;
	///////////////////////////////
	///////////////////////////////
	///METHODS FROM MAINACTIVITY///
	///////////////////////////////
	///////////////////////////////
	public void getXmlFromServer() //launch GET thread to retrieve XML 
	{
		Log.v("NCSMobile", "Deploying GET thread...");
		new XmlDownloaderTask(this).execute(sDataUrl);
	}
	public void pushString(String string) //Called when GET thread completes, handles errors and publishes result
	{
		if(string==null)
		{
			Toast.makeText(this, "Failed to fetch content", Toast.LENGTH_SHORT).show();
			
		}
		else if (!(string.equals(xmlText)))
		{
			Log.d("Notifier service", "Calling populateFields()");
			populateFields(string);
			if(mainActivityBound)
				sendToMainActivity(players,logEntries, FLAG_UPDATE_NORMAL);
			dirty=true;
		}
		else
		{
			Log.v("XML Parser", "No log updates.");
			dirty=false;
		}
		xmlText=string;
	}
	public void populateFields(String xmlString) //MASSIVE function to extract data from the XML
	{
		Log.v("Notifier service", "populating fields...");
		//init XML reader
		XmlPullParser parser = Xml.newPullParser();
		String ns=null; //namespace
		if(logEntries==null||players==null)
		{
			Log.w("Notifier service", "populateFields trying to clear null lists");
			
		}
		else
		{
			logEntries.clear();
			players.clear();
			Log.v("Notifier service", "populateFields cleared players normally");
		}
		try //because things break
		{
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false); //set up parser
			parser.setInput(new StringReader(xmlString)); //set parser input
			Log.v("XML Parser", "Extracting XML data..."); 
			int xmlMode=MODE_CHAT; //initialize mode
			while(parser.getEventType()!=XmlPullParser.END_DOCUMENT) //while you're still reading the document
			{
				switch(parser.getEventType()) //easy way to parse XML while accounting for unknowns
				{
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
	public void handleSounds(List<Player> players)
	{
		int playersOnline=0;
		for (Player p:players)
		{
			if(p.isOnline())
			{
				playersOnline++;
				if(dirty&&false) //disabled for optimization
				{
					notifyOnline(p);
				}
			}
		}
		if(playersOnline>0)
		{
			
			if(playersOnline<3)
			{
				if(notifierPlaying==false)
				{
					notifierPlaying=true;
				}
				playSound(RINGTONE_SHORT); //short irritating notification sound
			}
			else
			{
				if(notifierPlaying==false)
				{
					notifierPlaying=true;
				}
				playSound(RINGTONE_LONG); //much longer irritating notification sound
			}
		}
		else //kill sound if nobody is online
		{
			notifierPlaying=false;
			
			if(mp!=null)
			{
				if(mp.isPlaying())
				{
					mp.pause();
					mp.stop();
				}
			}
			cplaying = 0;
		}
	
	}
	public static class SoundPlayer
	{
		public static final int SS = R.raw.ringtone_cut;
		private static SoundPool soundPool;
		private static HashMap soundPoolMap;
		public static void initSounds(Context context) 
		{
			soundPool = new SoundPool(2,AudioManager.STREAM_NOTIFICATION,100);
			soundPoolMap = new HashMap(3);
			soundPoolMap.put(SS, soundPool.load(context,R.raw.ringtone_cut,1));
			
		}
		public static void playSound()
		{
			if(mp.isPlaying()==false)
			{
				mp.start();
				
			}
			
			
		}
	}
	public void playSound(int soundType)
	{
		switch(soundType)
		{
			case RINGTONE_SHORT:
				if(mp==null||cplaying==0||cplaying==RINGTONE_LONG)
					mp = MediaPlayer.create(this, R.raw.ringtone_cut);
				cplaying=RINGTONE_SHORT;
				if(mp.isPlaying()==false)
					SoundPlayer.playSound();
				break;
			case RINGTONE_LONG:
				if(mp==null||cplaying==0||cplaying==RINGTONE_SHORT)
					mp = MediaPlayer.create(this, R.raw.ringtone_extended);
				cplaying=RINGTONE_LONG;
				if(mp.isPlaying()==false)
					SoundPlayer.playSound();
				break; // */
		}
	}
	public void sendToMainActivity(List<Player> players, List<LogEntry> logEntries, int flags)
	{
		testForPlayers();
		if(MainActivity.getInstance()!=null)
			MainActivity.getInstance().postXmlUpdateUI(players, logEntries);
		else
			Log.w("Notifier service", "Mainactivity was null!");
	}
	public void testForPlayers()
	{
		for (Player p:players)
		{
			if(p.isOnline())
			{
				notifyOnline(p);
			}
		}
	}
	public void autoUpdateFunction()
	{
		Log.v("Notifier service", "Refreshing...");
		getXmlFromServer();
		if(dirty)
		{
			MainActivity.getInstance().setPlayersArray(players);
			MainActivity.getInstance().setLogArray(logEntries);
			MainActivity.getInstance().downloadHeads();
		}
		//else if(mainActivityBound)
			//MainActivity.getInstance().downloadHeads();
		handleSounds(players);
		testForPlayers();
		
	}
	public void launchAutoUpdater()
	{
		autoUpdate=true;
		Log.v("Notifier service","Auto-updater started");
		
		notifierTask.run();
		if(mainActivityBound)
		{
			MainActivity.getInstance().setUIRefreshing(true);
			MainActivity.getInstance().downloadHeads();
		}
		else
			Log.d("Notifier service", "Tried to update nonexistent mainActivity");
	}
	public void killAutoUpdater()
	{
		Log.v("Notifier service","Auto-updater killed");
		notificationHandler.removeCallbacks(notifierTask);
		autoUpdate=false;
		if(mainActivityBound)
			MainActivity.getInstance().setUIRefreshing(false);
		else
			Log.d("Notifier service","Tried to kill service without updating UI");
		
	}
}
