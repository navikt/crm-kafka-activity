# Adding Permissions

1. Create a new permission file by copying and pasting an existing file (not from the `main` folder, but from the `objects` folder)
2. Edit/add the following:
   - Add `fieldPermissions` for each field added in your [push topics](/.topics/push-topics)
   - Edit `objectPermissions.object` to the new SObject name
   - Don't edit any permissions, it should always be read-only and `viewAllRecords`
3. Add the permission API name to [main/KafkaActivityDialogMain.permissionsetgroup-meta.xml](/permissions/main/KafkaActivityDialogMain.permissionsetgroup-meta.xml)
