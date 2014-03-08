<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}Register - " contextPath="${contextPath}">
    <h1>Register for this Functional Curation system</h1>
    
    <p>
        Only users with an account are able to upload their own models and/or protocols for analysis.
        Once you have registered, you will need to wait for an admin to approve the account before you are able to upload files.
    </p>
    
      <section id="registerform">
    	<p>
	    	<label for="mail">E-mail Address* <small>(for sending you a password, and for meta data of your files)</small></label>
	    	<br/>
	    	<input type="email" name="mail" id="mail" placeholder="your email address" /> <span id="mailaction"></span>
    	</p>
    	
    	<p>
	    	<label for="nick">Username* <small>(will be displayed as the owner of your files, and must be unique)</small></label>
	    	<br/>
	    	<input type="text" name="nick" id="nick" placeholder="your alias goes here" /> <span id="nickaction"></span>
    	</p>
    	
    	<p>
	    	<label for="givenName">Given Name <small>(for meta data of your files)</small></label>
	    	<br/>
	    	<input type="text" name="givenName" id="givenName" placeholder="your given name" />
    	</p>
    	
    	<p>
	    	<label for="familyName">Family Name <small>(for meta data of your files)</small></label>
	    	<br/>
	    	<input type="text" name="familyName" id="familyName" placeholder="your family name" />
    	</p>
    	
    	<p>
	    	<label for="institution">Institution <small>(just for our records)</small></label>
	    	<br/>
	    	<input type="text" name="institution" id="institution" placeholder="your institution" />
    	</p>
    	
    	<p>
    		<button id="registersubmit">Register</button> <span id="submitaction"></span>
    	</p>
      </section>
</t:skeleton>


