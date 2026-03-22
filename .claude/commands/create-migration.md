Ask the user: "What should the migration be named? (e.g. `add_card_tags`, `create_decks_table`)"

Wait for the user's answer. Use the provided name as `$MIGRATION_NAME`.

Then run:
```
./gradlew :$MODULE:createMigration -PsqlName=$MIGRATION_NAME
```

Before running, ask which module to target if it is not clear from context — valid options are `collection-manager` and `wizard-stat-aggregator`.

After the command succeeds, tell the user the path of the created migration file and remind them to write their SQL in the `-- TODO: write your migration here` section and the rollback in `-- TODO: write your rollback here`.
