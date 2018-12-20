IOT Structured Streaming Proofshop
====================================

This is a guide for how to use the IOT Structured Streaming Proofshop brought to you by the DataStax field team.

*WARNING* Don't try to run this with the default m3.xlarge node type-it won't work!!! Use an m3.2xlarge to be successful.

*WARNING*  There is a bug in Spark that is fixed in 6.0.5.  Before this fix, the spark time window will not produce correct results.  So, wait for 6.0.5 if correct results in the sensor_summary table are important.

### Motivation

The IOT Structured Streaming Proofshop is a targeted event to prove IOT structured streaming capability within DSE and demonstrate that prospect can use this for their own IOT use cases.  


### What is included?

A Powerpoint presentation will walk the prospect through DSE Spark Structured Streaming and the IOT use case.


### Business Take Aways

DataStax enables immediate, real-time IOT alarm, dashboarding, and analysis

DataStax-powered solutions deliver a highly personalized, responsive, and consistent experience whatever the channel, location, or volume of customers and transactions. Customers will have an engaging experience that drives customer satisfaction and advocacy, which translates to increased brand loyalty and revenue growth.

### Technical Take Aways
*WARNING* Don't try to run this with the default m3.xlarge node type-it won't work!!! Use an m3.2xlarge to be successful.

Understand how spark structured streaming allows real-time decision making in an easy-to-create and easy-to-maintain Spark Dataset environment.  Paralleling the transition from RDDs to Datasets, streaming has gone from complex DStreams to easy-to-use Structured Streaming.  The combination of structured streaming analytics with DataStax provides the real-time analytics needed in IOT
