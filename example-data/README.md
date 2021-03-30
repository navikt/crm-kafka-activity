# Example Data

(WIP)

A Kafka message contains a `key` and a `value`.

## Key

where the `key` is an incremented number starting from 0. The same record from the database can both be key 54 and key 75, because each change made to the record is published to a topic. Thus, that highest `key` for the same record is always the newest. The field `Id` inside the JSON `value` is the unique ID for a record, which can then occur multiple times because the `key` is the unique ID for each changed event.

## Value

Every record in Salesforce contains a unique ID. This ID is presented inside the JSON `value` under `Id`. The rest of the fields in the JSON `value` is then unique for each Salesforce Object (i.e., one Kafka topic for each object).
