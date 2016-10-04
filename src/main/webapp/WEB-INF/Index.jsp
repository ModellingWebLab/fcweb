<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}" contextPath="${contextPath}">
    <h1>Electrochemistry Web Lab</h1>

    <h2>Quick start links</h2>
    <ul>
        <li><a href="${contextPath}/db.html">View and compare results of experiments</a> stored on this site.</li>
        <li><a href="${contextPath}/fitting.html">Fit models to your own data</a>.</li>
        <c:choose>
            <c:when test="${User != null && User.authorized}">
                <c:set var="analyseLink" value="myfiles.html"/>
            </c:when>
            <c:otherwise>
                <c:set var="analyseLink" value="register.html"/>
            </c:otherwise>
        </c:choose>
        <li><a href="${contextPath}/${analyseLink}">Analyse your own models/protocols</a>.</li>
        <li>Find out more about this site - read on!</li>
    </ul>

    <h2>What is Functional Curation?</h2>
    <p>
        Modellers have adopted XML-based markup languages to describe mathematical models over the past decade.
        This is great, as it means that models can be defined unambiguously, and shared easily, in a machine-readable format.
    </p>
    <p>
        We have been trying to do the same thing with 'protocols' - to define what you have to
        do to replicate/simulate an experiment, and analyse the results.
        We can then curate models according to their functional behaviour under a range of experimental scenarios.
    </p>
    <p>
        For the first time, we can thus easily compare how different models react to the same protocol,
        or compare how a model behaves under different protocols.
    </p>
    <p class="right"><a href="${contextPath}/about.html">Read more...</a></p>

    <h2>What is this website?</h2>
    <p>
        This is our prototype system for electrochemistry.
        More description TODO!
        The <a href="${contextPath}/db.html">stored results of these experiments can be viewed</a> and compared.
    </p>

    <p>
        If you wish to analyse your own models or create new protocols, you will need to <a href="${contextPath}/register.html">register for an account</a> and have it approved.
    </p>

    <div align="center" style="width:790px; text-align:center; margin: 0.5em 0pt 0.5em 0.8em; border: 1px solid #D1D1D1; padding: 10px;">
      <img alt="Chaste - Functional Curation Schematic" src="/FunctionalCuration/res/img/fc_schematic.png" width="780"/>
      <div style="width:770px;text-align:left;">
         A schematic of the way we organise model and protocol descriptions.
         This website provides an interface to a Model/Protocol Simulator, storing and displaying the results.
         (Adapted from <a href="http://dx.doi.org/10.1016/j.pbiomolbio.2014.10.001">Cooper, Vik and Waltemath 2014</a>, figure 1.)
      </div>
    </div>
    <p>
        Have a look at the <a href="${contextPath}/db.html">main experiment database</a> to get started,
        or <a href="${contextPath}/about.html">read more about the system.</a>
    </p>


</t:skeleton>

