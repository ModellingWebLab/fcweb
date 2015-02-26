<!--                                                      
                                                      
    ___     / __      ___      ___    __  ___  ___    
  //   ) ) //   ) ) //   ) ) ((   ) )  / /   //___) ) 
 //       //   / / //   / /   \ \     / /   //        
((____   //   / / ((___( ( //   ) )  / /   ((____

     
-->
<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="contextPath" required="true"%>
<%@ attribute name="title" required="false"%>
<%@ attribute name="user" required="false" type="uk.ac.ox.cs.chaste.fc.beans.User"%>
<%@ attribute name="headerImports" required="true" type="uk.ac.ox.cs.chaste.fc.beans.PageHeader"%>
<%@ attribute name="notes" required="false" type="uk.ac.ox.cs.chaste.fc.beans.Notifications"%>

<!DOCTYPE html>
<html>
	<head>
		<title><c:if test="${not empty title}">${title}</c:if>Functional Curation @ Chaste - Cardiac Electrophysiology</title>
		
		<%-- static header fields -> used on every page --%>
		<script type='text/javascript'>
			var contextPath = "${contextPath}";
			var preferences = new Array ();
			<c:if test="${user != null && user.authorized}">
				<c:forEach items="${user.preferences}" var="setting" >
					preferences["${setting.key}"] = "${setting.value}";
				</c:forEach>
			</c:if>
		</script>
		<meta http-equiv='Content-Type' content='text/html;charset=utf-8' />
		<link rel='stylesheet' type='text/css' href='${contextPath}/res/css/style.css' />
		<link rel='stylesheet' type='text/css' href='${contextPath}/res/css/jquery-ui-1.10.4.custom.min.css' />
		<script type='text/javascript' src='${contextPath}/res/js/main.js' charset='UTF-8'></script>
		<script type='text/javascript' src='${contextPath}/res/js/3rd/jquery-2.0.3.min.js' charset='UTF-8'></script>
		<script type='text/javascript' src='${contextPath}/res/js/3rd/jquery-ui-1.10.4.custom.min.js' charset='UTF-8'></script>
		<script type='text/javascript' src='${contextPath}/res/js/3rd/xdate.js' charset='UTF-8'></script>
		<script type="text/x-mathjax-config">
			MathJax.Hub.Config({
				"HTML-CSS": {
					imageFont:null
  				}
			});
		</script>
		<script type='text/javascript' src='${contextPath}/res/js/3rd/MathJax-2.4-latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML' charset='UTF-8'></script>
		
		
		<%-- dynamic header fields -> just import them as needed --%>
		<c:if test="${headerImports != null}">
			<c:forEach items="${headerImports.metas}" var="himp" >
				<meta http-equiv='${himp.httpEquiv}' content='${himp.content}'/>
			</c:forEach>
			<c:forEach items="${headerImports.links}" var="himp" >
				<link rel='${himp.rel}' type='${himp.type}' href='${contextPath}/${himp.href}'/>
			</c:forEach>
			<c:forEach items="${headerImports.scripts}" var="himp" >
				<script type='${himp.type}' src='${contextPath}/${himp.src}' charset='${himp.charset}'>${himp.content}</script>
			</c:forEach>
		</c:if>
		
		<c:if test="${user != null}">
			<script type='text/javascript'>var ROLE = ${user.roleDump}; var USERNAME = "${user.nick}";</script>
		</c:if>
	</head>
	<body>
	  <div id="page">
	  	
	  	<%-- top of the page start --%>
		<header>
			<div id="logo">
				<a href="${contextPath}/index.html">
					<!-- <img alt="Chaste - Functional Curation" src="${contextPath}/res/img/chaste.jpg" /> -->
					<br/><br/>
				</a>
			</div>
			
			<nav>
				<ul class="navbar">
					<li><a href="${contextPath}/index.html">Home</a></li>
					<li><a href="${contextPath}/db.html">Experiments</a></li>
					<li><a href="${contextPath}/contact.html">Contact/Team</a></li>
					<li><a href="${contextPath}/about.html">About</a></li>
					<c:choose>
						<c:when test="${user != null && user.authorized}">
							<li id="usernavbar"><a href="${contextPath}/myfiles.html">${user.nick}</a>
								<ul>
									<li><a href="${contextPath}/myfiles.html">My files</a></li>
									<li><a href="${contextPath}/myaccount.html">Account</a></li>
									<c:if test="${user.role == 'ADMIN'}">
										<li><a href="${contextPath}/admin.html">Admin</a></li>
									</c:if>
									<li><a href="${contextPath}/logout.html">Logout</a></li>
								</ul>
							</li>
						</c:when>
						<c:otherwise>
							<li><a href="${contextPath}/login.html">Login</a></li>
							<li><a href="${contextPath}/register.html">Register</a></li>
						</c:otherwise>
					</c:choose>
				</ul>
			</nav>
			
			<div id="breadcrumb">
			</div>
		</header>
	  	<%-- top of the page done --%>
		
		
		
		
		<%-- notification stuff start --%>
		<div id="notifications">
				
				
				
					<div id="error"  <c:if test="${notes == null || !notes.error}">class="invisible"</c:if>     >
						<h3>The following errors occurred:</h3>
						<ul id='errorlist'>
							<c:forEach items="${notes.errors}" var="err" >
								<li>${err}</li>
							</c:forEach>
						</ul>
						<a class="pointer" id="dismisserrors">dismiss</a>
					</div><%-- #error --%>
					
					
					
				
				
				
					<div id="info"  <c:if test="${notes == null || !notes.info}">class="invisible"</c:if>     >
						<h3>Note:</h3>
						<ul id='infolist'>
							<c:forEach items="${notes.infos}" var="info" >
								<li>${info}</li>
							</c:forEach>
						</ul>
						<a class="pointer" id="dismissnotes">dismiss</a>
					</div><%-- #info --%>
		</div><%-- #notifications --%>
		<%-- notification stuff done --%>
		
		
		<%-- here comes the action --%>
		<div id="body">
			<jsp:doBody/>
		</div>
		<%-- action done --%>
		
		
		
		<%-- finish the site --%>
		<footer>
			Cardiac Electrophysiology Web Lab &copy; 2014 University of Oxford
		</footer>
		
		
	  </div><%-- #page --%>
	</body>
</html>