<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}" contextPath="${contextPath}" newExpModelName="${newExpModelName}" newExpProtocolName="${newExpProtocolName}">
    <h1>Welcome</h1>

    <p>
        Welcome to the prototype <strong>Functional Curation</strong> system for cellular cardiac electro-physiology.
    </p>
    <p>
        Modellers have adopted XML-based markup languages to describe mathematical models over the past decade.
        This is great, as it means that models can be defined unambiguously, and shared easily, 
        in a machine-readable format.
    </p>
    <p>
        We have been trying to do the same thing with 'protocols' - to define what you have to 
        do to replicate/simulate an experiment, and analyse the results.
    </p>
    <p>
        This website provides an interface to our prototype system for cardiac electrophysiology.
        It brings together models encoded using <a href="http://www.cellml.org/">CellML</a> and 
        virtual experiment protocols encoded in our own language,
        using <a href="https://chaste.cs.ox.ac.uk/trac/wiki/ChasteGuides/CodeGenerationFromCellML#Standardisednames">
        standardised tags</a> to generate interfaces between them, doing all the necessary units conversions.
    </p>
    
    <div align="center" style="width:790px; text-align:center; margin: 0.5em 0pt 0.5em 0.8em; border: 1px solid #D1D1D1; padding: 10px;">
      <img alt="Chaste - Functional Curation Schematic" src="/FunctionalCuration/res/img/fc_schematic.png" width="780"/>
      <div style="width:770px;text-align:left;">
         A schematic of the way we organise model and protocol descriptions.
         This website provides an interface to a Model/Protocol Simulator, storing and displaying the results.
      </div>
    </div>
    
    <p>
        What's great about this is that for the first time you can:
        <ul>
            <li> compare how different models react to the same protocol,<br/>
                 e.g. <a href="${contextPath}/compare/e/508/509/510/511/512/514/515/516/517/518/519/521/522/523/524/525/526/show/1848458697/displayPlotFlot">action potentials under steady 1Hz pacing</a>,
                 or <a href="${contextPath}/compare/e/326/327/458/330/345/331/333/334/335/337/338/339/400/341/show/-1621094149/displayPlotFlot">an IV curve of the fast sodium current</a>.</li>
            <li> compare how a model behaves under different protocols,<br/>
                 e.g. <a href="${contextPath}/compare/e/145/131/show/845419076/displayPlotFlot">S1-S2 and steady state restitution</a>.</li>
        </ul>
    </p>

    <p>
        Have a look at the <a href="${contextPath}/db.html">main experiment database</a> to get started, 
        or <a href="${contextPath}/about.html">read more about the system.</a>
    </p>


</t:skeleton>

