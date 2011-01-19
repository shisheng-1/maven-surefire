package org.apache.maven.plugins.surefire.report;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.reporting.MavenReportException;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 * @version $Id$
 */
public class SurefireReportGenerator
{
    private SurefireReportParser report;

    private List testSuites;

    private boolean showSuccess;

    private String xrefLocation;

    public SurefireReportGenerator( List reportsDirectories, Locale locale, boolean showSuccess, String xrefLocation )
    {
        report = new SurefireReportParser( reportsDirectories, locale );

        this.xrefLocation = xrefLocation;

        this.showSuccess = showSuccess;
    }

    public void doGenerateReport( ResourceBundle bundle, Sink sink )
        throws MavenReportException
    {
        testSuites = report.parseXMLReportFiles();

        sink.head();

        sink.title();
        sink.text( bundle.getString( "report.surefire.header" ) );
        sink.title_();

        StringBuffer str = new StringBuffer();
        str.append( "<script type=\"text/javascript\">\n" );
        str.append( "function toggleDisplay(elementId) {\n" );
        str.append( " var elm = document.getElementById(elementId + 'error');\n" );
        str.append( " if (elm && typeof elm.style != \"undefined\") {\n" );
        str.append( " if (elm.style.display == \"none\") {\n" );
        str.append( " elm.style.display = \"\";\n" );
        str.append( " document.getElementById(elementId + 'off').style.display = \"none\";\n" );
        str.append( " document.getElementById(elementId + 'on').style.display = \"inline\";\n" );
        str.append( " }" );
        str.append( " else if (elm.style.display == \"\") {" );
        str.append( " elm.style.display = \"none\";\n" );
        str.append( " document.getElementById(elementId + 'off').style.display = \"inline\";\n" );
        str.append( " document.getElementById(elementId + 'on').style.display = \"none\";\n" );
        str.append( " } \n" );
        str.append( " } \n" );
        str.append( " }\n" );
        str.append( "</script>" );
        sink.rawText( str.toString() );

        sink.head_();

        sink.body();

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.surefire.header" ) );
        sink.sectionTitle1_();
        sink.section1_();

        constructSummarySection( bundle, sink );

        Map suitePackages = report.getSuitesGroupByPackage( testSuites );
        if ( !suitePackages.isEmpty() )
        {
            constructPackagesSection( bundle, sink, suitePackages );
        }

        if ( !testSuites.isEmpty() )
        {
            constructTestCasesSection( bundle, sink );
        }

        List failureList = report.getFailureDetails( testSuites );
        if ( !failureList.isEmpty() )
        {
            constructFailureDetails( sink, bundle, failureList );
        }

        sink.body_();

        sink.flush();

        sink.close();
    }

    private void constructSummarySection( ResourceBundle bundle, Sink sink )
    {
        Map summary = report.getSummary( testSuites );

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.surefire.label.summary" ) );
        sink.sectionTitle1_();

        sinkAnchor( sink, "Summary" );

        constructHotLinks( sink, bundle );

        sinkLineBreak( sink );

        sink.table();

        sink.tableRow();

        sinkHeader( sink, bundle.getString( "report.surefire.label.tests" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.errors" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.failures" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.skipped" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.successrate" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.time" ) );

        sink.tableRow_();

        sink.tableRow();

        sinkCell( sink, (String) summary.get( "totalTests" ) );

        sinkCell( sink, (String) summary.get( "totalErrors" ) );

        sinkCell( sink, (String) summary.get( "totalFailures" ) );

        sinkCell( sink, (String) summary.get( "totalSkipped" ) );

        sinkCell( sink, summary.get( "totalPercentage" ) + "%" );

        sinkCell( sink, (String) summary.get( "totalElapsedTime" ) );

        sink.tableRow_();

        sink.table_();

        sink.lineBreak();

        sink.paragraph();
        sink.rawText( bundle.getString( "report.surefire.text.note1" ) );
        sink.paragraph_();

        sinkLineBreak( sink );

        sink.section1_();
    }

    private void constructPackagesSection( ResourceBundle bundle, Sink sink, Map suitePackages )
    {
        NumberFormat numberFormat = report.getNumberFormat();

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.surefire.label.packagelist" ) );
        sink.sectionTitle1_();

        sinkAnchor( sink, "Package_List" );

        constructHotLinks( sink, bundle );

        sinkLineBreak( sink );

        sink.table();

        sink.tableRow();

        sinkHeader( sink, bundle.getString( "report.surefire.label.package" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.tests" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.errors" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.failures" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.skipped" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.successrate" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.time" ) );

        sink.tableRow_();

        Iterator packIter = suitePackages.keySet().iterator();

        while ( packIter.hasNext() )
        {
            sink.tableRow();

            String packageName = (String) packIter.next();

            List testSuiteList = (List) suitePackages.get( packageName );

            Map packageSummary = report.getSummary( testSuiteList );

            sinkCellLink( sink, packageName, "#" + packageName );

            sinkCell( sink, (String) packageSummary.get( "totalTests" ) );

            sinkCell( sink, (String) packageSummary.get( "totalErrors" ) );

            sinkCell( sink, (String) packageSummary.get( "totalFailures" ) );

            sinkCell( sink, (String) packageSummary.get( "totalSkipped" ) );

            sinkCell( sink, packageSummary.get( "totalPercentage" ) + "%" );

            sinkCell( sink, (String) packageSummary.get( "totalElapsedTime" ) );

            sink.tableRow_();
        }

        sink.table_();

        sink.lineBreak();

        sink.paragraph();
        sink.rawText( bundle.getString( "report.surefire.text.note2" ) );
        sink.paragraph_();

        packIter = suitePackages.keySet().iterator();

        while ( packIter.hasNext() )
        {
            String packageName = (String) packIter.next();

            List testSuiteList = (List) suitePackages.get( packageName );

            Iterator suiteIterator = testSuiteList.iterator();

            sink.section2();
            sink.sectionTitle2();
            sink.text( packageName );
            sink.sectionTitle2_();

            sinkAnchor( sink, packageName );

            sink.table();

            sink.tableRow();

            sinkHeader( sink, "" );

            sinkHeader( sink, bundle.getString( "report.surefire.label.class" ) );

            sinkHeader( sink, bundle.getString( "report.surefire.label.tests" ) );

            sinkHeader( sink, bundle.getString( "report.surefire.label.errors" ) );

            sinkHeader( sink, bundle.getString( "report.surefire.label.failures" ) );

            sinkHeader( sink, bundle.getString( "report.surefire.label.skipped" ) );

            sinkHeader( sink, bundle.getString( "report.surefire.label.successrate" ) );

            sinkHeader( sink, bundle.getString( "report.surefire.label.time" ) );

            sink.tableRow_();

            while ( suiteIterator.hasNext() )
            {
                ReportTestSuite suite = (ReportTestSuite) suiteIterator.next();

                if ( showSuccess || suite.getNumberOfErrors() != 0 || suite.getNumberOfFailures() != 0 )
                {

                    sink.tableRow();

                    sink.tableCell();

                    sink.link( "#" + suite.getPackageName() + suite.getName() );

                    if ( suite.getNumberOfErrors() > 0 )
                    {
                        sinkIcon( "error", sink );
                    }
                    else if ( suite.getNumberOfFailures() > 0 )
                    {
                        sinkIcon( "junit.framework", sink );
                    }
                    else if ( suite.getNumberOfSkipped() > 0 )
                    {
                        sinkIcon( "skipped", sink );
                    }
                    else
                    {
                        sinkIcon( "success", sink );
                    }

                    sink.link_();

                    sink.tableCell_();

                    sinkCellLink( sink, suite.getName(), "#" + suite.getPackageName() + suite.getName() );

                    sinkCell( sink, Integer.toString( suite.getNumberOfTests() ) );

                    sinkCell( sink, Integer.toString( suite.getNumberOfErrors() ) );

                    sinkCell( sink, Integer.toString( suite.getNumberOfFailures() ) );

                    sinkCell( sink, Integer.toString( suite.getNumberOfSkipped() ) );

                    String percentage = report.computePercentage( suite.getNumberOfTests(), suite.getNumberOfErrors(),
                                                                  suite.getNumberOfFailures(), suite
                        .getNumberOfSkipped() );
                    sinkCell( sink, percentage + "%" );

                    sinkCell( sink, numberFormat.format( suite.getTimeElapsed() ) );

                    sink.tableRow_();
                }
            }

            sink.table_();

            sink.section2_();
        }

        sinkLineBreak( sink );

        sink.section1_();
    }

    private void constructTestCasesSection( ResourceBundle bundle, Sink sink )
    {
        NumberFormat numberFormat = report.getNumberFormat();

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.surefire.label.testcases" ) );
        sink.sectionTitle1_();

        sinkAnchor( sink, "Test_Cases" );

        constructHotLinks( sink, bundle );

        ListIterator suiteIterator = testSuites.listIterator();

        while ( suiteIterator.hasNext() )
        {
            ReportTestSuite suite = (ReportTestSuite) suiteIterator.next();

            List testCases = suite.getTestCases();

            if ( testCases != null && !testCases.isEmpty() )
            {
                ListIterator caseIterator = testCases.listIterator();

                sink.section2();
                sink.sectionTitle2();
                sink.text( suite.getName() );
                sink.sectionTitle2_();

                sinkAnchor( sink, suite.getPackageName() + suite.getName() );

                sink.table();

                while ( caseIterator.hasNext() )
                {
                    ReportTestCase testCase = (ReportTestCase) caseIterator.next();

                    if ( testCase.getFailure() != null || showSuccess )
                    {
                        sink.tableRow();

                        sink.tableCell();

                        Map failure = testCase.getFailure();

                        if ( failure != null )
                        {
                            sink.link( "#" + testCase.getFullName() );

                            sinkIcon( (String) failure.get( "type" ), sink );

                            sink.link_();
                        }
                        else
                        {
                            sinkIcon( "success", sink );
                        }

                        sink.tableCell_();

                        if ( failure != null )
                        {
                            sink.tableCell();

                            sinkLink( sink, testCase.getName(), "#" + testCase.getFullName() );

                            sink.rawText( "  <div class=\"detailToggle\" style=\"display:inline\">" );

                            sink.link( "javascript:toggleDisplay('" + testCase.getName() + "');" );

                            sink.rawText( "<span style=\"display: inline;\" " + "id=\"" + testCase.getName()
                                + "off\">+</span><span id=\"" + testCase.getName() + "on\" "
                                + "style=\"display: none;\">-</span> " );
                            sink.text( "[ Detail ]" );
                            sink.link_();

                            sink.rawText( "</div>" );

                            sink.tableCell_();
                        }
                        else
                        {
                            sinkCell( sink, testCase.getName() );
                        }

                        sinkCell( sink, numberFormat.format( testCase.getTime() ) );

                        sink.tableRow_();

                        if ( failure != null )
                        {
                            sink.tableRow();

                            sinkCell( sink, "" );
                            sinkCell( sink, (String) failure.get( "message" ) );
                            sinkCell( sink, "" );
                            sink.tableRow_();

                            List detail = (List) failure.get( "detail" );
                            if ( detail != null )
                            {

                                sink.tableRow();
                                sinkCell( sink, "" );

                                sink.tableCell();
                                sink.rawText(
                                    "  <div id=\"" + testCase.getName() + "error\" style=\"display:none;\">" );

                                Iterator it = detail.iterator();

                                sink.verbatim( true );
                                while ( it.hasNext() )
                                {
                                    sink.text( it.next().toString() );
                                    sink.lineBreak();
                                }
                                sink.verbatim_();

                                sink.rawText( "</div>" );
                                sink.tableCell_();

                                sinkCell( sink, "" );

                                sink.tableRow_();
                            }
                        }
                    }
                }

                sink.table_();

                sink.section2_();
            }
        }

        sinkLineBreak( sink );

        sink.section1_();
    }

    private void constructFailureDetails( Sink sink, ResourceBundle bundle, List failureList )
    {
        Iterator failIter = failureList.iterator();

        if ( failIter != null )
        {
            sink.section1();
            sink.sectionTitle1();
            sink.text( bundle.getString( "report.surefire.label.failuredetails" ) );
            sink.sectionTitle1_();

            sinkAnchor( sink, "Failure_Details" );

            constructHotLinks( sink, bundle );

            sinkLineBreak( sink );

            sink.table();

            while ( failIter.hasNext() )
            {
                ReportTestCase tCase = (ReportTestCase) failIter.next();

                Map failure = tCase.getFailure();

                sink.tableRow();

                sink.tableCell();

                String type = (String) failure.get( "type" );
                sinkIcon( type, sink );

                sink.tableCell_();

                sinkCellAnchor( sink, tCase.getName(), tCase.getFullName() );

                sink.tableRow_();

                String message = (String) failure.get( "message" );

                sink.tableRow();

                sinkCell( sink, "" );

                StringBuffer sb = new StringBuffer();
                sb.append( type );

                if ( message != null )
                {
                    sb.append( ": " );
                    sb.append( message );
                }

                sinkCell( sink, sb.toString() );

                sink.tableRow_();

                List detail = (List) failure.get( "detail" );
                if ( detail != null )
                {
                    Iterator it = detail.iterator();

                    boolean firstLine = true;

                    String techMessage = "";
                    while ( it.hasNext() )
                    {
                        techMessage = it.next().toString();
                        if ( firstLine )
                        {
                            firstLine = false;
                        }
                        else
                        {
                            sink.text( "    " );
                        }
                    }

                    sink.tableRow();

                    sinkCell( sink, "" );

                    sink.tableCell();
                    sink.rawText( "  <div id=\"" + tCase.getName() + "error\" >" );

                    if ( xrefLocation != null )
                    {
                        String path = tCase.getFullClassName().replace( '.', '/' );

                        sink.link( xrefLocation + "/" + path + ".html#" +
                            getErrorLineNumber( tCase.getFullName(), techMessage ) );
                    }
                    sink.text(
                        tCase.getFullClassName() + ":" + getErrorLineNumber( tCase.getFullName(), techMessage ) );

                    if ( xrefLocation != null )
                    {
                        sink.link_();
                    }
                    sink.rawText( "</div>" );

                    sink.tableCell_();

                    sink.tableRow_();
                }
            }

            sink.table_();
        }

        sinkLineBreak( sink );

        sink.section1_();
    }

    private String getErrorLineNumber( String className, String source )
    {
        StringTokenizer tokenizer = new StringTokenizer( source );

        String lineNo = "";

        while ( tokenizer.hasMoreTokens() )
        {
            String token = tokenizer.nextToken();
            if ( token.startsWith( className ) )
            {
                int idx = token.indexOf( ":" );
                lineNo = token.substring( idx + 1, token.indexOf( ")" ) );
                break;
            }
        }
        return lineNo;
    }

    private void constructHotLinks( Sink sink, ResourceBundle bundle )
    {
        if ( !testSuites.isEmpty() )
        {
            sink.paragraph();

            sink.text( "[" );
            sinkLink( sink, bundle.getString( "report.surefire.label.summary" ), "#Summary" );
            sink.text( "]" );

            sink.text( " [" );
            sinkLink( sink, bundle.getString( "report.surefire.label.packagelist" ), "#Package_List" );
            sink.text( "]" );

            sink.text( " [" );
            sinkLink( sink, bundle.getString( "report.surefire.label.testcases" ), "#Test_Cases" );
            sink.text( "]" );

            sink.paragraph_();
        }
    }

    private void sinkLineBreak( Sink sink )
    {
        sink.lineBreak();
    }

    private void sinkIcon( String type, Sink sink )
    {
        sink.figure();

        if ( type.startsWith( "junit.framework" ) || "skipped".equals( type ) )
        {
            sink.figureGraphics( "images/icon_warning_sml.gif" );
        }
        else if ( type.startsWith( "success" ) )
        {
            sink.figureGraphics( "images/icon_success_sml.gif" );
        }
        else
        {
            sink.figureGraphics( "images/icon_error_sml.gif" );
        }

        sink.figure_();
    }

    private void sinkHeader( Sink sink, String header )
    {
        sink.tableHeaderCell();
        sink.text( header );
        sink.tableHeaderCell_();
    }

    private void sinkCell( Sink sink, String text )
    {
        sink.tableCell();
        sink.text( text );
        sink.tableCell_();
    }

    private void sinkLink( Sink sink, String text, String link )
    {
        sink.link( link );
        sink.text( text );
        sink.link_();
    }

    private void sinkCellLink( Sink sink, String text, String link )
    {
        sink.tableCell();
        sinkLink( sink, text, link );
        sink.tableCell_();
    }

    private void sinkCellAnchor( Sink sink, String text, String anchor )
    {
        sink.tableCell();
        sinkAnchor( sink, anchor );
        sink.text( text );
        sink.tableCell_();
    }

    private void sinkAnchor( Sink sink, String anchor )
    {
        sink.anchor( anchor );
        sink.anchor_();
    }
}
