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
    <p>
        For more background on Functional Curation, see our <a href="http://www.2020science.net/research/functional-curation">section of the 2020 Science project website</a>,
        and our <a href="http://dx.doi.org/10.1016/j.pbiomolbio.2011.06.003">2011 reference publication</a>.
        You can also get the <a href="https://chaste.cs.ox.ac.uk/trac/wiki/FunctionalCuration">open source software implementing the backend for this system</a> from the Chaste website.
    </p>
    <p>
        You are encouraged to browse around and view the results of protocols encoded so far run on a range of cell models.
        If you wish to evaluate your own models under these protocols, you will need to register for an account.
        By default your own models and experiments are 'restricted' so that only logged-in users may view them.
        You may alternatively make them private to yourself, or publish them so that all users of the system can see the results.
    </p>
    <p>
        Currently only administrators may upload new protocols, but if you have one you'd like to add to the system, please do
        <a href="${contextPath}/contact.html">contact us</a>.
    </p>
    <hr/>
    <p>
        <b>Note:</b> it appears that the interactive portions of this site <b>do not work</b> with at least some versions of Microsoft Internet Explorer.
        The site has been tested successfully with both <a href="http://www.mozilla.org/en-US/firefox">Firefox</a> and <a href="https://www.google.com/intl/en/chrome/browser/">Chrome</a>.
    </p>
</t:skeleton>

