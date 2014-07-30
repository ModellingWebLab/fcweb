<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}All Experiments - " contextPath="${contextPath}">
    <h1>Available experiments</h1>
    
    <!-- 
    <button id="matrixchooser">matrix</button>
    
    <button id="searchchooser">search</button>
    
    <section id="searchTab">
        <h2>Search (not yet implemented)</h2>
        <input type="text" id="filter" placeholder="search the DB" /> <button>search</button>
    </section>
     -->
     
    <section id="matrixTab">
        <!-- <h2>Matrix overview</h2> -->
        <p>
        This matrix shows the latest versions (visible to you) of the models and protocols in our database, with the corresponding experiments.
        <c:if test="${User.isAllowedToCreateNewExperiment()}">
            If you can't see experiments you expect to be there, this is probably because they are associated with an older version of a listed model or protocol.
            You can click on the white squares to launch experiments using the latest versions.
        </c:if>
        </p>
        <p>
        Note that you can compare models' behaviours under a particular protocol by viewing the protocol (click on a column heading), selecting the 'Compare models' button, and comparing the experiments using models of interest.
        The converse comparison (one model, many protocols) is available via viewing a model.
        </p>
        <p>
        Alternatively, enable 'comparison mode' to allow selecting arbitrary experiments from this matrix view to compare.<br/>
        Comparison mode: <button id="comparisonModeButton"></button>
        <span id="comparisonModeActions">
            <button id="comparisonLink">Compare selected experiments</button>
            <button id="comparisonMatrix" title="Show matrix featuring only selected models and/or protocols">Show sub-matrix</button>
        </span>
        </p>
        <div id="matrixdiv"></div>
        <br/>
        Key:
        <table class="matrixTable small">
            <tr>
                <td class="center">not run</td>
                <td class="experiment-QUEUED center">queued</td>
                <td class="experiment-RUNNING center">running</td>
                <td class="experiment-SUCCESS center">ran successfully</td>
                <td class="experiment-PARTIAL center">partially ran</td>
                <td class="experiment-FAILED center">failed to complete</td>
                <td class="experiment-INAPPRORIATE center">inappropriate</td>
            </tr>
        </table>
    </section>
</t:skeleton>

