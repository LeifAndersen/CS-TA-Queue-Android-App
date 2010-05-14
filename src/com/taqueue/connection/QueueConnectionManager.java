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

import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpEntity;
import org.apache.http.params.HttpParams;
import android.os.SystemClock;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.lang.Runnable;
/**
 * Class that manages the connections to the queue server
 */
public class QueueConnectionManager implements java.io.Serializable{
        /**
         * The hostname where the queue is hosted
         */
        private static final String HOST = "http://kodos.eng.utah.edu";
        /**
         * The path to the servlet on the host
         */
        private static final String PATH = "/taqueue/QueueServlet";
        /**
         * Connection timeout in ms
         */
        private static final int CONNECTION_TIMEOUT = 5000;
        /**
         * The 'section'(or class) that we want to be working with
         * @serial
         */ 
        private String section;

        /**
         * The password for the queue, this is all in plain text since it will be sent over the wire
         * that way(Don't blame me, I'm just doing what the client does)
         * @serial
         */
        private String password;

        /**
         * If we are connected to a queue or not
         * @serial
         */
        private boolean connected;

        /**
         * QueueConnectionManager constructor
         */
        public QueueConnectionManager(){
                this.connected = false;
        }
        public String getSection(){
                return section;
        }
        /**
         * Returns whether or not the queue is connected.
         */
        public boolean isConnected(){
                return connected;
        }
        /**
         * Sends a POST to the queue with the parameters nvps, returns the result.
         * @param nvps the NameValuePair of parameters to be sent to the queue server
         * @return ConnectionResult containing the status code for the attempt and the message returned
         * message will be null iff the status is not OK
         */
        private ConnectionResult sendMessage(List<NameValuePair> nvps){
                ConnectionResult res = new ConnectionResult();
                //default to OK, we'll change in the event of an error
                res.status = ConnectionStatus.OK;
                try{
                        //set up the connection
                        DefaultHttpClient client = new DefaultHttpClient();
                        //set up the timeout
                        HttpParams httpParams = client.getParams();
                        HttpConnectionParams.setConnectionTimeout(httpParams,CONNECTION_TIMEOUT);
                        HttpConnectionParams.setSoTimeout(httpParams,CONNECTION_TIMEOUT);

                        HttpPost post = new HttpPost(HOST+PATH);
                        //set up our POST values
                        post.setEntity(new UrlEncodedFormEntity(nvps,HTTP.UTF_8));
                        //send it along
                        ResponseHandler<String> handler = new BasicResponseHandler();
                        connectionWatcher watcher = new connectionWatcher(post);
                        Thread t = new Thread(watcher);
                        t.start();
                        res.message = client.execute(post,handler);
                        watcher.finished();
                        //and clean up
                        client.getConnectionManager().shutdown();
                //if any exceptions are thrown return a connection error result
                }catch(Exception e){
                        res.status = ConnectionStatus.CONNECTION_ERROR;
                        res.message = null;
                }
                return res;
        }
        /**
         * Tells the servlet to remove the student 'name' at machine.
         * Uses remove(String)
         * @param name Name of the student to remove
         * @param machine Machine the student to remove is on
         * @return the connection result containing the status code for sending the message
         * and the message returned from the server(the state of the queue)
         * IMPORTANT: the returned state of the queue will most likely not include the changes just made by this call
         * it is a `feature' in the queue...
         */
        public ConnectionResult remove(String name, String machine){
                return this.remove(name+" @ "+machine);
        }
        /**
         * Tells the servlet to remove toRemove from the queue
         * @param toRemove the entry to remove
         * @return  The connection result containing the status code for sending the message
         * and the message returned from the server(the state of the queue)
         * IMPORTANT: the returned state of the queue will most likely not include the changes just made by this call
         * it is a `feature' in the queue...
         */
        public ConnectionResult remove(String toRemove){
                //first, make sure we have a connection going.
                if(!this.connected){
                        ConnectionResult res = new ConnectionResult();
                        res.status = ConnectionStatus.NOT_CONNECTED;
                        return res;
                }
                //set up our message parameters
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("REQUEST","remove"));
                nvps.add(new BasicNameValuePair("SECTION",this.section));
                nvps.add(new BasicNameValuePair("STUDENT",toRemove));
                //I don't know why this command uses "KEY" instead of "PASSWORD", but that is what KEY is[someone was inconsistent].
                nvps.add(new BasicNameValuePair("KEY",this.password));
                //and return the result
                return this.sendMessage(nvps);
        }

        /**
         * Requests an updated student list from the servlet.
         * @return The connection result containing the status code for the send and a String containing the current queue[TODO: cite the format]
         */
        public ConnectionResult update(){
                //first, make sure we have a connection going.
                if(!this.connected){
                        ConnectionResult res = new ConnectionResult();
                        res.status = ConnectionStatus.NOT_CONNECTED;
                        return res;
                }
                //set up our message parameters
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("REQUEST","update"));
                nvps.add(new BasicNameValuePair("SECTION",this.section));
                //and return the result
                return this.sendMessage(nvps);
        }

        /**
         * Activate the queue
         * IMPORTANT: the returned state of the queue will most likely not include the changes just made by this call
         * it is a `feature' in the queue...
         * @return The connection result containing the status code for sending the message
         * and the message returned from the server(the state of the queue)
         */
        public ConnectionResult activate(){
                return setStatus("active");
        }

        /**
         * Deactivate the queue
         * IMPORTANT: the returned state of the queue will most likely not include the changes just made by this call
         * it is a `feature' in the queue...
         * @return The connection result containing the status code for sending the message
         * and the message returned from the server(the state of the queue)
         */
        public ConnectionResult deactivate(){
                return setStatus("inactive");
        }

        /**
         * Freeze the queue
         * IMPORTANT: the returned state of the queue will most likely not include the changes just made by this call
         * it is a `feature' in the queue...
         * @return The connection result containing the status code for sending the message
         * and the message returned from the server(the state of the queue)
         */
        public ConnectionResult freeze(){
                return setStatus("frozen");
        }

        /**
         * Sets the queue status to newStatus(accept values are "active","inactive","frozen")
         * IMPORTANT: the returned state of the queue will most likely not include the changes just made by this call
         * it is a `feature' in the queue...
         * @return the connection result containing the status code and message returned(queue state)
         */
        private ConnectionResult setStatus(String newStatus){
                //first, make sure we have a connection going.
                if(!this.connected){
                        ConnectionResult res = new ConnectionResult();
                        res.status = ConnectionStatus.NOT_CONNECTED;
                        return res;
                }
                //otherwise activate the queue
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("REQUEST","change"));
                nvps.add(new BasicNameValuePair("STATUS",newStatus));
                nvps.add(new BasicNameValuePair("SECTION",this.section));
                nvps.add(new BasicNameValuePair("PASSWORD",this.password));
                //return the result of sending that message
                return sendMessage(nvps);
        }

        /**
         * Connects to the queue for class section using the password password
         * Saves the values if successful, otherwise the state is not changed
         * @param sec The class to connect to
         * @param password the password for the queue[NOTE: will be sent in the clear]
         * @return The connection status of the operation, OK on success, CONNECTION_ERROR
         * on any connection related issues, BAD_PASSWORD on a bad password, and 
         * SERV_ERROR on unexpected response from the server
         */
        public ConnectionStatus connectTo(String sec, String password){
                //set up our POST values
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("REQUEST","register"));
                nvps.add(new BasicNameValuePair("SECTION",sec));
                nvps.add(new BasicNameValuePair("PASSWORD",password));
                ConnectionResult res = sendMessage(nvps);
                //make sure our message was sent successfully
                if(res.status != ConnectionStatus.OK)
                        return res.status;
                //check if the password is valid
                if(res.message.trim().equals("Incorrect password specified."))
                        return ConnectionStatus.BAD_PASSWORD;
                //if this happens then something odd has came back to us from the server,ah!
                if(!res.message.trim().equals("success")){
                        System.out.println(res.message.trim());
                        return ConnectionStatus.SERV_ERROR;
                }
                this.section = sec;
                this.password = password;
                this.connected = true;
                return ConnectionStatus.OK;
        }
        /**
         * Write the QueueConnectionManager to the output stream out
         */
        private void writeObject(java.io.ObjectOutputStream out) throws IOException{
                out.defaultWriteObject();
        }
        /**
         * Sets the state of the QueueConnectionManager by reading in
         * NOTE: As of now we don't verify that the data we read is sane
         */
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
                in.defaultReadObject();
        }
        private class connectionWatcher implements Runnable
        {
                public boolean isDone;
                private HttpPost message;
                public connectionWatcher(HttpPost toWatch){
                        isDone = false;
                        message = toWatch;
                }
                public void run(){
                        SystemClock.sleep(CONNECTION_TIMEOUT);
                        if(!isDone)
                                message.abort();
                }
                public void finished(){
                        isDone = true;
                }
        }
}
