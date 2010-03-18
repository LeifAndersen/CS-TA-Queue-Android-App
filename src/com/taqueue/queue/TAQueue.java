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
package com.taqueue.queue;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
/**
 * Class that handles the state of the TA queue
 */
public class TAQueue implements java.io.Serializable{
        /**
         * The state of the queue at this time
         * @serial
         */
        private QueueState state;

        /**
         * The section the queue is listening to
         * @serial
         */
        private String section;
        
        /**
         * List of all the students, will be reset everytime an update happens
         * @serial
         */
        private ArrayList<Student> students;

        public TAQueue(){
                //setup defaults
                this.section = "None";
                this.state = QueueState.STATE_UNCONNECTED;
                students = new ArrayList<Student>();
        }
        public void setSection(String newSection){
                this.section = newSection;
        }
        /**
         * Parses the message from the server and updates the queue state as needed
         * @param message message from the server
         */
        public void parseUpdate(String message){
                //messages are using \r\n, so split those away
                String[] lines = message.split("\r\n");
                //first line is the queue state
                String state = lines[0].trim();
                if(state.equals("The queue is active"))
                        this.state = QueueState.STATE_ACTIVE;
                else if(state.equals("The queue is inactive"))
                        this.state = QueueState.STATE_INACTIVE;
                else if(state.equals("The queue is frozen"))
                        this.state = QueueState.STATE_FROZEN;
                else //this shouldn't happen, but just incase
                        this.state = QueueState.STATE_UNCONNECTED;
                //and now re load the students list
                students = new ArrayList<Student>();
                for(int i=1;i<lines.length;i++){
                        //split on '@' to get the name and machine
                        //if there are more than 1 @ we assume it is part of the machine
                        //there is no way to choose(because of the way the queue works)
                        //name is in [0], machine in [1]
                        String[] split = lines[i].split(" @ ",2);
                        students.add(new Student(split[0],split[1]));
                }
        }
        /**
         * Get the list of students in the queue
         * @return list of all students currently in the queue
         */
        public List<Student> getStudents(){
                return this.students;
        }

        /**
         * Get the current state of the queue
         * @return queue state
         */
        public QueueState getState(){
                return this.state;
        }
        
        /**
         * Get the the queues section
         * @return Section name
         */
        public String getSection(){
                return this.section;
        }
        /*Serializable methods*/
        private void writeObject(java.io.ObjectOutputStream out) throws IOException{
                out.defaultWriteObject();
        }
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
                in.defaultReadObject();
        }
}
