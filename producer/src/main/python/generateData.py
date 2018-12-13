#!/usr/bin/python
#  import necessary packages 
from kafka import KafkaProducer
import datetime
import random
import socket
import csv
import time

# connect to socket to as a server 
producer = KafkaProducer(bootstrap_servers='node0:9092')
#  keep this open until killed
while 1:
	#  open csv file holding sensors metadata 
	filename = "producer/data/hive_sensorsClean5.csv"
	f = open(filename,'r') 
	#  get current time
	current_time = str(datetime.datetime.now() )
        cntr = 0
	edge_id = "Edge 1"
	#   use the csv reader to parse the csv file
	csv_f = csv.reader(f)
	#    loop through all the sensors in the file
	for nextSensor in csv_f:
	#  generate random movement number
		cntr += 1
		if cntr > 1:
			depth = random.randint(1, 100) + random.random()
	#   generate random humidity set
			value = random.randint(1, 100) + random.random()
	#   sensor serial number is first column in the csv
			serial_number = nextSensor[0]
	#   send to the link
	#	print 'this is serial %s' % (serial_number)
			print_string =  '%s;%s;%s;%s;%s;%s\n' % (edge_id, serial_number,value,current_time,depth,value)
			print (print_string)
        		producer.send("stream_ts", print_string)
        		producer.flush()
	time.sleep(10)
# close the client socket
