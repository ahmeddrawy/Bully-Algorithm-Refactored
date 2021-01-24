# Bully Algorithm
This is my implementation for [Bully Algorithm](https://en.wikipedia.org/wiki/Bully_algorithm) used in **Distributed computing**
### Brief Description
Using peer to peer architecture and java socket programming, Simulating bully algorithm using interprocess communication without using threads or interprocess communication libraries

Multiple instances of your application should run (different processes) and communicate with each other


### Built using Peer to Peer Architecture
#### New Process enters the system 
1. check if there's no coordinator in coordinator port
   1. if there's no coordinator, then current process declares that it's the coordinator
2. if there's a coordinator, process sends a message declaring itself,
3. Coordinator assigns the new Process a port to listen to
4. Coordinator sends a list of alive processes in system to the new process
5. Coordinator sends the new process port to the other processes in system

####  Coordinator fails
 Coordinator sends an alive message for all alive processes each period of time, if any process found that a coordinator failed  sends an election message to other processes

#### Winning the Elections
Eventually the process with the highest priority - port in our case - wins an election.
The winning process will notify all the other running processes that it is the coordinator.


### Main Classes
1. Peer
   1. Bind process to the specified port
   2. send and receive messages 

2.Process
   1. Has a peer to send and receive messages
   2. Decode messages to take actions based on bully algorithms, previously explained
   3. Notify other processes
   4. Has list of peers of other processes in systems to interact with 
1. Message
   1. a utility class to encapsulate message details
   2. Has an enum for message type
2.Peer
### Built with
- Java socket programming

#### Author
- [ahmeddrawy](https://github.com/ahmeddrawy)

