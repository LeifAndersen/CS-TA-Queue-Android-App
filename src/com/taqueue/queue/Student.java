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
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
/**
 * Class representing a student
 */
public class Student implements java.io.Serializable{
	/**
	 * Student name
	 * @serial
	 */
	private String name;
	/**
	 * Student machine
	 * @serial
	 */
	private String machine;
	/**
	 * Student Constructor
	 * @param name Name of the student
	 * @param machine machine the student is on
	 */
	public Student(String name, String machine){
		this.name = name;
		this.machine = machine;
	}

	/**
	 * Get the student name
	 * @return Name of the student
	 */
	public String getName(){
		return name;
	}

	/**
	 * Get the machine the student is on
	 * @return Machine the student is at
	 */
	public String getMachine(){
		return machine;
	}
	/*Serializable methods*/
	private void writeObject(java.io.ObjectOutputStream out) throws IOException{
		out.defaultWriteObject();
	}
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
		in.defaultReadObject();
	}
}
