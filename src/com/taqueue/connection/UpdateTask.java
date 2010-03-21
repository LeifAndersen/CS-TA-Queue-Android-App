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
import com.taqueue.queue.TAQueue;
/**
 * Class that handled repeatedly sending update requests to the queue to keep the queue up to date
 */
public class UpdateTask extends AsyncTask<Void,String,Void>{
        private QueueConnectionManager manager;
        private TAQueueActivity activity;
        private TAQueue queue;
        private boolean loopForever;
        public UpdateTask(TAQueueActivity act,QueueConnectionManager m, TAQueue queue,boolean loopForever){
                manager = m;
                activity = act;
                this.queue = queue;
                this.loopForever = loopForever;
        }
        @Override
        /**
         * Send update requests and then sleep and repeat untill killed
         */
        protected Void doInBackground(Void... v){
                //update, if loopForever is true this will run until killed by the activity, otherwise it will just run once
                do{
                        String message;
                        ConnectionResult res = manager.update();
                        if(res.status == ConnectionStatus.OK)
                                message = res.message;
                        else
                                message = res.status.toString();
                        publishProgress(message);
                        //sleep for 2.5 seconds then do it again :D
                        SystemClock.sleep(2500);
                }while(loopForever);
                return null;
        }
        /**
         * Called whenever we get a new update from the queue, this passes it up to the activity for displaying
         */
        protected void onProgressUpdate(String... v){
                queue.parseUpdate(v[0]);
                activity.doUpdate();
        }
}
