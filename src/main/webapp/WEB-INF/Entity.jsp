<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}${entity.type} - " contextPath="${contextPath}" newExpModelName="${newExpModelName}" newExpProtocolName="${newExpProtocolName}">
    <c:choose>
        <c:when test="${entity.type eq 'model'}"><c:set var="otherType" value="protocol"/></c:when>
        <c:otherwise><c:set var="otherType" value="model"/></c:otherwise>
    </c:choose>
    <h1 id="entityname">
        <small>${fn:toUpperCase(fn:substring(entity.type, 0, 1))}${fn:toLowerCase(fn:substring(entity.type, 1,fn:length(entity.type)))}: </small>
        ${entity.name}
    </h1>
    
	<div class="suppl">
	    <small>Created <time>${entity.created}</time> by <em>${entity.author}</em>. 
    	    <c:if test="${entity.author == User.nick || User.admin}">
    	    	Delete ${entity.type}: <a id='deleteEntity' title="delete all versions of this ${entity.type}"><img src="${contextPath}/res/img/delete.png" alt="delete all versions of this ${entity.type}" title="delete all versions of this ${entity.type}"/></a>
    	    </c:if>
    	    
    		<c:if test="${User.allowedToForceNewExperiment && entity.type == 'experiment'}">
    			<a id="rerunExperiment"><img src="${contextPath}/res/img/refresh.png" alt="rerun experiment" title="rerun experiment"/></a> <span id="rerunExperimentAction"></span>
    		</c:if>
    		<c:if test="${entity.type == 'experiment'}">
    			<br/>Corresponding model: 
    			<a href="${contextPath}/${correspondingModel.entity.type}/${correspondingModel.entity.url}/${correspondingModel.entity.id}/${correspondingModel.url}/${correspondingModel.id}">${correspondingModel.name}</a>
    			&amp; protocol: 
    			<a href="${contextPath}/${correspondingProtocol.entity.type}/${correspondingProtocol.entity.url}/${correspondingProtocol.entity.id}/${correspondingProtocol.url}/${correspondingProtocol.id}">${correspondingProtocol.name}</a>
    		</c:if>
		</small>
	</div>
	
    
    <div id="entitydetails">
    	<h2>Versions</h2>
    	<div id="entityversionlist">
    		<c:if test="${entity.type == 'experiment'}">
    			<p id='expversioncolorlegend'>
    				Status Legend:
    				<span class="experiment-QUEUED">queued</span>
    				<span class="experiment-RUNNING">running</span>
    				<span class="experiment-INAPPRORIATE">inappropriate</span>
    				<span class="experiment-FAILED">failed</span>
    				<span class="experiment-PARTIAL">partial failure</span>
    				<span class="experiment-SUCCESS">success</span>
    			</p>
    		</c:if>
    		<c:if test="${entity.type ne 'experiment'}">
	   			<small>(<a href="${contextPath}/${entity.type}/createnew/?newentityname=${entity.id}">add new version</a>)</small>
	   		</c:if>
	   		<c:forEach items="${entity.orderedVersions}" var="version" >
	    		<p title="${version.value.created} -- Visibility: ${version.value.visibility}<c:if test="${entity.type == 'experiment'}"> -- ${version.value.status}</c:if>" class="entityviz-${version.value.visibility}<c:if test="${entity.type == 'experiment'}"> experiment-${version.value.status}</c:if>">
					<c:choose>
						<c:when test="${entity.type == 'experiment' && version.value.numFiles eq 0}">
							<strong>
							${version.value.version}</strong> by <em>${version.value.author}</em> - ${version.value.returnText}
							<c:if test="${entity.author == User.nick || User.admin}">
						    	<a id='deleteVersion-${version.value.id}' class="deleteVersionLink"><img src="${contextPath}/res/img/delete.png" alt="delete version" title="delete version" /></a>
						    </c:if><br/>
	    					<span class="suppl"><small>created </small> <time>${version.value.created}</time> <small>containing</small> ${version.value.numFiles} File<c:if test="${version.value.numFiles!=1}">s</c:if>.</span>
						</c:when>
						<c:otherwise>
							<strong>
							<a class="entityversionlink" href="${contextPath}/${entity.type}/${entity.url}/${entity.id}/${version.value.url}/${version.value.id}/">
							${version.value.version}</a></strong> by <em>${version.value.author}
							<c:if test="${entity.author == User.nick || User.admin}">
						    	<a id='deleteVersion-${version.value.id}' class="deleteVersionLink"><img src="${contextPath}/res/img/delete.png" alt="delete version" title="delete this version of the ${entity.type}" /></a>
						    </c:if></em><br/>
	    					<span class="suppl"><small>created </small> <time>${version.value.created}</time> <small>containing</small> ${version.value.numFiles} File<c:if test="${version.value.numFiles!=1}">s</c:if>.</span>
						</c:otherwise>
					</c:choose>
	    		</p>
	   		</c:forEach>
   		</div>
    </div>
    
    <div id="entityversion">
    	<div class="closebtn"><small><a id="entityversionclose">&otimes; close / see version table</a></small></div>
    	<h2 id="entityversionname"></h2>
	    <div class="suppl"><small>
	    <c:choose>
    	    <c:when test="${entity.type == 'experiment'}">Run</c:when>
    	    <c:otherwise>Created</c:otherwise>
	    </c:choose>
	    <time id="entityversiontime"></time> by <em id="entityversionauthor"></em>.
		    <c:if test="${entity.author == User.nick && entity.type != 'experiment'}">
		    	Change visibility: 
		    	<select id="versionVisibility">
			    	<option value="PUBLIC" id="visibility-PUBLIC">PUBLIC</option>
			    	<option value="RESTRICTED" id="visibility-RESTRICTED">RESTRICTED</option>
			    	<option value="PRIVATE" id="visibility-PRIVATE">PRIVATE</option>
		    	</select>
		    	<span id="versionVisibilityAction"></span>
		    </c:if>
		    <span id="exptStatus"></span>
		    <c:if test="${entity.author == User.nick || User.admin}">
		    	Delete version: <a id='deleteVersion'><img src="${contextPath}/res/img/delete.png" alt="delete version" title="delete this version of the ${entity.type}" /></a>
		    </c:if>
		    <c:if test="${entity.type != 'experiment' && User.isAllowedToCreateNewExperiment()}">
		        Run experiments: <a class='runExpts' title="Run experiments using this ${entity.type}"><img alt="Run experiments using this ${entity.type}" src="${contextPath}/res/img/batch.png"/></a>
		    </c:if>
		    </small>
	    </div>
	    
	    <div id="running-experiment-note" style="display: none;">
	       <p>
	           This experiment has not yet finished running, and so no result files are available.
	           You can refresh the page to see if it has completed.
	       </p>
	    </div>
	    
	    <div id="experiment-files-switcher">
	    	<button style="float:left;" id="experiment-files-switcher-files" title="View files associated with this ${entity.type}">Files</button>
            <button style="margin-left:5px; float:left;" id="experiment-files-switcher-exp" title="View &amp; compare experiments using this ${entity.type}">Compare ${otherType}s</button>
	    	<c:if test="${entity.type == 'protocol'}">
    	    	    <button style="margin-left:5px; float:left;" id="compare-all-models" title="Compare all available models under this protocol">Compare all</button>
   	    	</c:if>
		<br/>
	    </div>
	    
	    <div id="entityversiondetails">
			<div id="entityversionfiledetails">
	    		<div class="closebtn"><small><a id="entityversionfileclose">&otimes; close</a></small></div>
		    	<h3 id="entityversionfilename"></h3>
			    <div class="suppl"><small>Created <time id="entityversionfiletime"></time> by <em id="entityversionfileauthor"></em>.</small></div>
			    <div id="entityversionfiledisplay"></div>
			</div>
			
		    <div id="entityversionfiles">
			    <h3>Files attached to this version</h3>
			    <table id="entityversionfilestable">
				</table>
				<a id='downloadArchive' title="Download a 'combine' format archive of all the files">
				     <img src="${contextPath}/res/img/document-save-5.png" alt="Download" title="Download a 'combine' format archive of all the files"/> 
				     Download archive of all files
				</a>
    	        <div id="entityversionfilesreadme"></div>
			</div>
		</div>
		
		<div id="entityexperimentlist">
		    <h3>Experiments using this ${entity.type}</h3>
		    <p>
		        This ${entity.type} has been run with the following ${otherType}s.
		        Click to view the latest results of each single experiment, or select multiple experiments to compare them.
                <c:if test="${entity.type != 'experiment' && User.isAllowedToCreateNewExperiment()}">
                    You may also <a class='runExpts' title="Run experiments using this ${entity.type}">run new experiments using this ${entity.type}</a> and your own ${otherType}s.
                </c:if>
		    </p>
			<div id="entityexperimentlistpartners"></div>
			<div id="entityexperimentlistpartnersact">
				[<a id="entityexperimentlistpartnersactall">select all</a>]
				[<a id="entityexperimentlistpartnersactnone">select none</a>]<br/>
				<button id="entityexperimentlistpartnersactcompare">compare selected experiments</button>
			</div>
		</div>
		
	</div>
</t:skeleton>

