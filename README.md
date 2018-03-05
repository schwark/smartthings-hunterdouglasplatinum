# Hunter Douglas Platinum Gateway Integration with SmartThings Hub

Note: Only been tested on Hub v2 - don't know if works the same on Hub v1

Since I could not figure if and how the Platinum gateway responds to SSDP requests, I have created a preference for the gateway IP that you need to manually enter. The Platinum Gateway app shows you the IP that it is connected to that you need to find and manually enter.

Since I could not get SmartThings hub to return the result of a TCP query on a local network, you have to telnet or netcat or run the python script included, to your gateway to get one file and put it up on a web accessible page and enter that URL in as a Status URL. You can use pastebin as a location or your dropbox. But if you use pastebin, please make sure to use the "raw" page link.

netcat:

	nc -i3 <ip-address-of-gateway> 522 < input.txt > output.txt

telnet on Windows:

	telnet -f output.txt <ip-address-of-gateway> 522 < input.txt

python:
	
	python getstatus.py <ip-address-of-gateway> > output.txt


* Github integration (first need to enable [github integration](http://docs.smartthings.com/en/latest/tools-and-ide/github-integration.html))
```
Click on My SmartApps/Settings/Add new repository
Owner: schwark, Name: smartthings-hunterdouglasplatinum, Branch: master
Save
Update from Repo / smartthings-hunterdouglasplatinum (master)
Check all the ones under New and Obsolete
Check Publish checkbox at the bottom (next the Cancel button)
Click Execute update
```


