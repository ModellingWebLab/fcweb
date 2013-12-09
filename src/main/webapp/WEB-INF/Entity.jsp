<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}${entity.type} - " contextPath="${contextPath}" newExpModelName="${newExpModelName}" newExpProtocolName="${newExpProtocolName}">
    <h1 id="entityname">${entity.name}</h1>
	<div class="suppl"><small>${entity.type} created</small> <time>${entity.created}</time> <small>by</small> <em>${entity.author}</em>
	    <c:if test="${entity.author == User.nick || User.admin}">
	    	- delete entity: <a id='deleteEntity'><img src="${contextPath}/res/img/delete.png" alt="delete entity" title="delete entity" /></a>
	    </c:if>
		<c:if test="${User.allowedToForceNewExperiment && entity.type == 'experiment'}">
			<a id="rerunExperiment"><img src="${contextPath}/res/img/refresh.png" alt="rerun experiment" title="rerun experiment"/></a> <span id="rerunExperimentAction"></span>
		</c:if>
	</div>
	
    
    <div id="entitydetails">
    	<h2>Versions</h2>
    	<div id="entityversionlist">
    		<c:if test="${entity.type == 'experiment'}">
    			<p id='expversioncolorlegend'>
    				Status Legend:
    				<span class="experiment-RUNNING">running</span>
    				<span class="experiment-INAPPRORIATE">inappropriate</span>
    				<span class="experiment-FAILED">failed</span>
    				<span class="experiment-SUCCESS">success</span>
    			</p>
    		</c:if>
	   		<c:forEach items="${entity.versions}" var="version" >
	    		<p title="${version.value.created} -- Visibility: ${version.value.visibility}<c:if test="${entity.type == 'experiment'}"> -- ${version.value.status}</c:if>" class="entityviz-${version.value.visibility}<c:if test="${entity.type == 'experiment'}"> experiment-${version.value.status}</c:if>">
					<c:choose>
						<c:when test="${entity.type == 'experiment' && version.value.status != 'SUCCESS'}">
							<strong>
							${version.value.version}</strong> by <em>${version.value.author}</em> - ${version.value.returnText}
							<c:if test="${entity.author == User.nick || User.admin}">
						    	<a id='deleteVersion'><img src="${contextPath}/res/img/failed.png" alt="delete version" title="delete version" /></a>
						    </c:if><br/>
	    					<span class="suppl"><small>created </small> <time>${version.value.created}</time> <small>containing</small> ${version.value.numFiles} File<c:if test="${version.value.numFiles!=1}">s</c:if>.</span>
						</c:when>
						<c:otherwise>
							<strong>
							<a class="entityversionlink" href="${contextPath}/${entity.type}/${entity.url}/${entity.id}/${version.value.url}/${version.value.id}/">
							${version.value.version}</a></strong> by <em>${version.value.author}
							<c:if test="${entity.author == User.nick || User.admin}">
						    	<a id='deleteVersion-${version.value.id}' class="deleteVersionLink"><img src="${contextPath}/res/img/delete.png" alt="delete version" title="delete version" /></a>
						    </c:if></em><br/>
	    					<span class="suppl"><small>created </small> <time>${version.value.created}</time> <small>containing</small> ${version.value.numFiles} File<c:if test="${version.value.numFiles!=1}">s</c:if>.</span>
						</c:otherwise>
					</c:choose>
	    		</p>
	   		</c:forEach>
   		</div>
    </div>
    
    <div id="entityversion">
    	<div class="closebtn"><small><a id="entityversionclose">&otimes; close / see  version table</a></small></div>
    	<h2 id="entityversionname"></h2>
	    <div class="suppl"><small>Version created</small> <time id="entityversiontime"></time> <small>by</small> <em id="entityversionauthor"></em>.
		    <c:if test="${entity.author == User.nick}">
		    	Change visibility: 
		    	<select id="versionVisibility">
			    	<option value="PUBLIC" id="visibility-PUBLIC">PUBLIC</option>
			    	<option value="RESTRICTED" id="visibility-RESTRICTED">RESTRICTED</option>
			    	<option value="PRIVATE" id="visibility-PRIVATE">PRIVATE</option>
		    	</select>
		    	<span id="versionVisibilityAction"></span>
		    </c:if>
		    <c:if test="${entity.author == User.nick || User.admin}">
		    	delete version: <a id='deleteVersion'><img src="${contextPath}/res/img/delete.png" alt="delete version" title="delete version" /></a>
		    </c:if>
	    </div>
	    
	    <div id="experiment-files-switcher">
	    	<button id="experiment-files-switcher-exp">Experiments</button>
	    	<button id="experiment-files-switcher-files">Files</button>
	    </div>
	    
	    <div id="entityversiondetails">
			<div id="entityversionfiledetails">
	    		<div class="closebtn"><small><a id="entityversionfileclose">&otimes; close</a></small></div>
		    	<h3 id="entityversionfilename"></h3>
			    <div class="suppl"><small>File created</small> <time id="entityversionfiletime"></time> <small>by</small> <em id="entityversionfileauthor"></em>.</div>
			    <div id="entityversionfiledisplay"></div>
			</div>
			
		    <div id="entityversionfiles">
			    <h3>Files attached to this version</h3>
			    <table id="entityversionfilestable">
				</table>
				<a id='downloadArchive'><img src="${contextPath}/res/img/document-save-5.png" alt="download combine archive" title="download combine archive" /> Archive</a>
				
			    <div id="entityversionfilesreadme"></div>
			</div>
		</div>
		
		<div id="entityexperimentlist">
			<div id="entityexperimentlistpartners"></div>
			<div id="entityexperimentlistpartnersact">
				<a id="entityexperimentlistpartnersactall">select all</a>
				<a id="entityexperimentlistpartnersactnone">select none</a><br/>
				<button id="entityexperimentlistpartnersactcompare">compare selected experiments</button>
			</div>
		</div>
		
	</div>
</t:skeleton>

