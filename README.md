
<p align="center">
    <img src="https://i.imgur.com/GLPqt81.png" alt="drawing" width="300"/>
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![gitter](https://img.shields.io/badge/gitter.im-Join%20Chat-green.svg)](https://gitter.im/divroll/dyno/)
## What is dyno?

Dyno is a key-value datastore library for S3 and it is best paired with [Wasabi Hot Cloud Object Storage](https://wasabi.com/cloud-storage-pricing/#cost-estimates)  
as it [does not charge for egress or API requests.](https://wasabi.com/cloud-storage-pricing/#cost-estimates)

Dyno is very simple to use and does not even need a long documentation. 

Current implementation is for Java or JVM but we plan to 
make a library for other server-side languages like PHP and Node.js (stay tuned!)

**Requirements:** JDK 7, Maven

- [Features](#features)
- [Installation](#installation)
- [How to use](#how-to-use)

Features
---

- It's a super simple library
- Works with every S3 cloud storage (Amazon, Wasabi, Riak CS, etc)

Installation
---

```$xslt
$mvn clean install
```

and add to your project dependency: 

```$xslt
<dependency>
    <groupId>com.divroll</groupId>
    <artifactId>dyno</artifactId>
    <version>0-SNAPSHOT</version>
</dependency>
```

How to use
---

When developing applications using Dyno as the datastore library a different frame of mind 
is required in order to achieve similar results from SQL and NoSQL databases. 

Remember Dyno is just a library and not a server, it is up to the developer to implement distributed caching mechanism to prevent key duplication or similar limitations when using S3 as a datastore.
This limitation does not apply when using Wasabi Hot Cloud Storage since it [employs data consistency model.](https://wasabi-support.zendesk.com/hc/en-us/articles/115001684591-What-data-consistency-model-does-Wasabi-employ-)

Here's a simple example usage of Dyno:

```$xslt
// Setup Dyno
Dyno dyno = DynoClientBuilder
    .simple()
    .withEndpointConfig("s3.wasabi.sys", "us-east-1")
    .withCredentials(ACCESS_KEY, SECRET_KEY)
    .withBucket("dyno")
    .withKeySpace(":")
    .withBufferSize(1024)
    .build();

// Here's a sample way to create a "User" entity with Dyno

// First create an entity with user_id this will prevent creation of another user 
// with the same username

Entity user = EnityBuilder
    .create(dyno)
    .with("username", "dino")
    .with("user_id")
    .build(uuid(), String.class)
    .putIfAbsent();
```

The example code above creates an `Entity` which is a key-value pair with String key `username:dino:user_id` and a String value that is then stored to S3. 
The code below then takes the step further by storing a password and `user_id` is used instead as key to allow users to change username and/or password in the future. 
```$xslt
// Since the username "dino" has been secured we can assign the password simply by 
// puting a new entity with the assigned password:

Key key = EnityBuilder
    .create(dyno)
    .with("user_id", user.getValueString())
    .with("password")
    .build(sha256("the_password"), String.class)
    .putIfAbsent();
	
```

And if you want to store more than just the one value you can do: 
	
```$xslt

User newUser = new UserBuilder()
    .password(sha256("the_password"))
    .firstName("Dino")
    .lastName("Dinosaur")
    .profilePicture(imageBytes)
    .build();

Key key = EnityBuilder
    .create(dyno)
    .with("user_id", user.getValueString())
    .with("user")
    .build(newUser, User.class)
    .putIfAbsent();

// And easily get/retrieve it with:

Entity entity = key.getEntity(User.class);

```

For more examples head over the [tests directory](https://github.com/divroll/dyno/tree/master/src/test/java/com/divroll).


#### What's the motivation behind this library? 

We don't want to worry on scaling any database. 
Using S3 as main application datastore helps us achieve that. 
Also the storage cost difference is huge when using S3 over using a database
which usually is stored in a block storage. 

An example estimate would be $5.99 USD per TB in Wasabi Hot Cloud Storage (S3) 
vs $100.00 per TB in a block storage. 

#### About the logo

We know that a dinosaur has nothing to do with the word "dyno" 
-- well, yes, but it sounds similar ✌