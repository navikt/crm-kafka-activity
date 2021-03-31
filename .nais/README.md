# Publishing to Topics

Before the new topic can have data published to it, the topics must be created successfully first (see [.nais](/.nais)). When topics exists both as Push Topics (salesforce) and Kafka Topics (Aiven), you can continue here.

## What Happens

1. These configuration files will make a new instance of the existing app/code/Docker image be deployed. The same code/image runs for all configuration files, but separate instances so that each instance manages their own topic publication.
1. The configuration files also decides which Salesforce Push Topic should publish its designated data to which Kafka topic.

## Define nais Configurations

1. Copy and paste a folder for the new topic
1. Make sure the folder contains `dev.yml` and `prod.yml` (or either of them for testing purposes)
1. Edit these values inside `dev.yml` and `prod.yml`
   - `metadata`.`name`
     - Same as [.topics/kafka-topics](/.topics/push-topics) → [TopicFolder] → `topic.yml` → `metadata.name`
   - `KAFKA_CLIENTID`
     - Same as bove
   - `KAFKA_TOPIC`
     - Namespace + Same as bove
   - `EMP_ENV`
     - Either `prod` or `dev`
   - `EMP_TOPIC`
     - [.topics/push-topics](/.topics/push-topics) → [PushTopicFile] → `PUSH_TOPIC_NAME`
1. Push changes to `main`
1. Verify that [deploy-nais.yml](https://github.com/navikt/crm-kafka-activity/actions/workflows/deploy-nais.yml) ran successfully
1. Verify that the topics are ready in dev/prod clusters
   - `kubectl config use-context dev-gcp && kubectl get pods --namespace=team-dialog`
   - `kubectl config use-context prod-gcp && kubectl get pods --namespace=team-dialog`

### Getting an error on the last commands?

- Make sure [naisdevice](https://doc.nais.io/device/install/) is setup correctly on your computer
- Make sure naisdevice (app) icon is green
- Make sure you're logged into GCP using `glcoud auth login`
