package com.example.ncsmobile;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PlayerAdapter extends ArrayAdapter<Player>
{

	public PlayerAdapter(Context context, List<Player> objects)
	{
		super(context, 0, objects);
		
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if(convertView == null)
		{
			LayoutInflater inflater=LayoutInflater.from(getContext());
			convertView=inflater.inflate(R.layout.list_item_player, parent, false);
		}
		Player currentPlayer=getItem(position);
		ImageView imageHead = (ImageView)convertView.findViewById(R.id.image_head);
		TextView textName= (TextView)convertView.findViewById(R.id.text_name);
		TextView textOnline = (TextView)convertView.findViewById(R.id.text_online);
		
		if ( currentPlayer.isOnline())
		{
			textOnline.setTextColor(Color.GREEN);
			textOnline.setText("Online");
		}
		else
		{
			textOnline.setText("Offline");
			textOnline.setTextColor(Color.RED);
		}
		
		textName.setText(currentPlayer.getName());
		convertView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v)
			{
				//launch activity with embedded player intent
				
			}
		});
		imageHead.setImageBitmap(currentPlayer.getSmallHead());
		//Log.v("Heads","Displaying " + currentPlayer.getName() + "'s small head");
		
		//imageHead.setImageURI(Uri.parse(currentPlayer.getBigHead().toString()));
		return convertView;
	}
}
