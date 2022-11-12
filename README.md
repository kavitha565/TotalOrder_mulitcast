# Totally ordered multicasting using Lamport’s algorithm
    Inorder to implement this we have created three processes representing three nodes of Distributed system

# Files
    Process1.java consists of code of Process1 node of distributed system
    Process2.java consists of code of Process2 node of distributed system
    Process3.java consists of code of Process3 node of distributed system
    Event.java consists of Event class with getters and setter methods to store process and event details like Process id, Event id, Process number, Acknowledgement status
    Run.bat file consists commands to runs the three processes parallelly in Windows
    Run.sh file consists commands to runs the three processes parallelly om Linux

# Prerequisites
    Java

# Steps to be followed for execution
    1) To compile the programs open the terminal in the directory where the files reside and run the following command

        javac Process1.java Process2.java Process3.java

        The above command generate corresponding .class files of  files respectively

    2) Finally to execute the programs open the Run.bat file in Windows and Run.sh file in Linux

    3) This opens up three processes in three different terminals and displays events along with process id in same order.