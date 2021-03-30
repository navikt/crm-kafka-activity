# Defining a New Topic

To define a new topic that will later be published to, you must first creat a topic in both Salesforce and Kafka

## Creating Push Topics in Salesforce

1. Define new Push Topics in [push-topics](/.topics/push-topics)
1. Push changes to `main` (you can also create topics in Kafka and push at the same time)
1. Verify that [deploy-push-topics.yml](https://github.com/navikt/crm-kafka-activity/actions/workflows/deploy-push-topics.yml) ran successfully

## Creating Topics in Kafka

1. Copy and paste an existing folder in [kafka-topics](/.topics/kafka-topics)
1. Change the topic name inside `topic.yml`
1. Push changes to `main`
1. Verify that [deploy-topics.yml](https://github.com/navikt/crm-kafka-activity/actions/workflows/deploy-topics.yml) ran successfully
1. Verify that the topics are ready in dev/prod clusters
   - `kubectl config use-context dev-gcp && kubectl get topic --namespace=team-dialog`
   - `kubectl config use-context prod-gcp && kubectl get topic --namespace=team-dialog`

### Getting an error on the last commands?

- Make sure [naisdevice](https://doc.nais.io/device/install/) is setup correctly on your computer
- Make sure naisdevice (app) icon is green
- Make sure you're logged into GCP using `glcoud auth login`

# Publishing to a New Topic

See [.nais](/.nais) to start publishing to a topic after successful topic creation.
