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
  <h1>Fit models to user-supplied data</h1>

  <div id="template_select">
    <!-- The interface for choosing a parameter fitting template protocol to specialise & run -->
    <h2>Available parameter fitting templates</h2>
    <ul>
      <c:forEach items="${FittingProtocols}" var="proto">
        <li id="proto_${proto.id}"><a id="link_${proto.id}" class="template_link">${proto.entity.name} -- created by ${proto.author} on <time>${proto.created}</time></a></li>
      </c:forEach>
    </ul>
    <div id="template_list"></div>
  </div>

  <div id="fitting_spec" style="display: none;">
    <!-- The interface for specifying settings based on a particular template fitting protocol -->
    <h2>Fitting experiment specification</h2>
    <table id="fittingtable" class="leftright">
      <tr>
        <th>Fitting algorithm</th>
        <td>
          <label for="algName">Name:</label>
          <select id="algName">
            <option value="" selected="selected"/>
          </select>
          <table id="algArgs" class="leftright">
          </table>
        </td>
      </tr>
      <tr>
        <th>Model to fit</th>
        <td>
          <select id="model"></select>
        </td>
      </tr>
      <tr>
        <th>Ranges for model parameters</th>
        <td>
          <table id="modelParams" class="leftright">
          </table>
        </td>
      </tr>
      <tr>
        <th>Experimental data</th>
        <td>
          TODO! File upload, link outputs to CSV columns.
        </td>
      </tr>
    </table>
    <button id="submit">Run fitting</button>
  </div>
</t:skeleton>