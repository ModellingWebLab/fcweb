<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}Fitting - " contextPath="${contextPath}">
<!-- Basic HTML for the interface goes here, with <div> placeholders for sections that the JS will construct. -->
<!-- We'll need at least 2 views:
 one displaying a list of available fitting experiments to use as templates (see e.g. db.js and entity.js for useful code);
 one giving the upload & settings view once a template has been selected.
 
 We can include the outlines of both views here, and the JS will hide whichever is not currently being shown.
 See Entity.jsp for potentially useful snippets.
  -->
</t:skeleton>