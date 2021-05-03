# Consume Data from Topics

To consume data from any of the Kafka streams, the NAIS application must be added to the topic definition file.

There will be a folder for each topic inside [.topics/kafka-topics/](/.topics/kafka-topics). To add an application to some or all topics, edit `topic.yml` for the required topic. Under `acl`, add your namespace, application name and <ins>read-only</ins> access. The format should be like this:

```yaml
acl:
  # this is the producer NAIS app
  - team: team-dialog
    application: crm-kafka-activity-oppgaver-v2
    access: readwrite
  # this is the consumer NAIS app (i.e., the new app to add)
  - team: team-something
    application: something-else
    access: read
```

# Defining a New Topic

To define a new topic that will later be published to, you must first creat a topic in both Salesforce and Kafka

## Creating Push Topics in Salesforce

1. Duplicate `template.cls` in [push-topics](/.topics/push-topics) and give it a new name
1. **Important!** Commit (but don't push) the default template (with **<ins>no</ins>** changes inside)
1. Modify the following fields
   - `PUSH_TOPIC_NAME`
     - Is later used as `EMP_TOPIC` in [.nais](/.nais) config
   - `SOBJECT_NAME`
     - SObject API Name
   - `FIELDS`
     - List of fields to query
   - `PARAMETERS`
     - What should appear after a `WHERE` clause in a SOQL Query
1. Push changes to your branch
1. Verify that [deploy-push-topics.yml](https://github.com/navikt/crm-kafka-activity/actions/workflows/deploy-push-topics.yml) ran successfully

## Creating Topics in Kafka

1. Duplicate `template` folder in [kafka-topics](/.topics/kafka-topics) and give it a new name
1. **Important!** Commit (but don't push) the default template (with **<ins>no</ins>** changes inside)
1. Change the following inside `topic.yml`
   - `metadata`.`name`
     - Kafka Topic name used in other apps
   - `acl`.`application`
     - Should be same as above, so that the accompanying app matches the Kafka Topics
1. Push changes to your branch
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
