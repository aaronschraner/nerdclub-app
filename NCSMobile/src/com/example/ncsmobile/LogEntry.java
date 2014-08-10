package com.example.ncsmobile;

import java.util.ArrayList;

import android.graphics.Color;

public class LogEntry
{
	String ownerName; //person who said the line
	String time; //time the thing was said
	String content; //what was said
	Player owner;
	ArrayList<Player> potentialOwners;
	public LogEntry(String owner, String time, String content) //constructor method
	{
		this.ownerName=owner;
		this.time=time;
		this.content=content;
	}
	public LogEntry(String owner, String time, String content, ArrayList<Player> players) //constructor method
	{
		this.ownerName=owner;
		this.time=time;
		this.content=content;
		for (Player p : players)
		{
			if(p.getName().equals(ownerName))
			{
				this.owner=p;
			}
		}
		
	}
	public void setPlayerDB(ArrayList<Player> players)
	{
		potentialOwners=players;
		for (Player p : players)
		{
			if(p.getName().equals(ownerName))
			{
				this.owner=p;
			}
		}
	}
	//accessor methods
	public String getOwnerName()
	{
		return ownerName;
	}
	public Player getOwner(ArrayList<Player> players)
	{
		for (Player p : players)
		{
			if(p.getName().equals(ownerName))
			{
				return p;
			}
		}
		return null;
		
	}
	public Player getOwner()
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
		int preColor=Color.DKGRAY;
		if(!(owner==null)&&!(potentialOwners==null))
		{
			preColor = owner.getColor();
		}
		else if(this.ownerName.equals("Server"))
		{
			for( Player p:potentialOwners)
			{
				if(p.getName().equals("FFmpex"))
				{
					preColor=p.getColor();
				}
			}
		}
		int red=Color.red(preColor);
		int green=Color.green(preColor);
		int blue=Color.blue(preColor);
		int avg=(red+green+blue)/3;
		red-=avg*3/4;
		green-=avg*3/4;
		blue-=avg*3/4;
		red*=2;
		green*=2;
		blue*=2;
		
		int postColor=Color.rgb(red,green,blue);
		return postColor;
	}
}
