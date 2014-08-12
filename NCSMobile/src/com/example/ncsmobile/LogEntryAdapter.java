package com.example.ncsmobile;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class LogEntryAdapter extends ArrayAdapter<LogEntry>
{
	Context context;
	public LogEntryAdapter(Context context, List<LogEntry> logEntries)
	{
		
		super(context, 0, logEntries);
		this.context=context;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		
		if(convertView == null)
		{
			LayoutInflater inflater=LayoutInflater.from(getContext());
			convertView=inflater.inflate(R.layout.list_item_logentry, parent, false);
		}
		LogEntry currentEntry = getItem(position);
		TextView chatTime=(TextView)convertView.findViewById(R.id.time);
		TextView chatName=(TextView)convertView.findViewById(R.id.playerName);
		TextView chatSaid=(TextView)convertView.findViewById(R.id.playerSaid);
		ImageView playerHead=(ImageView)convertView.findViewById(R.id.chatHead);
		chatTime.setText(currentEntry.getTime());
		chatName.setText(currentEntry.getOwnerName());
		chatSaid.setText(currentEntry.getContent());
		chatName.setTextColor(currentEntry.getColor());
		chatSaid.setTextColor(currentEntry.getColor());
		if(!(currentEntry.getOwner()==null))
		{
			playerHead.setImageBitmap(currentEntry.getOwner().getSmallHead());
		}
		else
		{
			playerHead.setImageResource(R.drawable.abc_ab_bottom_solid_light_holo);
		}
		return convertView;
	}

}
