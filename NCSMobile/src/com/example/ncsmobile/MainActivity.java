package com.example.ncsmobile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
	
	String firstContainer=null;
	private static MainActivity instance;
	TextView tv; //view to hold fetched XML (Debug)
	Button button2; //Force call parse XML (Debug)
	String sDataUrl="http://71.193.212.135/app/status.xml"; //URL to grab data from
	String xmlText=null; //variable to put downloaded XML data into.
	public static final int MODE_CHAT=45; //mode for when parser is scanning chat entries
	public static final int MODE_PLAYERS=46; //mode for when parser is scanning players
	public static final String smallHeadSubDir="/small/";
	public static final String bigHeadSubDir="/big/";
	public static final String headDir="/head/";
	public static final String KEY_SERVER_XML = "KEY_SERVER_XML";
	public static final int RINGTONE_LONG=320;
	public static final int RINGTONE_SHORT=321;
	public static final int DELAY_REFRESH = 5000;
	private static final int FLAG_SOUND_ONLY = 11;
	private static final int FLAG_UPDATE_NORMAL = 15;
	public static final int ONLINE_NOTIFY=34;
	private static final String KEY_AUTO_UPDATE = "KEY_AUTO_UPDATE";
	ArrayAdapter playerAdapter;
	ArrayAdapter logEntryAdapter;
	ListView playerListView;
	ListView logListView;
	final Handler serviceHandler = new Handler();
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.v("Startup", "onCreate called");
		setContentView(R.layout.layout_drawer);
		Log.v("Startup", "set content view to drawer layout");
		//getXmlFromServer();
		//((Button)findViewById(R.id.delete_button)).setOnClickListener(new OnClickListener(){public void onClick(View v){launchRealService();}});
		Log.v("Startup","Assigned onClickListener to launch auto-notifier");
		instance=this;
		//NotifierService.getInstance().launchAutoUpdater();
		//((Button)findViewById(R.id.delete_button)).setOnClickListener(new OnClickListener(){public void onClick(View v){stopService(new Intent(getBaseContext(), NotifierService.class));}});
		//setUIRefreshing(true);
		launchRealService();
	}
	private Toast xmlToast;
	protected void onSaveInstanceState(Bundle outState) //doesn't save state
	{
		super.onSaveInstanceState(outState);
		
		outState.putBoolean(KEY_AUTO_UPDATE, NotifierService.getInstance().autoUpdate);
	}
	public static  MainActivity getInstance() //returns the instance of MainActivity so the service can access it.
	{
		return instance;
	}
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) //just calls getXmlFromServer and handles changes
	{
		super.onRestoreInstanceState(savedInstanceState);
		//String oldXmlTxt=savedInstanceState.getString(KEY_SERVER_XML);
		try
		{
			NotifierService.getInstance().getXmlFromServer();
		}catch(NullPointerException e)
		{
			Log.w("UI","Tried to launch getXmlFromServer on null notifierservice");
		}
		
		
	}
	
	List<LogEntry> logEntries= new ArrayList<LogEntry>(); //list of log entries extracted from XML
	List<Player> players=new ArrayList<Player>(); //list of players extracted from XML
	
	
	private NotifierService notifierBoundService;
	private ServiceConnection notifierConnection = new ServiceConnection() //initialize service connection
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			notifierBoundService = ((NotifierService.NotifierBinder)service).getService();
			Log.v("Notifier service", "Notifier connected!");
			Toast.makeText(MainActivity.this, "Service connected!", Toast.LENGTH_SHORT).show();
		}
		public void onServiceDisconnected(ComponentName className)
		{
			notifierBoundService = null;
			Log.v("Notifier service", "Notifier disconnected!");
			Toast.makeText(MainActivity.this, "Service disconnected.", Toast.LENGTH_SHORT).show();
		}
	};

	public boolean notifierIsBound=false;
	public void launchRealService() //Launch the notifier service (for real this time)
	{
		Log.v("Notifier service", "launch method called");
		startService(new Intent(MainActivity.this, NotifierService.class));
		
		bindNotifierService();
	}
	void bindNotifierService()
	{
		Intent notifyIntent = new Intent(MainActivity.this, NotifierService.class);
		bindService(notifyIntent, notifierConnection, Context.BIND_AUTO_CREATE);
		//startService(notifyIntent);
		Log.v("Notifier service", "Notifier bound!");
		notifierIsBound=true;
		
	}
	void unBindNotifierService()
	{
		if(notifierIsBound)
		{
			unbindService(notifierConnection);
			Log.v("Notifier service", "Notifier unbound!");
			notifierIsBound=false;
		}
	}
	Menu myMenu = null;
	public static boolean autorefreshRunning=false;
	public void postXmlUpdateUI(List<Player> players, List<LogEntry> logEntries)
	{
		for (LogEntry le:logEntries)
		{
			le.setPlayerDB((ArrayList<Player>)players); //ensure each log entry can find its owner
		}
		if(firstContainer==null)
		{
			playerAdapter= new PlayerAdapter(this, players);
			logEntryAdapter=new LogEntryAdapter(this, logEntries);
			logListView = (ListView) findViewById(R.id.chatview);
			logListView.setAdapter(logEntryAdapter);
			playerListView=(ListView) findViewById(R.id.listview);
			playerListView.setAdapter(playerAdapter); 
			//xmlToast.cancel();
			firstContainer="Not null anymore.";
			Log.v("UI","Assigned adapters");
		}
		else
		{
			playerAdapter.notifyDataSetChanged();
			logEntryAdapter.notifyDataSetChanged();
			Log.v("UI", "Updated UI ");
		}
		//
		downloadHeads();
		/*for (Player p:players)
		{
			putHeadInPlayer(p);
			playerAdapter.notifyDataSetChanged();
		}*/
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.global, menu);
		myMenu=menu;
		launchRealService(); 
		return super.onCreateOptionsMenu(menu);
	}
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.action_refresh: //force update button (the one with no letter in it)
				Log.v("Notifier service", "Menu item clicked");
				NotifierService.getInstance().getXmlFromServer();
				Log.v("Notifier service", "GetXmlFromServer launched ");
				return true;
				
			case R.id.action_autorefresh: //auto-update button
				if(NotifierService.getInstance().autoUpdate) //if notifier is set to auto-update
					NotifierService.getInstance().launchAutoUpdater(); //start the auto-updater
				else //if it's not set to auto update
				{
					NotifierService.getInstance().killAutoUpdater(); //stop the auto-updater
				}
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	public void setPlayersArray(List<Player> players) //so the notification can send MainActivity the players
	{
		this.players=players;
	}
	public void setLogArray(List<LogEntry> logEntries)
	{
		this.logEntries=logEntries;
	}
	public void pushString(String string)
	{
		Log.w("General", "Warning: something called pushString in MainActivity"); //for debug.
	}
	
	public void setUIRefreshing(boolean refreshing)  //set the autorefresh button to reflect the current state.
	{
		MenuItem autorefresh = (MenuItem) myMenu.findItem(R.id.action_autorefresh); //get reference to autoupdate button
		if(refreshing) //if the UI should look like it's refreshing
		{
			autorefresh.setIcon(R.drawable.ic_menu_autorefresh_on); //set the icon of the button to reflect that it is updating
			autorefresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					NotifierService.getInstance().killAutoUpdater(); //make it so when the button is pressed, it kills the updater.
					return false;
				}
			});
		}
		else
		{
			
			autorefresh.setIcon(R.drawable.ic_menu_autorefresh_off); //set the icon of the button to appear stopped
			autorefresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					NotifierService.getInstance().launchAutoUpdater(); //set the button to relaunch the auto-updater when the button is pressed
					return false;
				}
			});
			
		}
	}
	public void downloadHeads() //function to procedurally download and store all player heads
	{
		Log.d("Heads", "DownloadHeads invoked.");
		boolean headsHere=false;
		for (Player p:players) //iterate through all players
		{
			if(!hasHead(p)) //if player's head file does not exist
			{
				headsHere=true; //indicate that the head is still downloading and not ready to be displayed
				new headDownloaderTask().execute(p); //download and save the head
				Log.d("Heads", "Downloading head for " + p.getName());
			}
			
			if(!headsHere) //if the head is ready to be displayed
			{
				putHeadInPlayer(p); //generate the bitmap for this player
				playerAdapter.notifyDataSetChanged(); //update the views
				logEntryAdapter.notifyDataSetChanged();
			}
			try
			{
				putHeadInPlayer(p);
			}catch(Exception e){
				Log.w("Heads","Putting head in player " + p.getName() + " failed.");
				e.printStackTrace();
			}
			
			
		}
		
	}
	public class headDownloaderTask extends AsyncTask <Player, Void, Player> //task to download heads
	{
		
		@Override
		protected Player doInBackground(Player... params )
		{
			if(params==null||params.length == 0)
			{
				return null; //cancel if nothing useful passed
			}
			Log.v("Heads","Head downloader task launched.");
			boolean success=true;
			HttpClient clientS = new DefaultHttpClient(); //HTTP client for small head (used in chat)
			HttpClient clientB = new DefaultHttpClient(); //HTTP client for big head (used in online list/profile view)
			Bitmap rawResponse = null;
			int howManyPlayers=params.length;
			int playerIndex = 0;
			for(Player cPlayer:params)
			{
				HttpGet currentBigHeadGet = new HttpGet(cPlayer.getBigHeadURL().toString()); //get big head
				HttpGet currentSmallHeadGet = new HttpGet(cPlayer.getSmallHeadURL().toString()); //get small head
				try{
					Log.v("Heads", "Getting heads for " + cPlayer.getName() + " (" + playerIndex + "/" + params.length + ")");
					HttpResponse bigHeadResponse = clientB.execute(currentBigHeadGet); //execute download for big head
					Log.v("Heads", "Done getting " + cPlayer.getName() + "'s big head");
					HttpResponse smallHeadResponse = clientS.execute(currentSmallHeadGet); //execute download for small head
					Log.v("Heads", "Done getting " + cPlayer.getName() + "'s small head");
					byte[] rawBigHead=EntityUtils.toByteArray(bigHeadResponse.getEntity());
					byte[] rawSmallHead=EntityUtils.toByteArray(smallHeadResponse.getEntity());
					FileOutputStream bigHeadOutStream = new FileOutputStream(
							getCacheDir() + headDir + bigHeadSubDir + 
							cPlayer.getName() + ".png"); //write big head to file
					FileOutputStream smallHeadOutStream = new FileOutputStream(
							getCacheDir() + headDir + smallHeadSubDir + 
							cPlayer.getName() + ".png"); //write small head to file
					bigHeadOutStream.write(rawBigHead);
					smallHeadOutStream.write(rawSmallHead);
					Log.v("Heads", "Saved " + cPlayer.getName() + "'s heads.");
		 		}
				catch(ClientProtocolException e)
				{
					e.printStackTrace();
					Log.w("Heads","Something happened in the head downloader (Client protocol exception)");
				}
				catch(IOException e)
				{
					e.printStackTrace();
					Log.w("Heads", "Something happened in the head downloader (IO exception of some kind)");
				}
				finally
				{
					//give up
				}
				
				playerIndex++;
			}
			return params[0];
		}
		protected void onPostExecute(Player player)
		{
			putHeadInPlayer(player); //assign a drawable resource for the head
			playerAdapter.notifyDataSetChanged();
		}
	}
	public void clearCache() //for future use, when a player's head is updated
	{
		for( File d:(((new File(getCacheDir().getAbsolutePath() + headDir +
				smallHeadSubDir))).listFiles()))
		{
			d.delete();
		}
		for( File d:(((new File(getCacheDir().getAbsolutePath() + headDir +
				bigHeadSubDir))).listFiles()))
		{
			d.delete();
		}
		
	}
	public boolean hasHead(Player player) //does the player's head file exist?
	{
		File headFolder = new File(getCacheDir().getAbsolutePath() + headDir);
		if (!headFolder.exists())
		{
			headFolder.mkdir();
		}
		File smallHeadFolder = new File(getCacheDir().getAbsolutePath() + headDir + smallHeadSubDir);
		if(!smallHeadFolder.exists())
		{
			smallHeadFolder.mkdir();
		}
		File bigHeadFolder = new File(getCacheDir().getAbsolutePath() + headDir + bigHeadSubDir);
		if(!bigHeadFolder.exists())
		{
			bigHeadFolder.mkdir();
		}
		File smallHead = new File(
				getCacheDir().getAbsolutePath() + 
				headDir + smallHeadSubDir + player.getName() + ".png");
		File bigHead = new File(
				getCacheDir().getAbsolutePath() + 
				headDir + bigHeadSubDir + player.getName() + ".png");
		
		return (bigHead.exists()&&smallHead.exists());
		
		
	}
	public void putHeadInPlayer(Player player) //create a bitmap for the player's head file and put it in their object.
	{
		if(hasHead(player)) //if the player's head files exist
		{
			Bitmap bigHeadBitmap = BitmapFactory.decodeFile(
					getCacheDir().getAbsolutePath() +headDir+
					bigHeadSubDir + player.getName() + ".png"); //get displayable bitmaps from the files
			
			Bitmap smallHeadBitmap = BitmapFactory.decodeFile(
					getCacheDir().getAbsolutePath() +headDir+
					smallHeadSubDir + player.getName() + ".png");
			
			player.setBigHead(bigHeadBitmap);
			player.setSmallHead(smallHeadBitmap); //put the bitmaps in the player object
			
		}
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		unBindNotifierService();
		Log.v("UI","MainActivity destroyed.");
		notifierIsBound=false;
	}
}
