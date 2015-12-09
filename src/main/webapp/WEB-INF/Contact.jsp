<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}Contact - " contextPath="${contextPath}">
    <h1>Contact</h1>
    <p>
        Please direct queries about this website to <a href="mailto:jonathan.cooper@cs.ox.ac.uk?subject=Cardiac%20Web%20Lab&cc=gary.mirams@cs.ox.ac.uk">Jonathan Cooper</a>.
    </p>
    <p>
        The Functional Curation concept was invented by <a href="http://www.cs.ox.ac.uk/people/gary.mirams/">Gary Mirams</a> and <a href="http://www.cs.ox.ac.uk/people/jonathan.cooper/">Jonathan Cooper</a>.
        This web interface was created by <a href="http://www.sbi.uni-rostock.de/team/single/martin-scharm/">Martin Scharm</a>.
    </p>

    <p>
       <img alt="Team - Gary Mirams" style="float:left;margin:5px 10px 5px 0;" src="/FunctionalCuration/res/img/team/gary.jpg" width="150">
       <a href="http://www.cs.ox.ac.uk/people/gary.mirams/">Gary Mirams</a> is a mathematical modeller who started working on
       cardiac electrophysiology in 2009; and has been trying to work out how best to select, re-use and refine/develop cardiac models
       since then. He has written a few <a href="http://mirams.wordpress.com/">blog entries</a> about the motivation for this website.
    </p>
    <p style="clear:left;">
        <img alt="Team - Jonathan Cooper" style="float:left;margin:5px 10px 5px 0;" src="/FunctionalCuration/res/img/team/jonathan.jpg" width="150"/>
        <a href="http://www.cs.ox.ac.uk/people/jonathan.cooper/">Jonathan Cooper</a> is interested in using software engineering and computer science techniques 
        to improve the lives of research scientists. Most of his work has revolved around the development and use of 
        "domain specific languages" for describing and working with models of biological systems.
    </p>
    <p style="clear:left;">
        <img alt="Team - Martin Scharm" style="float:left;margin:5px 10px 10px 0;" src="/FunctionalCuration/res/img/team/martin.jpg" width="150"/>
        <a href="http://www.sbi.uni-rostock.de/team/single/martin-scharm/">Martin Scharm</a> created this web interface. 
        He is developing tools to identify differences between computational models and to 
        display these differences in a human readable format, to support model reuse.
    </p>

</t:skeleton>

