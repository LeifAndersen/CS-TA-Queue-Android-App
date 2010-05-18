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
import com.taqueue.TAQueueActivity;
/**
 * Class that handles sending a remove request to the queue, response is ignored(for now)
 * Handled using a task so that the UI doesn't lag in the event of connection issues
 */
public class RemoveTask extends AsyncTask<Void,Void,Void>{
	private QueueConnectionManager manager;
	private String name,machine;
	public RemoveTask(QueueConnectionManager m,String name, String machine){
		manager = m;
		this.name = name;
		this.machine=machine;
	}
	@Override
	/**
	 * Send the remove request to the server
	 */
	protected Void doInBackground(Void... v){
		manager.remove(name,machine);
		//value is ignored right now due to the queue not sending us back anything worth while
		return null;
	}
}
