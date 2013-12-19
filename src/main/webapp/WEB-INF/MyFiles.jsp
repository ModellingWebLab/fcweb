<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}My Files - " contextPath="${contextPath}" newExpModelName="${newExpModelName}" newExpProtocolName="${newExpProtocolName}">
    <h1>Your Files</h1>
    
    <button id="modelchooser">models</button>
    <button id="protocolchooser">protocols</button>
    <button id="experimentchooser">experiments</button>
    
    <section id="modellist">
	    <h2>Your Models</h2>
	    <c:if test="${User.allowedCreateEntity}"> 
	    	<small><a href="${contextPath}/model/createnew" id="addmodellink" class="pointer">create a new model</a></small>
		</c:if>
	    <ul>
	    	<c:forEach items="${models}" var="model" >
	    		<li title="${model.name}"><strong><a href="${contextPath}/model/${model.url}/${model.id}/${model.latestVersion.url}/${model.latestVersion.id}">${model.name}</a></strong> 
		    		<c:if test="${User.allowedCreateEntityVersion}">
			    		<small>(<a href="${contextPath}/model/createnew/?newentityname=${model.id}">add new version</a>)</small>
		    		</c:if>
		    	</li>
	    	</c:forEach>
    	</ul>
    </section>
    
    <section id="protocollist">
	    <h2>Your Protocols</h2>
	    <c:if test="${User.allowedCreateEntity}"> 
	    	<small><a href="${contextPath}/protocol/createnew" id="addprotocol" class="pointer">create a new protocol</a></small>
		</c:if>
	    
	    <ul>
	    	<c:forEach items="${protocols}" var="protocol" >
	    		<li title="${protocol.name}"><strong><a href="${contextPath}/protocol/${protocol.url}/${protocol.id}/${protocol.latestVersion.url}/${protocol.latestVersion.id}">${protocol.name}</a></strong> 
		    		<c:if test="${User.allowedCreateEntityVersion}">
			    		<small>(<a href="${contextPath}/protocol/createnew/?newentityname=${protocol.id}">add new version</a>)</small>
		    		</c:if>
		    	</li>
	    	</c:forEach>
    	</ul>
    </section>
    
    <section id="experimentlist">
	    <h2>Your Experiments</h2>
	    
	    <ul>
	    	<c:forEach items="${experiments}" var="experiment" >
	    		<li title="${experiment.name}"><strong><a href="${contextPath}/experiment/${experiment.url}/${experiment.id}/${protocol.latestVersion.url}/${experiment.latestVersion.id}">${experiment.name}</a></strong></li>
	    	</c:forEach>
    	</ul>
    </section>
</t:skeleton>

