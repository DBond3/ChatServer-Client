***README***

Bond, Dennis

ECEN/CS 4283 Project


Compiled and run on:
	Mac OS Sierra: version 10.12.3 , Java version 1.8.0_71
	Windows 10, Java Version 8 update 131


Server.java
------------------
compile: javac Server.java
usage: java Server


Client.java
------------------
compile: javac Client.java
usage: java Client

	ip: local ip address of the server

	Pick a user name

Features:

		Messages to all
		======================
		Just type them into the bar and hit enter key

		Direct Messages
		======================
		@USERNAME Message here

		Exclusion Messages
		======================
		!USERNAME Message here

		File Transfer
		=============
		Supports any file type

			File Transfer to all
			======================
			&
				-then select file to send


			Direct File Transfer
			======================
			~USERNAME
				-then select the file to send



Known Bugs:

	After user sends file, cannot send another one. JFilechooser will not open again. Known issue with JFilechooser itself, would fix by not using this method to select files.


