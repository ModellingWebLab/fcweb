<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}About - " contextPath="${contextPath}" newExpModelName="${newExpModelName}" newExpProtocolName="${newExpProtocolName}">
    <h1>About</h1>
    <p>
        For more background on Functional Curation, see our <a href="http://www.2020science.net/research/functional-curation">section of the 2020 Science project website</a>,
        and our <a href="http://dx.doi.org/10.1016/j.pbiomolbio.2011.06.003">2011 reference publication</a>.
        You can also get the <a href="https://chaste.cs.ox.ac.uk/trac/wiki/FunctionalCuration">open source software implementing the backend for this system</a> from the Chaste website.
    </p>
    <p>
        You are encouraged to browse around and view the results of protocols encoded so far run on a range of cell models.
        If you wish to evaluate your own models under these protocols, you will need to register for an account.
        By default your own models, protocols and experiments are private so that only you may view them.
        This can be altered to 'restricted', providing visibility to all logged-in users, or 'public' to publish them to the world.
    </p>
    <p>
        Currently only administrators may upload new protocols, but if you have one you'd like to add to the system, please do
        <a href="${contextPath}/contact.html">contact us</a>.
    </p>
</t:skeleton>

