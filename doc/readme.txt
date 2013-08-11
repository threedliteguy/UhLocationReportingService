This app requires the UserHash servlet to run at the "userhash server url" in the app settings.

It is convenient to run nginx as the front facing web server proxying requests to tomcat in the background.

For example if you have a small 512k VPS with a Ubuntu image, you can install the following:

apt-get install nginx
apt-get install tomcat7
apt-get install default-jdk

For SSL support (recommended), install a self-signed cert in nginx 
and add users/passwords in /etc/nginx/htpassword

The file nginx-site contains sample settings for nginx with SSL.

Also modify the tomcat's server.xml to add address="127.0.0.1", localhost only.

 <Connector 
     port="8080" 
     protocol="HTTP/1.1" 
     address="127.0.0.1" 
     maxHttpHeaderSize="8192"
     connectionTimeout="20000"
     URIEncoding="UTF-8"
     redirectPort="443" />

