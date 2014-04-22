# Functional Curation -- Web Interface -- Installation Instructions

The following instructions assume a Debian-based system (we use Ubuntu) using tomcat7.

## Dependencies

* maven
* java 7
* java based webserver (e.g. tomcat)
* mail server on localhost (e.g. postfix)

For the backend:

* All the Chaste dependencies
* python-requests
* Celery (>= 3.1)

## Setup backend

The backend consists of a web service called by the front-end, which manages experiment runs using the Celery task queue.
For the cardiac domain, the experiment execution program is an extension project to Chaste.

### Experiment task queue

Install the dependencies:
```
sudo apt-get install rabbitmq-server
sudo pip install celery
sudo pip install python-requests
```

In the `resources` folder there is a sample init script and configuration file for running Celery on boot.
Copy `resources/celeryd-init` as `/etc/init.d/celeryd`, and `resources/celeryd-default` as `/etc/default/celeryd`.
You'll definitely need to edit the latter file to suit your system.
The server can then be started with `sudo /etc/init.d/celery restart`,
but don't do this until you've finished the web service setup below.

For some additional security, you might want to stop the rabbitmq broker listening on external ports,
although the default account only accepts logins from localhost anyway.
A sample `/etc/rabbitmq/rabbitmq.conf.d/fcws.conf` is:
```
# Settings for Functional Curation Web Service
NODE_IP_ADDRESS=127.0.0.1
```

TODO: configuring multiple queues, so admin/internal users get dedicated workers for their experiments:
-Q arguments, possibly name workers for clarity.

### Web service setup

Setup a webserver/vhost that is able to execute Python CGI scripts.
Copy the contents of `resources/cgi-bin` to the webserver, so that the scripts are executable from the frontend.
(The default configuration in `celeryd-default` assumes these are in `/var/www/cgi-bin`.)
Ensure that the password in the `config.json` file matches the one you specify in `FunctionalCuration.xml` (see below),
and check that the paths etc. there match your system.

There is a sample Apache site configuration file at the end of these instructions.

If the backend is accessed via https and the certificate isn't signed by a standard authority,
you'll need to add it to the Java key store, using a command like:

```
/usr/lib/jvm/java-7-openjdk-amd64/jre/bin/keytool 
    -import -alias [SOME ALIAS]
    -file [CERT]
    -keystore [KEYSTORE]
# keystore is usually $JAVAHOME/jre/lib/security/cacerts
```

### FunctionalCuration executable

These steps need to be performed as the user that celery will run as,
to ensure the correct permissions for running experiments.
Check out Chaste, and check out the FunctionalCuration project within its projects folder.
You should then be able to build the project executable.
For instance, to build using 4 cores:

```
svn co https://chaste.cs.ox.ac.uk/svn/chaste/trunk Chaste
cd projects
svn co https://chaste.cs.ox.ac.uk/svn/chaste/projects/FunctionalCuration
cd ..
scons -j4 b=GccOptNative cl=1 exe=1 projects/FunctionalCuration/apps
```

## Setup front-end

### Setup database

Create a database 'chaste' with associated user account, and populate from `resources/chaste.sql`.
With MySql, suitable commands are:
```
create database chaste;
create user chaste identified by 'password';
grant all on chaste.* to 'chaste'@'localhost' identified by 'password';
flush privileges;
```

### Tomcat configuration
Make sure tomcat is using at least Java version 7. Tomcat's Java home can be configured in `/etc/default/tomcat7`.

The server configuration is in `/etc/tomcat7/server.xml`. Modify this file, so that it includes a line like:

    <Host name="localhost"  appBase="webapps"  deployXML="false" xmlBase="/var/lib/tomcat7/context"
          unpackWARs="true" autoDeploy="true">

Then, context files are stored in `/var/lib/tomcat7/context` and your apps are expected to be in `/var/lib/tomcat7/webapps`.

Copy `resources/FunctionalCuration.xml` to `/var/lib/tomcat7/context` and configure the file properly, including database credentials and link to the backend.

Add jdbc mysql driver to /var/lib/tomcat7/lib. (http://dev.mysql.com/downloads/connector/j/)

### Build project

Add the sems maven repository to your list of repositories: http://sems.uni-rostock.de/2013/10/maven-repository/

and move into project source directory and call

        mvn package

maven will find all dependencies and build a `war` file in `$PROJECTHOME/target/FunctionalCuration.war`

Before the first time you do this, you will need to create a Maven settings file in `$HOME/.m2/settings.xml` to specify where to find some dependencies.
The following example is suitable:

    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
        http://maven.apache.org/xsd/settings-1.0.0.xsd">
        <localRepository/>
        <interactiveMode/>
        <usePluginRegistry/>
        <offline/>
        <pluginGroups/>
        <mirrors/>
        <proxies/>
        <profiles>
            <profile>
                <activation>
                    <activeByDefault>true</activeByDefault>
                </activation>
                <repositories>
                    <repository>
                        <id>sems-maven-repository-releases</id>
                        <name>SEMS Maven Repo</name>
                        <url>http://mvn.sems.uni-rostock.de/releases/</url>
                        <layout>default</layout>
                        <snapshots>
                            <enabled>false</enabled>
                        </snapshots>
                    </repository>
                    <repository>
                        <id>sems-maven-repository-snapshots</id>
                        <name>SEMS Maven Repo</name>
                        <url>http://mvn.sems.uni-rostock.de/snapshots/</url>
                        <layout>default</layout>
                        <releases>
                            <enabled>false</enabled>
                        </releases>
                    </repository>
                </repositories>
            </profile>
        </profiles>
        <activeProfiles/>
    </settings>

### Install project

Copy the produced `war` file to the `/var/lib/tomcat7/webapps` directory on the server running the frontend. Make sure that its name is `FunctionalCuration.war`.
Then, tomcat will unpack this file and setup the web interface properly.

### Test interface

Go to http://server:8080/FunctionalCuration and hopefully you'll see the web interface.

The default admin credentials are:

    user: root
    pass: admin

For security reasons you should change the admin password, register a new user and get the password via mail.
Log in as admin, go to http://server:8080/FunctionalCuration/admin.html and assign the admin role to that new user.
Login as the new user and remove admin permissions of the root-user, or remove root-user completely from the database.

Now you can start uploading models and protocols.

### Integrate tomcat into apache2

Follow for example http://www.dreamchain.com/apache-server-tomcat-mod_jk-on-debian-6-0-squeeze/

A sample vhost configuration might look like:

	<VirtualHost *:80>
		ServerAdmin youradmin@your.company
		ServerName your.company
		
		DocumentRoot /var/www
		
		# backend:
		# setup python handler using mod_python
		ScriptAlias /cgi-bin/ /var/www/cgi-bin/
		<Directory "/var/www/cgi-bin">
			AllowOverride None
			Options +ExecCGI -MultiViews +SymLinksIfOwnerMatch
			Order allow,deny
			Allow from all
			<Files "config.json">
				Order allow,deny
				Deny from all
			</Files>
		</Directory>
		
		# frontend:
		# send requests to /FunctionalCuration* to tomcat
		JkMount /FunctionalCuration* ajp13_worker
	</VirtualHost>

Try to access http://your.company/FunctionalCuration

