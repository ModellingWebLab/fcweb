<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}" contextPath="${contextPath}">
    <h1>Cardiac Electrophysiology Web Lab</h1>

    <h2>Training Workshop on the Web Lab</h2>
    <p>
    We will be running a 2-day training workshop on the Web Lab on 10-11 September 2015.
    See the <a href="https://chaste.cs.ox.ac.uk/trac/wiki/FunctionalCuration/Workshop2015">workshop website</a> for details and to register.
    The registration deadline is August 1st.
    </p>
    
    <h2>Quick start links</h2>
    <ul>
        <li><a href="${contextPath}/db.html">View results of experiments</a> stored on this site.</li>
        <li>Compare the results of different experiments, e.g.
            <a href="${contextPath}/compare/e/925/1231/1307/929/930/931/932/933/934/1432/1451/935/1288/937/1385/938/940/1540/942/944/945/">action potentials under steady 1Hz pacing</a>,
            <a href="${contextPath}/compare/e/326/327/458/330/345/331/333/334/335/337/338/339/400/341/">an IV curve of the fast sodium current</a>, or
            <a href="${contextPath}/compare/e/145/131/">S1-S2 and steady state restitution</a>;
            or <a href="${contextPath}/db.html">set up your own comparisons</a>.</li>
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
        This is our prototype system for cardiac electrophysiology.
        It brings together models encoded using <a href="http://www.cellml.org/">CellML</a> and
        virtual experiment protocols encoded in our own language,
        using <a href="https://chaste.cs.ox.ac.uk/trac/wiki/ChasteGuides/CodeGenerationFromCellML#Standardisednames">
        standardised tags</a> to generate interfaces between them, doing all the necessary units conversions.
        The <a href="${contextPath}/db.html">stored results of these experiments can be viewed</a> and compared.
    </p>

    <p>
        Comparison examples:<br/>
        <a href="${contextPath}/compare/e/508/509/510/511/512/514/515/516/517/518/519/521/522/523/524/525/526/">
            <button>Action potentials under steady 1Hz pacing</button></a>
        <a href="${contextPath}/compare/e/326/327/458/330/345/331/333/334/335/337/338/339/400/341/">
            <button>An IV curve of the fast sodium current</button></a>
        <a href="${contextPath}/compare/e/145/131/">
            <button>S1-S2 and steady state restitution</button></a>
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

