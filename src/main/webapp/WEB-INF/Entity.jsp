<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}${entity.type} - " contextPath="${contextPath}">
    <c:choose>
        <c:when test="${entity.type eq 'model'}"><c:set var="otherType" value="protocol"/></c:when>
        <c:otherwise><c:set var="otherType" value="model"/></c:otherwise>
    </c:choose>
    <h1 id="entityname">
        <small>${fn:toUpperCase(fn:substring(entity.type, 0, 1))}${fn:toLowerCase(fn:substring(entity.type, 1,fn:length(entity.type)))}: </small>
        <span>${entity.name}</span>
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
    				<span class="experiment-INAPPLICABLE">inapplicable</span>
    				<span class="experiment-FAILED">failed</span>
    				<span class="experiment-PARTIAL">partial failure</span>
    				<span class="experiment-SUCCESS">success</span>
    			</p>
    		</c:if>
    		<c:if test="${User.allowedToCreateEntityVersion and entity.type ne 'experiment'}">
				<small>(<a href="${contextPath}/${entity.type}/createnew/?newentityname=${entity.id}">add new version</a>)</small>
			</c:if>
			<div id="entityversionlist_content">
				<c:forEach items="${entity.orderedVersions}" var="version" >
		    		<p id="version-item-${version.value.id}"
		    		   title="${version.value.created} -- Visibility: ${version.value.visibility}<c:if test="${entity.type == 'experiment'}"> -- ${version.value.status}</c:if>"
		    		   class="entityviz-${version.value.visibility}<c:if test="${entity.type == 'experiment'}"> experiment-${version.value.status}</c:if>">
	    				<input type="checkbox" value="${version.value.id}" class="comparisonCheckBox"/>
						<strong><c:choose>
						  <c:when test="${entity.type == 'experiment' and version.value.status == 'INAPPLICABLE'}">${version.value.version}</c:when>
						  <c:otherwise><a class="entityversionlink" href="${contextPath}/${entity.type}/${entity.url}/${entity.id}/${version.value.url}/${version.value.id}/">${version.value.version}</a></c:otherwise>
						</c:choose></strong>
						by <em>${version.value.author}
						<c:if test="${entity.author == User.nick || User.admin}">
					    	<a id='deleteVersion-${version.value.id}' class="deleteVersionLink"><img src="${contextPath}/res/img/delete.png" alt="delete version" title="delete this version of the ${entity.type}" /></a>
					    </c:if></em><c:if test="${not empty version.value.commitMessage}"> &mdash; <small>${version.value.commitMessage}</small></c:if><br/>
					    <c:if test="${entity.type == 'experiment' and (version.value.status == 'FAILED' or version.value.status == 'INAPPLICABLE') and version.value.returnText != 'finished'}">
					        <span class="suppl"><small>${version.value.returnText}</small></span><br/>
					    </c:if>
    					<span class="suppl"><small>created </small> <time>${version.value.created}</time> <small>containing</small> ${version.value.numFiles} file<c:if test="${version.value.numFiles!=1}">s</c:if>.</span>
				    </p>
		        </c:forEach>
			</div>
			<div id="compareVersionsSelectors">
			    [<small><a id="compareVersionsSelectorsAll">select all</a></small>]
			    [<small><a id="compareVersionsSelectorsNone">select none</a></small>]
			</div>
			<button id="compareVersions">compare selected versions</button>
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
		    <c:if test="${User.isAllowedToUpdateEntity(entity) && entity.type != 'experiment'}">
		    	Change visibility: 
		    	<select id="versionVisibility">
		    	    <c:if test="${User.admin}">
		    	        <option VALUE="MODERATED" id="visibility-MODERATED">MODERATED</option>
	    	        </c:if>
			    	<option value="PUBLIC" id="visibility-PUBLIC">PUBLIC</option>
			    	<option value="RESTRICTED" id="visibility-RESTRICTED">RESTRICTED</option>
			    	<option value="PRIVATE" id="visibility-PRIVATE">PRIVATE</option>
		    	</select>
                <img src="${contextPath}/res/img/info.png" alt="help" title="Public = anyone can view &#10;Restricted = logged-in users can view &#10;Private = only you can view"/>
		    	<span id="versionVisibilityAction"></span>
		    </c:if>
		    <span id="exptStatus"></span>
		    <c:if test="${entity.author == User.nick || User.admin}">
		    	Delete version: <a id='deleteVersion'><img src="${contextPath}/res/img/delete.png" alt="delete version" title="delete this version of the ${entity.type}" /></a>
		    </c:if>
		    <c:if test="${entity.type != 'experiment' && User.isAllowedToCreateNewExperiment()}">
		        Run experiments: <a class='runExpts' title="run experiments using this ${entity.type}"><img alt="run experiments using this ${entity.type}" src="${contextPath}/res/img/batch.png"/></a>
		    </c:if>
		    <c:if test="${entity.type == 'protocol' && User.admin}">
		        Refresh interface: <a id='updateInterface'><img src="${contextPath}/res/img/refresh.png" alt="refresh interface" title="recompute this protocol's ontology term interface"/></a>
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
	    		<div class="closebtn"><small>
	    		    <a id="exportPlot" style="display: none;">export plot data |</a>
			    <a id="zoomFile">zoom |</a>
	    		    <a id="entityversionfileclose">&otimes; close</a>
	    		</small></div>
		    	<h3 id="entityversionfilename"></h3>
			    <div class="suppl"><small>Created <time id="entityversionfiletime"></time> by <em id="entityversionfileauthor"></em>.</small></div>
			    <div id="entityversionfiledisplay"></div>
			</div>
			
		    <div id="entityversionfiles">
			    <h3>Files attached to this version</h3>
			    <p id="parse_status"></p>
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
			<div id="entityexperimentlistpartners"><ul></ul></div>
			<div id="entityexperimentlistpartnersact">
				[<a id="entityexperimentlistpartnersactall">select all</a>]
				<span id="entityexperimentlist_span_latest">[<a id="entityexperimentlistpartnersactlatest">select latest</a>]</span>
				[<a id="entityexperimentlistpartnersactnone">select none</a>]
				<br/>
				<button id="entityexperimentlist_showallversions">show all versions</button>
				<button id="entityexperimentlistpartnersactcompare">compare selected experiments</button>
			</div>
		</div>
		
	</div>
</t:skeleton>

