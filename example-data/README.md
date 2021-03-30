# Example Data

(WIP)

A Kafka message contains a `key` and a `value`.

## Key

The `key` field is an incremented number starting from 0. The number is unique for each Kafkas topic. A given Salesforce record can have its `key` field to be both 54 and 75, because each change made to the record is published to a topic. Thus, that highest `key` field for the same record is the newest and identical to how the records is viewed inside Salesforce. The JSON field `Id` inside the JSON `value` field is the unique ID for a record, which can then occur multiple times because the `key` is the unique ID for each changed event.

## Value

The `value` field is a JSON object. It will mostly be consisting of fields at the root level (see example values above). The JSON object always contains the field `Id`. The rest of the fields in the JSON object are unique for each Salesforce Object. Each Kafka topic will contain the same JSON model for all data. However, some fields might be empty or null, depending on the data inside Salesforce.
