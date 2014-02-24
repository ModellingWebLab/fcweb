<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}My Account - " contextPath="${contextPath}">
    <h1>Your account</h1>
    <table id="myaccounttable" class="leftright">
    	<tr>
    		<th>E-mail address</th>
    		<td>${User.mail}</td>
    	</tr>
    	<tr>
    		<th>Nickname</th>
    		<td>${User.nick}</td>
    	</tr>
    	<tr>
    		<th>Institution</th>
    		<td>
    		    <input type="text" id="instituteChanger" value="${User.institution}"/>
    		    <span id="instituteChangeaction"></span>
    		</td>
    	</tr>
    	<tr>
    		<th>Permissions</th>
    		<td>
    		    <c:if test="${User.role == 'ADMIN'}">
                    Administrator: you have full administrative privileges.
                </c:if>
                <c:if test="${User.role == 'GUEST'}">
                    Guest: to upload new models please <a href="${contextPath}/contact.html">contact us</a>.
                </c:if>
                <c:if test="${User.role == 'MODELER'}">
                    Modeller: you may add your models to the system; to upload your own protocols please <a href="${contextPath}/contact.html">contact us</a>.
                </c:if>
                <c:if test="${User.role == 'PROTO_AUTHOR'}">
                    Advanced modeller: you may upload both models and protocols.
                </c:if>
    		</td>
    	</tr>
    	<tr>
    		<th>Registered since</th>
    		<td><time>${User.created}</time></td>
    	</tr>
    	<tr>
    		<th>E-mail preferences</th>
    		<td><input type="checkbox" id="sendMailsCheckbox" <c:if test="${User.sendMails}">checked="checked"</c:if> > Inform me about finished experiments. <span id="sendMailsChangeaction"></span></td>
    	</tr>
    </table>
    
    <h2>Change your password</h2>
    <p>
    	<label for="oldpassword">Old password:</label><br/>
    	<input type="password" id="oldpassword" placeholder="old password"/>
    </p>
    <p>
    	<label for="newpassword1">New password:</label><br/>
    	<input type="password" id="newpassword1" placeholder="new password"/>
    </p>
    <p>
    	<label for="newpassword2">Please repeat the new password:</label><br/>
    	<input type="password" id="newpassword2" placeholder="repeat password"/>
    </p>
    <p>
    	<button id="pwchanger">Change Password</button>
    	 <span id="changeaction"></span>
    </p>
</t:skeleton>

