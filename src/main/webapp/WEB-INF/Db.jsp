<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}DB - " contextPath="${contextPath}" newExpModelName="${newExpModelName}" newExpProtocolName="${newExpProtocolName}">
    <h1>DB</h1>
    
    <button id="matrixchooser">matrix</button>
    <button id="searchchooser">search</button>
    
    <section id="searchTab">
	    <h2>Search</h2>
	    <input type="text" id="filter" placeholder="search the DB" /> <button>search</button>
    </section>
    
    <section id="matrixTab">
	    <h2>Matrix Overview</h2>
	    <span id="actionIndicator"></span><br/>
	    <div id="matrixdiv"></div>
	    <br/>
		<table class="matrixTable small">
			<tr>
				<td class="center">unrun</td>
				<td class="experiment-RUNNING center">running</td>
				<td class="experiment-SUCCESS center">successful</td>
				<td class="experiment-FAILED center">failed</td>
				<td class="experiment-INAPPRORIATE center">inappropriate</td>
			</tr>
		</table>
    </section>
</t:skeleton>

