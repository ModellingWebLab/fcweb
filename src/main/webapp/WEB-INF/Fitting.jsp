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
    <p>
      The settings below have been filled in from your chosen template, and may now be varied.
      You may adjust any of the fitting algorithm parameters, select a different model to fit,
      specify allowed ranges for the model parameters (or fix some to specific values rather than fitting them),
      and/or upload a new experimental data set.
    </p>
    <table id="fittingtable" class="leftright allborders">
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
        <th>Ranges for model parameters<br/>
            <small>TODO: In dimensionless form? Set both ends same to fix value?</small>
        </th>
        <td>
          <table id="modelParams" class="leftright">
          </table>
        </td>
      </tr>
      <tr>
        <th>Objective function parameters</th>
        <td>
          <table id="objParams" class="leftright">
          </table>
        </td>
      </tr>
      <tr>
        <th>Experimental data</th>
        <td>
          <span id="uploadaction"></span>
          <span id="dataColumnsInstructions" style="display:none;"></span>
          <table id="dataColumns" class="leftright">
          </table>
          <div id="dropbox">
            Drop Replacement Data File Here<br />
            <a>Open Dialog</a>
            <input type="file" id="fileupload" />
          </div>
        </td>
      </tr>
    </table>
    <p>
      <label for="versionName">Label:</label><br/>
      <input type="text" name="versionName" id="versionName" placeholder="short label for this experiment" size="35"/>
      <a class="pointer" id="dateInserter"><small>use current date &amp; time</small></a>
      <span id="versionAction"></span>
    </p>
    <p>
      <label for="commitMsg">Description:</label><br/>
      <textarea cols="70" rows="3" name="commitMsg" id="commitMsg" placeholder="optional message to describe this fitting experiment"></textarea>
    </p>
    <p>
      <label for="visibility">Visibility:</label>
      <select id="visibility">
        <option value="PUBLIC" id="visibility-PUBLIC">PUBLIC</option>
        <option value="RESTRICTED" id="visibility-RESTRICTED">RESTRICTED</option>
        <option value="PRIVATE" id="visibility-PRIVATE" selected="selected">PRIVATE</option>
      </select>
      <img src="${contextPath}/res/img/info.png" alt="help" title="Public = anyone can view &#10;Restricted = logged-in users can view &#10;Private = only you can view"/>
      <span id="visibilityAction"></span>
    </p>
    <button id="submit">Run fitting</button>
    <span id="submitAction"></span>
    <p id="successMsg"></p>
  </div>
</t:skeleton>