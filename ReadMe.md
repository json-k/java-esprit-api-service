# Esprit API Service

## Intro

This is a service abstraction of the [Dalim](https://dalim.com) [Esprit](https://www.dalim.com/en/products/es-enterprise-solutions/) API.

## Philosophy

Is there a point to abstracting an existing API? Whilst problems to not get less complex by adding code (in fact it is often the opposite) we can sometimes prevent the writing of complex code and solution in many places by taking on the problem in a single place.

I was also interested in how quickly I might be able to write this (because it's fun (update: it wasn't)).

## Build

This is a Gradle build that creates a service running Embedded Tomcat via the Java Service Wrapper.

##### Tasks

* **serviceRun**		Builds and runs the service (just like production).
* **serviceUpdate**		Updates the service to the latest code (it should reload).
* **serviceStop**		Stop it when running in the IDE
* **serviceDist**		Package everything up in zip ready for deployment

## Installation

Once you have a dist package built it can be installed via:

1. Transfer the dist package to the install machine.
2. Set the target host, username, and password in the service.json file.
3. Run espritAPI/bin/espritAPI.sh to see the program arguments (it can be installed with that command on Linux). Use the bat files to install on Windows.

## Functions

So far the service provides on endpoint:

```
	http://server:8181/espritapi/rest/customers/{customerId}/jobs/{jobId}/documents/{docId}/(thumbnail|preview|file)
```

It allows the browsing or customers, jobs, and documents via their ID. The service will log in and out of Esprit as needed to serve clients.
