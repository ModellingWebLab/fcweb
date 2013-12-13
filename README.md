# FunctionalCuration -- WebInterface

## Install

following is about debian based systems using tomcat7

### Dependencies

* maven
* java
* java based webserver (e.g. tomcat)
* tbc

### Setup database
create database from resources/chaste.sql

create user having all permissions on that database.

### setup backend

setup webserver/vhost that is able to execute python scripts. copy `resources/cgi-bin/*py` to the webserver, so that it is executable from the frontend.

### Tomcat configuration
make sure tomcat is using at least java in version 7. it can be configured in /etc/default/tomcat7

add jdbc mysql driver to /var/lib/tomcat7/lib.

Server configuration is in `/etc/tomcat7/server.xml`. modify file, so that it includes a line like:

    <Host name="localhost"  appBase="webapps"  deployXML="false" xmlBase="/var/lib/tomcat7/context"
          unpackWARs="true" autoDeploy="true">

then, context files are stored in `/var/lib/tomcat7/context` and your apps are expected to be in `/var/lib/tomcat7/webapps`.

copy `resources/FunctionalCuration.xml` to `/var/lib/tomcat7/context` and configure the file properly, including database credentials and link to the backend.

### build project
add the sems maven repository to your list of repositories: http://sems.uni-rostock.de/2013/10/maven-repository/

just move into project source directory and call

    mvn package

maven will find all dependencies and build a `war` file in `$PROJECTHOME/target/FunctionalCuration.war`


### install project

copy the produced `war` file to the `/var/lib/tomcat7/webapps` directory on the server running the frontend. make sure that it's name is `FunctionalCuration.war`.
then, tomcat will unpack this file and setup the web interface properly

### test interface

go to http://server:8080/FunctionalCuration and hopefully you'll see the web interface.

default admin credentials are:

    user: root
    pass: admin

for security reasons you should change admins password. register a new user and get the password via mail.
log in as admin. go to http://server:8080/FunctionalCuration/admin.html and assign the admin role to that new user.
login as new user and remove admin permissions of the root-user, or remove root-user completly from database

start uploading models/protocols

### integrate tomcat into apache2
follow for example http://www.dreamchain.com/apache-server-tomcat-mod_jk-on-debian-6-0-squeeze/

a sample vhost configuration might look like:

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
				</Directory>
				
				# frontend:
				# send requests to /FunctionalCuration* to tomcat
				JkMount /FunctionalCuration* ajp13_worker
	</VirtualHost>


try to access http://your.company/FunctionalCuration




