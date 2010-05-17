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

import android.app.Activity;
import android.content.res.Resources;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CheckBox;
import android.content.SharedPreferences;
import android.view.View;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;

import com.taqueue.queue.*;
import com.taqueue.connection.*;

public class LoginActivity extends Activity{

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
        private QueueConnectionManager manager;
        /**
         * Dialog showing the "please wait" while connecting, will be dismissed once the connectTask finishes
         */
        private ProgressDialog connectingDialog;

        public void onCreate(Bundle savedInstanceState){
                super.onCreate(savedInstanceState);
                setContentView(R.layout.login);
		manager = new QueueConnectionManager();
		//load the saved section/password
		SharedPreferences settings = getSharedPreferences(PREFS,0);
		String section = settings.getString("section","");
		String password = settings.getString("password","");
		//load if the save login check box is checked
		boolean isSaveChecked = settings.getBoolean("saveChecked",false);
		((CheckBox)findViewById(R.id.saveCheckBox)).setChecked(isSaveChecked);
		//and set the EditText's text
		((EditText) findViewById(R.id.sectionInput)).setText(section);
		((EditText) findViewById(R.id.passwordInput)).setText(password);
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
			//set up the bundle of stuff we need to pass to the queue
			Bundle b = new Bundle();
			b.putSerializable("manager",manager);
			//set up the intent
			Intent i = new Intent(LoginActivity.this,TAQueueActivity.class);
			i.putExtras(b);
			startActivity(i);
                }else if(status == ConnectionStatus.BAD_PASSWORD){
                        //show a bad password alert
                        showDialog(DIALOG_BAD_LOGIN);
                }else{
                        //show a connection error alert
                        showDialog(DIALOG_CONNECTION_ERROR);
                }
        }
}
