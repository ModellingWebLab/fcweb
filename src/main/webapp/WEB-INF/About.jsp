<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:skeleton headerImports="${PageHeader}" notes="${Notifications}" user="${User}" title="${Title}About - " contextPath="${contextPath}">
    <h1>About Functional Curation</h1>
    <p>
        The key idea underpinning functional curation is that when mathematical and computational models are
        being developed, a primary goal should be the continuous comparison of those models
        against experimental data. When computational models are being re-used in new studies, it is similarly
        important to check that they behave appropriately in the new situation to which you're applying them.
    </p>
    <p>
        To achieve this goal, it's a pre-requisite to be able to replicate <i>in-silico</i> precisely the same protocols used in an experiment of interest.
        We are therefore developing a language for describing rich 'virtual experiment' protocols,
        and software for running these on compatible models.
        This website provides a front-end to a prototype of that language and software.
    </p>
    <p>
        For more background on Functional Curation, our <a href="http://dx.doi.org/10.1016/j.pbiomolbio.2011.06.003">2011 reference publication</a> is a good place to start.
        We also have a <a href="https://peerj.com/preprints/273/">recent preprint on the underlying concept of <i>virtual experiments</i></a>.
        Full details about the <a href="https://chaste.cs.ox.ac.uk/trac/wiki/FunctionalCuration">open source software implementing the back-end for this system</a> can be found on the Chaste website.
        Documentation on the protocol language can also be found there.
    </p>
    <p>
        Please <a href="${contextPath}/db.html">browse around</a> and view the results of protocols encoded so far, run on a range of cell models.
        If you wish to evaluate your own models under these protocols, you will need to <a href="${contextPath}/register.html">register for an account</a> and have it approved.
        By default your own models, protocols and experiments are private so that only you may view them.
        This can be altered to 'restricted', providing visibility to all logged-in users, or 'public' to publish them to the world.
    </p>
    <p>
        Currently only trusted users may upload new protocols, but if you have one you'd like to add to the system, please do
        <a href="${contextPath}/contact.html">contact us</a>.
    </p>

    <h2>Future plans</h2>
    <p>
        The main features on our "to do" list include:
    </p>
    <ul>
        <li>Utilise an ontology for the standardised names used to identify model constructs accessible to protocols,
            to allow even more sophisticated interfacing between models and protocols.</li>
        <li>With community agreement, annotate models directly in the <a href="http://models.cellml.org/electrophysiology">CellML repository</a> using these standardised names.</li>
        <li>Develop a protocol editor, facilitating the creation of new protocols.</li>
        <li>Propose some of our protocol language features for the next versions of <a href="http://sed-ml.org/">SED-ML</a>.</li>
        <li>Link directly to databases of wet-lab experimental data from the same protocols,
            to allow comparison of simulated and real experiments.
            This will allow us to start to automatically validate models,
            and begin to think about auto-fitting, and even auto-developing models!</li>
    </ul>
    <p>
        Please <a href="${contextPath}/contact.html">get in touch</a> if you would like to join forces to help with any of these.
    </p>

    <h2>Acknowledgements</h2>
    <img style="float:left;" src="${contextPath}/res/img/chaste.jpg" alt="Chaste project logo"/>
    <p>
        We would like to thank Steve Niederer and the Noble modelling group for discussions on ways to test cell model behaviour.
        We gratefully acknowledge funding from the <a href="http://www.2020science.net/research/functional-curation">2020 Science Programme</a>.
        The functional curation software is built on top of <a href="https://chaste.cs.ox.ac.uk">Chaste</a>.
    </p>
</t:skeleton>

