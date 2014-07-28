# Documentation for developers

This file contains some useful reference notes for developers working on this website.

## Database auto-update

Occasionally new features require updating the database structure.
Since nobody wants to update the database of every instance manually,
there is an update functionality in the [DatabaseConnector](https://bitbucket.org/joncooper/fcweb/commits/7792742317fd0e0e775b26599fa1f5cb9ac23c2c#Lsrc/main/java/uk/ac/ox/cs/chaste/fc/mgmt/DatabaseConnector.javaT122)
(see commit 7792742).
This function will identify the database version as an integer:

* `1`, if there is no settings table yet
* `select val from settings where user=-1 and key=DBVERSION`, otherwise

It then compares this with the value of static field `DB_VERSION` defined in the [DatabaseConnector](https://bitbucket.org/joncooper/fcweb/commits/7792742317fd0e0e775b26599fa1f5cb9ac23c2c#Lsrc/main/java/uk/ac/ox/cs/chaste/fc/mgmt/DatabaseConnector.javaF20T20),
and missing updates to the database are applied until it is up to date.
Note that, for convenience, the version needs to be an `int`, there are no minor versions.

Therefore, if you need to modify the database in the future you may want to add some code there ;-)

## Settings API

On the client side you can access the user's preferences using `preferences["key"]` from your JavaScript.
You can set preferences by sending a JSON object such as the following to `contextPath + "/myaccount.html"`:

```
#!js
{
    task: "updatePref",
    prefKey: "someKey",
    prefVal: "someValue"
}
```

To set/get preferences on the server side use the following methods in `User.java`:

* `getPreference (String key, String defaultValue)`
* `setPreference (String key, String value)`

The settings table is structured as:

* `user`=>`int(11)`;
* `key`=>`varchar(20)`;
* `val`=>`varchar(100)`

Hopefully 20 chars are sufficient to name a unique key.

If you need to store some other global settings you may also use the settings table,
simply use a negative user-id (the `DBVERSION` above is stored as user `-1`).
