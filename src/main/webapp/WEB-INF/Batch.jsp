<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}Run experiments - " contextPath="${contextPath}">
    <h1 id="entityname">Run experiments using ${entity.name}</h1>

    <c:if test="${User.allowedToForceNewExperiment}">
        <input type="checkbox" name="force" id="forceoverwrite" value="force"/> <label for="forceoverwrite">Force overwriting of existing experiments</label><br/>
    </c:if>
    
    <c:choose>
        <c:when test="${entity.entity.type eq 'model'}"><c:set var="otherType" value="protocol"/></c:when>
        <c:otherwise><c:set var="otherType" value="model"/></c:otherwise>
    </c:choose>

    <button id="checkAll">check all</button>
    <button id="checkLatest" title="Use only the latest version of each ${otherType}">check latest</button>
    <button id="uncheckAll">uncheck all</button>
    <button id="batchcreator1">run experiments</button>
    <br/> <span id="batchcreatoraction1"></span>

    <p>
        <c:if test="${entity.entity.type eq 'model'}">You may run this model under the following protocols.</c:if>
        <c:if test="${entity.entity.type eq 'protocol'}">You may run this protocol on the following models.</c:if>
        <c:if test="${not(User.allowedToForceNewExperiment)}">
            Note that only admins are allowed to overwrite the results of existing experiments, so you can only run model/protocol combinations that have not previously been run.
        </c:if>
    </p>

    <div id="batchlist">
        <h2>Your ${otherType}s</h2>
        <c:forEach items="${options}" var="opt" >
            <c:if test="${opt.author.nick eq User.nick}">
	            <c:if test="${opt.hasVersions()}">
	                <h3>${opt.name}</h3>
	                <ul>
	                    <c:set var="firstElement" value="true"/>
	                    <c:forEach items="${opt.getOrderedVersions()}" var="version" >
	                        <li title="${version.value.version}">
	                            <input type="checkbox" name="${version.value.id}" id="checkbox-${version.value.id}" class="batch-checkbox<c:if test="${firstElement eq true}"> latestVersion
	                                <c:set var="firstElement" value="false"/>
	                                </c:if>" value="${version.value.id}"/>
	                            <label for="checkbox-${version.value.id}"><strong>${version.value.version}</strong> by <em>${version.value.author}</em></label><br/>
	                            <span class="suppl"><small>created </small> <time>${version.value.created}</time> <small>containing</small> ${version.value.numFiles} File<c:if test="${version.value.numFiles!=1}">s</c:if>.</span>
	                        </li>
	                    </c:forEach>
	                </ul>
	            </c:if>
            </c:if>
        </c:forEach>
        <h2>Other ${otherType}s</h2>
        <c:forEach items="${options}" var="opt" >
            <c:if test="${opt.author.nick ne User.nick}">
                <c:if test="${opt.hasVersions()}">
                    <h3>${opt.name}</h3>
                    <ul>
                        <c:set var="firstElement" value="true"/>
                        <c:forEach items="${opt.getOrderedVersions()}" var="version" >
                            <li title="${version.value.version}">
                                <input type="checkbox" name="${version.value.id}" id="checkbox-${version.value.id}" class="batch-checkbox<c:if test="${firstElement eq true}"> latestVersion
                                    <c:set var="firstElement" value="false"/>
                                    </c:if>" value="${version.value.id}"/>
                                <label for="checkbox-${version.value.id}"><strong>${version.value.version}</strong> by <em>${version.value.author}</em></label><br/>
                                <span class="suppl"><small>created </small> <time>${version.value.created}</time> <small>containing</small> ${version.value.numFiles} File<c:if test="${version.value.numFiles!=1}">s</c:if>.</span>
                            </li>
                        </c:forEach>
                    </ul>
                </c:if>
            </c:if>
        </c:forEach>
        <button id="batchcreator2">Run experiments</button> <span id="batchcreatoraction2"></span>
    </div>
</t:skeleton>

