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
/**
 * Enum for the result of a QueueConnectionManager request
 * OK means that the command was executed successfully
 * BAD_PASSWORD results from a bad password
 * CONNECTION_ERROR results from any connection issues
 * SERV_ERROR is when the server returns a message we didn't expect
 * NOT_CONNECTED is whenever a method is called that expects to be connected
 * but the ConnectionManager is not yet connected
 */
public enum ConnectionStatus{
        OK,BAD_PASSWORD,CONNECTION_ERROR, SERV_ERROR, NOT_CONNECTED
}
