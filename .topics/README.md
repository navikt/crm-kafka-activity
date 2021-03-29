# Defining a New Topic

To define a new topic that will later be published to, you must first creat a topic in both Salesforce and Aiven

## Creating Push Topics in Salesforce

1. Define new Push Topics in [push-topics](/.topics/push-topics)
1. Push changes
1. Publish Push Topics by running Apex code in both Salesforce [production](https://navdialog.lightning.force.com) and [preproduction](https://navdialog--preprod.lightning.force.com)

## Creating Topics in Aiven

1. Copy and paste an existing folder in [kafka-topics](/.topics/kafka-topics)
1. Change the topic name inside `topic.yml`
1. Push changes
1. Verify that [deploy-topics.yml](https://github.com/navikt/crm-kafka-activity/actions/workflows/deploy-topics.yml) ran successfully
1. Verify that the topics are ready in dev/prod clusters

   - `kubectl config use-context dev-gcp && kubectl get topic --namespace=team-dialog`
   - `kubectl config use-context prod-gcp && kubectl get topic --namespace=team-dialog`

   - Make sure [naisdevice](https://doc.nais.io/device/install/) is setup correctly on your computer
   - Make sure naisdevice icon is green
   - Make sure you're in the correct context using `kubectl config use-context [ENV]`, with env `dev-gcp` or `prod-gcp`
   - Make sure you're logged into Google Cloud locally using `gcloud auth login`
   - Run `kubectl apply -f .kafka-topics/topic.yml` for each new topic

# Publishing to a New Topic

Before the new topic can have data published to it, the topics must be created successfully first (see [Defining a New Topic](#defining-a-new-topic)). Then do the following:

1. Add a new config file in [.nais](/.nais) for either `prod` or `dev`, or both
1. Edit [deploy-nais.yml](/.github/.workflows/deploy-nais.yml)
   - Add the new nais config file name to the `namespace` array in either `deploy-dev` or `deploy-prod`, or both
   - Make sure to **not** include `-dev` and `-prod` in the `namespace` array, just the base name
