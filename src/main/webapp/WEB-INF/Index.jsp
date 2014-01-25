<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}" contextPath="${contextPath}" newExpModelName="${newExpModelName}" newExpProtocolName="${newExpProtocolName}">
    <h1>Welcome</h1>
    <p>
        This is a prototype <strong>Functional Curation</strong> system for (single cell) cardiac electrophysiology.
        The key idea underpinning functional curation is that when mathematical and computational models are being developed and curated the primary goal should be the continuous validation of those models against experimental data.
        To achieve this goal, it must be possible to simulate in the computational models precisely the same protocols used in generating the experimental data on which the models are based.
        This system brings together models encoded using <a href="http://www.cellml.org/">CellML</a> and virtual experiment protocols encoded in our own language, using <a href="https://chaste.cs.ox.ac.uk/trac/wiki/ChasteGuides/CodeGenerationFromCellML#Standardisednames">standardised names</a> to interface between them.
        You can explore the results of running any protocol on any model, and compare how different models respond to the same protocol.
    </p>
    <div id="schematic">
        <img alt="Chaste - Functional Curation Schematic" src="/FunctionalCuration/res/img/fc_schematic.jpg" />    
    </div>
    <p>
    <a href="${contextPath}/about.html">Read more ...</a>
    </p>
</t:skeleton>

