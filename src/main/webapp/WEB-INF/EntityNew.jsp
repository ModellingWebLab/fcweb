<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}Create ${newentitytype} - " contextPath="${contextPath}">
	<div id='newentityform'>
	    <h1>Create ${newentitytype}</h1>
	    <p>
	    	<label for="entityname">Name of the ${fn:toLowerCase(newentitytype)}:</label>
	    	<br/>
			<c:choose>
				<c:when test="${not empty newentityname}">
					<strong>${newentityname}</strong>
					<input type="hidden" name="entityname" id="entityname" value="${newentityname}" placeholder="${newentitytype} Name"/>
				</c:when>
				<c:otherwise>
			    	<input type="text" name="entityname" id="entityname" placeholder="${newentitytype} Name"/>
			    	<span id="entityaction"></span>
				</c:otherwise>
			</c:choose>
	    </p>
	    <p>
	    	<label for="versionname">Version:</label>
	    	<br/>
	    	<input type="text" name="versionname" id="versionname" placeholder="Version Identifier"/>
	    	<a class="pointer" id="dateinserter"><small>use current date</small></a>
	    	<span id="versionaction"></span>
	    </p>
	    <p>
	    	<label for="commitMsg">Commit message:</label>
	    	<br/>
	    	<textarea cols="70" rows="3" name="commitMsg" id="commitMsg" placeholder="optional message to describe this commit"></textarea>
	    	<span id="commitmsgaction"></span>
	    </p>
	    <p>
            <label for="visibility">Visibility:</label>
            <select id="visibility">
                <c:if test="${User.admin}">
                    <option VALUE="MODERATED" id="visibility-MODERATED">MODERATED</option>
                </c:if>
                <option value="PUBLIC" id="visibility-PUBLIC">PUBLIC</option>
                <option value="RESTRICTED" id="visibility-RESTRICTED">RESTRICTED</option>
                <option value="PRIVATE" id="visibility-PRIVATE" selected="selected">PRIVATE</option>
            </select>
            <img src="${contextPath}/res/img/info.png" alt="help" title="Public = anyone can view &#10;Restricted = logged-in users can view &#10;Private = only you can view"/>
            <span id="visibilityaction"></span>
	    </p>
	    <t:upload/>
	    <p id="reRunPara" style="display: none;">
	    	<input type="checkbox" name="reRunExperiments" id="reRunExperiments"/>
	    	<label for="reRunExperiments">rerun experiments involving previous versions of this ${fn:toLowerCase(newentitytype)}</label>
	    	<small>(this might take some time)</small>
	    </p>
	    <p>
	    	<button id="savebutton">Create ${newentitytype}</button>
	    	<span id="saveaction"></span>
	    </p>
    </div>
</t:skeleton>

