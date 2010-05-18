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
package com.taqueue.connection;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.app.ProgressDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import com.taqueue.LoginActivity;
/**
*/
public class ConnectTask extends AsyncTask<Void,Void,ConnectionStatus>{
	private QueueConnectionManager manager;
	private String section;
	private String password;
	private LoginActivity callback;
	private Dialog connectDialog;
	/**
	 * ConnectTask constructor
	 * @param callback the activity to call once the connection completes
	 * @param manager QueueConnectionManager to use to connect
	 * @param section Section to register
	 * @param password Password for section's queue
	 */
	public ConnectTask(LoginActivity callback,QueueConnectionManager manager,String section, String password){
		this.callback = callback;
		this.manager = manager;
		this.section = section;
		this.password = password;
	}
	protected void onPreExecute(){
		connectDialog = ProgressDialog.show(callback,"","Connecting, please wait...",true,true,
			new DialogInterface.OnCancelListener(){
				 public void onCancel(DialogInterface dialog){
				  	ConnectTask.this.cancel(true);
				  }
			}
			);
	}
	@Override
	/**
	 * Simply connects to the section and returns the result
	 */
	protected ConnectionStatus doInBackground(Void... v){
		return manager.connectTo(section,password);
	}
	protected void onPostExecute(ConnectionStatus s){
		connectDialog.dismiss();
		callback.onConnection(s);
	}
}
