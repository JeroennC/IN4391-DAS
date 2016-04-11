# IN4391-DAS
An implementation of a distributed game server for the DAS (Dragons Arena System).
Created by Koos van der Linden & Jeroen Castelein

## System design
The servers are fully connected maintaining a consistent game state, and serving many clients together.
Core layer implemented using asynchronous RMI, passing along messages to communicate.

## How to run
Launch the Main class with VM argument `-Djava.security.policy=my.policy`.
You can then
- Start a server with `start Server [id]`
- Start a server view with `sview [serverId]`
- Start a client with `start Client [id]`
- Start a client with view with `cview [serverId]`
- Type `exit` to kill all running servers and clients