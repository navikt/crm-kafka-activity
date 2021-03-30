# Publishing to Topics

Before the new topic can have data published to it, the topics must be created successfully first (see [.nais](/.nais)). When topics exists both as Push Topics (salesforce) and Kafka Topics (Aiven), you can continue here.

## What Happens

1. These configuration files will make a new instance of the existing app/code/Docker image be deployed. The same code/image runs for all configuration files, but separate instances so that each instance manages their own topic publication.
1. The configuration files also decides which Salesforce Push Topic should publish its designated data to which Kafka topic.

## Define nais Configurations

1. Create (or copy & paste) a folder for the new topic (folder name doesn't matter, but the same name as the topic is preferred)
1. Make sure the folder contains `dev.yml` or `prod.yml` (or either of them for testing purposes)
1. Edit these values inside `dev.yml` and `prod.yml`
   - `KAFKA_TOPIC`
     - [.topics/kafka-topics](/.topics/push-topics) → [TopicFolder] → `topic.yml` → `metadata.name`)
   - `EMP_URL`
     - Either `https://salesforce.com` or `https://test.salesforce.com`
   - `EMP_TOPIC`
     - [.topics/push-topics](/.topics/push-topics) → [PushTopicFile] → `PUSH_TOPIC_NAME`
