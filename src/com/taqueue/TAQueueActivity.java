/*
 * Copyright 2010 Chad Brubaker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taqueue;

import android.app.ListActivity;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.SimpleAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CheckBox;
import android.content.SharedPreferences;
import android.view.View;
import com.taqueue.connection.*;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import android.util.Log;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import com.taqueue.queue.*;
/**
 * Activity that handles our UI(and updating it)
 */
public class TAQueueActivity extends ListActivity{
	/**
	 * Dialog informing of a connection loss
	 */
	private static final int CONNECTION_LOSS_DIALOG = 1; 
	/**
	 * File where preferences will be saved
	 */
	private static final String PREFS="TAQueuePrefs";
	/**
	 * TAQueue containing the state of the queue(active/inactive/frozen and who is active)
	 */
	private TAQueue queue;
	/**
	 * Connection manager for our connection to the queue
	 */
	private QueueConnectionManager manager;
	/**
	 * Task that sends the queue an update repeatedly(at a delay)
	 */
	private UpdateTask updater;

	/**
	 * The string prefixing names in the textview "Name:" by default
	 */
	private String nameStr;
	/**
	 * The string prefixing machine names in the textview "Machine:" by default
	 */
	private String machineStr;
	private SimpleAdapter adapter;
	/**
	 * Called whenever an update to the queue has happened, will then update the UI to reflect the changes
	 */
	public void doUpdate(){
		//if we are in the process of closing then ignore everything
		if(updater == null)
			return;
		//first, update the state
		TextView status = (TextView) findViewById(R.id.status);
		//set the state text based on the queue state
		switch(queue.getState()){
			case STATE_ACTIVE: //active
				status.setText(R.string.status_active);
				status.setTextColor(this.getResources().getColor(R.drawable.active_color));
				break;
			case STATE_INACTIVE: //inactive
				status.setText(R.string.status_inactive);
				status.setTextColor(this.getResources().getColor(R.drawable.inactive_color));
				break;
			case STATE_FROZEN: //frozen
				status.setText(R.string.status_frozen);
				status.setTextColor(this.getResources().getColor(R.drawable.frozen_color));
				break;
			case STATE_UNCONNECTED:
				//there was a connection error, tell the user and bail!
				this.updater.cancel(true);
				this.updater = null;
				showDialog(CONNECTION_LOSS_DIALOG);
				return;
		}
		//now update the section
		TextView section = (TextView) findViewById(R.id.section);
		section.setText(getResources().getString(R.string.section) + queue.getSection());
		//now set up the adapter
		ArrayList<Map<String,String>> itemsList = new ArrayList<Map<String,String>>();
		//load student info
		for(Student s : queue.getStudents()){
			HashMap<String,String> map = new HashMap<String,String>();
			map.put("name",nameStr+s.getName());
			map.put("machine",machineStr+s.getMachine());
			itemsList.add(map);
		}
		//and display
		adapter = new SimpleAdapter(this,itemsList,R.layout.student_entry,new String[]{"name","machine"},new int[]{R.id.name,R.id.machine});
		setListAdapter(adapter);
	}
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.queue);
		//store the name and machine prefixes
		nameStr = getResources().getString(R.string.student_name);
		machineStr = getResources().getString(R.string.machine_name);
		//load state if we have one(and we have connected)
		if(savedInstanceState != null &&  savedInstanceState.containsKey("Connection Manager")){
			//if we have a saved state then use our old values
			manager = (QueueConnectionManager) savedInstanceState.getSerializable("Connection Manager");
			queue = (TAQueue) savedInstanceState.getSerializable("Queue");

			//spawn the update task
			if(updater != null){
				updater.cancel(true);
			}
			updater = new UpdateTask(this,manager,queue,true);
			updater.execute();
			//finally, update the display based on our saved values
			this.doUpdate();
		}else{ //otherwise we were just created, get the bundle from the intent and extract the manager
			queue = new TAQueue();
			Bundle b = this.getIntent().getExtras();
			if(b == null){ //handle error
				this.finish();
			}
			manager = (QueueConnectionManager) b.getSerializable("manager");
			if(manager == null || !manager.isConnected()){//handle error
				this.finish();
			}
			queue.setSection(manager.getSection());
			updater = new UpdateTask(this,manager,queue,true);
			updater.execute();
		}

	}
	public void onResume(){
		//if the connection is good then start updating again
		if(manager.isConnected()){
			if(updater != null)
				updater.cancel(true);
			updater = new UpdateTask(this,manager,queue,true);
			updater.execute();
		}
		super.onResume();
	}
	public void onDestroy(){
		this.adapter = null;
		if(updater != null){
			updater.cancel(true);
			updater = null;
		}
		super.onDestroy();
	}
	public void onStop(){
		//if we are not looking at the queue no real point in keeping it up to date
		if(updater != null){
			updater.cancel(true);
			updater = null;
		}
		super.onStop();
	}
	/**
	 * Method called when the remove button is clicked on a student. 
	 * Sends a remove message to the queue(the actual UI clearing will be handled in the next update)
	 * @param v the view that was clicked
	 */
	public void onRemoveClick(View v){
		//get the entry we are in
		RelativeLayout parent = (RelativeLayout)v.getParent();
		//parse the name/machine
		TextView nameView = (TextView) parent.getChildAt(1);
		TextView machineView = (TextView) parent.getChildAt(2);
		//split the name and machine from the view text
		String name = nameView.getText().toString().split(nameStr,2)[1];
		String machine = machineView.getText().toString().split(machineStr,2)[1];
		//spawn a task to do the remove
		new RemoveTask(manager,name,machine).execute();
		//force a single update
		UpdateTask single = new UpdateTask(this,manager,queue,false);
		single.execute();
	}
	/**
	 * Changes the state of the queue(if connected). Used for the activate/freeze/deactivate
	 * buttons.
	 * @param newState the state the queue should change to
	 * If the queue is not connected then changeState will do nothing.
	 */
	private void changeState(QueueState newState){
		//only try to change state if we are connected
		if(!manager.isConnected())
			return;
		//change the state
		switch(newState){
			case STATE_ACTIVE:
				manager.activate();
				break;
			case STATE_INACTIVE:
				manager.deactivate();
				break;
			case STATE_FROZEN:
				manager.freeze();
				break;
		}
		//cause a single update(helps sluggishness)
		new UpdateTask(this,manager,queue,false).execute();
	}
	/**
	 * Called when the user presses the Activate button, activates the queue if the queue is connected
	 */
	public void onActivateClick(View v){
		this.changeState(QueueState.STATE_ACTIVE);
	}
	/**
	 * Called when the user presses the Freeze buttons, freezes the queue if the queue is connected
	 */
	public void onFreezeClick(View v){
		this.changeState(QueueState.STATE_FROZEN);
	}
	/**
	 * Called when the user presses the Deactivate buttons, deactivates the queue if the queue is connected
	 */
	public void onDeactivateClick(View v){
		this.changeState(QueueState.STATE_INACTIVE);
	}
	/**
	 * Called when we need to save the app state, adds the connection manager to the bundle
	 */
	protected void onSaveInstanceState(Bundle outState){
		//save the connection manager only if we are connected, otherwise don't bother
		if(this.manager.isConnected()){
			outState.putSerializable("Connection Manager",this.manager);
			outState.putSerializable("Queue",this.queue);
		}
	}
	/**
	 * Called when we want to create a dialog
	 * @param val the dialog to be created(see const definitions at the top)
	 */
	protected Dialog onCreateDialog(int val){
		//builder for any alerts we need
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		//choose which dialog to make
		switch(val){
			case CONNECTION_LOSS_DIALOG:
				builder.setMessage(getResources().getString(R.string.conn_loss))
					.setPositiveButton("OK",new DialogInterface.OnClickListener(){
							public void onClick(DialogInterface dialog, int id){
							dialog.cancel();
							((Dialog)dialog).getOwnerActivity().finish();
							}
							});
				return builder.create();
		}
		//otherwise it was a bad value, return null
		return null;
	}


}
