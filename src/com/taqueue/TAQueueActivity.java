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
public class TAQueueActivity extends ListActivity
{
        /*
         * IDs for dialogs, see onCreateDialog for more info
         */
        public static final int DIALOG_CONNECTING = 0;
        public static final int DIALOG_BAD_LOGIN = 1;
        public static final int DIALOG_CONNECTION_ERROR=2;

        /**
         * File where preferences will be saved
         */
        private static final String PREFS="TAQueuePrefs";
        /**
         * Dialog showing the "please wait" while connecting, will be dismissed once the connectTask finishes
         */
        private ProgressDialog connectingDialog;
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
                                //we aren't connected yet, so don't even bother doing anything more[this shouldn't happen, but if it does...]
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
                //setContentView(R.layout.login);
                //store the name and machine prefixes
                nameStr = getResources().getString(R.string.student_name);
                machineStr = getResources().getString(R.string.machine_name);
                //load state if we have one(and we have connected
                if(savedInstanceState != null &&  savedInstanceState.containsKey("Connection Manager")){
                        //if we have a saved state then use our old values
                        setContentView(R.layout.queue);
                        //load the connection manager
                        manager = (QueueConnectionManager) savedInstanceState.getSerializable("Connection Manager");
                        //load the queue
                        queue = (TAQueue) savedInstanceState.getSerializable("Queue");
                        
                        //spawn the update task
                        if(updater != null){
                                updater.cancel(true);
                        }
                        updater = new UpdateTask(this,manager,queue,true);
                        updater.execute();
                        //finally, update the display based on our saved values
                        this.doUpdate();
                }else{ //if we don't have a saved state then ask for login
                        manager = new QueueConnectionManager();
                        queue = new TAQueue();
                        setContentView(R.layout.login);

                        //load the saved section/password
                        SharedPreferences settings = getSharedPreferences(PREFS,0);
                        String section = settings.getString("section","");
                        String password = settings.getString("password","");
                        //load if the save login check box is checked
                        boolean isSaveChecked = settings.getBoolean("saveChecked",false);
                        ((CheckBox)findViewById(R.id.saveCheckBox)).setChecked(isSaveChecked);
                        //and set the EditText's text
                        EditText sectionText = (EditText) findViewById(R.id.sectionInput);
                        EditText passwordText = (EditText) findViewById(R.id.passwordInput);
                        sectionText.setText(section);
                        passwordText.setText(password);


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
                //remove all callbacks 
                this.adapter = null;
                if(updater != null){
                        updater.cancel(true);
                        updater = null;
                }
                super.onDestroy();
        }
        public void onStop(){
                //if we are not looking at the queue no real point in keeping it up to date
                //TODO: decide if canceling is even worth it
                if(updater != null){
                        updater.cancel(true);
                        updater = null;
                }
                super.onStop();
        }
        /**
         * Method called when the remove button is clicked on a student. Sends a remove message to the queue(the actual UI clearing will be handled in the next update)
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
         * Called when the user presses the Activate button, activates the queue if the queue is connected
         */
        public void onActivateClick(View v){
                //only try to activate if the connection is ok
                if(manager.isConnected()){
                        manager.activate();
                        //force a single update
                        UpdateTask single = new UpdateTask(this,manager,queue,false);
                        single.execute();
                }
        }
        /**
         * Called when the user presses the Freeze buttons, freezes the queue if the queue is connected
         */
        public void onFreezeClick(View v){
                //only try to freeze if the connection is ok
                if(manager.isConnected()){
                        manager.freeze();
                        //force a single update
                        UpdateTask single = new UpdateTask(this,manager,queue,false);
                        single.execute();
                }
        }
        /**
         * Called when the user presses the Deactivate buttons, deactivates the queue if the queue is connected
         */
        public void onDeactivateClick(View v){
                //only try to deactivate if the connection is ok
                if(manager.isConnected()){
                        manager.deactivate();
                        //force a single update
                        UpdateTask single = new UpdateTask(this,manager,queue,false);
                        single.execute();
                }
        }
        /**
         * Called when the user clicks the "Connect" button in the login screen
         */
        public void onConnectClick(View v){
                //get the login info
                String section = ((TextView)findViewById(R.id.sectionInput)).getText().toString();
                String password = ((TextView)findViewById(R.id.passwordInput)).getText().toString();
                //save the section/password to the preferences if the user wants, otherwise save "" for both
                boolean save = ((CheckBox)findViewById(R.id.saveCheckBox)).isChecked();
                SharedPreferences settings = getSharedPreferences(PREFS,0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("section", (save ? section : ""));
                editor.putString("password", (save ? password : ""));
                editor.putBoolean("saveChecked",save);
                //and save the settings
                editor.commit();
                //spawn a connect task
                new ConnectTask(this,manager,section,password).execute();
                //and spawn a "connecting" dialog
                showDialog(DIALOG_CONNECTING);

        }
        /**
         * Called when we want to create a dialog
         * @param val the dialog to be created(see const definitions at the top of TAQueueActivity)
         */
        protected Dialog onCreateDialog(int val){
                //builder for any alerts we need
                AlertDialog.Builder builder= new AlertDialog.Builder(this);
                //choose which dialog to make
                switch(val){
                        //dialog showing that the queue is connecting
                        case DIALOG_CONNECTING:
                                connectingDialog = ProgressDialog.show(this,"","Connecting. Please wait...",true,false);
                                return connectingDialog;
                        //alert dialog for bad password
                        case DIALOG_BAD_LOGIN:
                                builder.setMessage(getResources().getString(R.string.bad_password))
                                .setPositiveButton("OK",new DialogInterface.OnClickListener(){
                                        public void onClick(DialogInterface dialog, int id){
                                                dialog.cancel();
                                        }
                                });
                                return builder.create();
                        //alert dialog for when there is an error connecting to the queue
                        case DIALOG_CONNECTION_ERROR:
                                builder.setMessage(getResources().getString(R.string.error_connecting))
                                .setPositiveButton("OK",new DialogInterface.OnClickListener(){
                                        public void onClick(DialogInterface dialog, int id){
                                                dialog.cancel();
                                        }
                                });
                                return builder.create();
                }
                //otherwise it was a bad value, return null
                return null;
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
         * Called when a ConnectTask completes
         * @param status the status of the QueueConnection
         */
        public void onConnection(ConnectionStatus s){
                //finish the progress dialog
                this.connectingDialog.cancel();
                removeDialog(DIALOG_CONNECTING);
                ConnectionStatus status = s;
                //if we connected start everything up
                if(status == ConnectionStatus.OK){
                        TextView statusView = (TextView) findViewById(R.id.status);
                        setContentView(R.layout.queue);
                        queue.setSection(manager.getSection());
                        //spawn an updater that updates forever
                        updater = new UpdateTask(this,manager,queue,true);
                        updater.execute();
                }else if(status == ConnectionStatus.BAD_PASSWORD){
                        //show a bad password alert
                        showDialog(DIALOG_BAD_LOGIN);
                }else{
                        //show a connection error alert
                        showDialog(DIALOG_CONNECTION_ERROR);
                }
        }
}
