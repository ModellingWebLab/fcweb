# Documentation for developers

This file contains some useful reference notes for developers working on this website.

## Upgrading the system

To some extent the required upgrade steps will depend on exactly what has changed.
However in general there are 4 components that need to be updated, and updating in the following order is safest.

1.  The back-end `FunctionalCuration` executable.
    Since the interface to this doesn't tend to change, it can usually be updated independently.
    A potential gotcha is needing to change `config.json` (usually in `/var/www/cgi-bin/fcws`) if the environment variables need to change
    (e.g. due to a different Chaste build type being used).
2.  The back-end celery task queue that the web service calls.
    You will need to copy manually any modified files from `resources/cgi-bin`, making sure not to overwrite the local `config.json`.
    Be aware of the need to change `/etc/default/celeryd` first if any changes to the queues workers should process are made.
3.  The back-end web service.
    This should normally be done together with updating celery, although for some changes (that don't touch `__init__.py`) you can get away with skipping it.
4.  The front-end website.
    Replacing the .war file takes care of everything, including database updates (see the next section).

Sample update commands are as follows; adapt as necessary for your installation.

```
# 1 - back-end exe
cd eclipse/workspace/Chaste
svn up . projects/FunctionalCuration
scons -j4 b=GccOpt cl=1 exe=1 projects/FunctionalCuration/apps
# 2 - celery
sudo /etc/init.d/celeryd restart
# 3 - apache
sudo /etc/init.d/apache2 graceful
# 4 - website
mvn package && sudo cp target/FunctionalCuration.war /var/lib/tomcat7/webapps/
```

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
