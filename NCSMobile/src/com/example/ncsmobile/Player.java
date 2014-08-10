package com.example.ncsmobile;

import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Player //class to hold a player's information
{
	String pName; //player name
	int color; //profile/chat color
	URL sHeadURL; //URL of small (chat) head
	URL bHeadURL; //URL of larger (profile) head
	boolean isOnline; //if the player is currently online
	//boolean isLogged; //for players who are in the whitelist who haven't played since
	//before the oldest log file still existing.
	Bitmap smallHead;
	Bitmap bigHead;
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
	public URL getSmallHeadURL()
	{
		return sHeadURL;
	}
	public URL getBigHeadURL()
	{
		return bHeadURL;
	}
	public boolean isOnline()
	{
		return isOnline;
	}
	public String toString()
	{
		String str;
		if (isOnline)
		{
			str=" Online";
		}
		else
		{
			str=" Offline";
		}
		return pName + str;
	}
	public void setBigHead(Bitmap head)
	{
		bigHead=head;
	}
	public void setSmallHead(Bitmap head)
	{
		smallHead=head;
	}
	public Bitmap getSmallHead()
	{
		return smallHead;
	}
	public Bitmap getBigHead()
	{
		return bigHead;
	}
	private int view;
	public void setView(int view)
	{
		this.view=view;
	}
	public int getView()
	{
		return view;
	}
	
}
