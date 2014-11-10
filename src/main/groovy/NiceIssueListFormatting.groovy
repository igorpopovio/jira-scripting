import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.properties.APKeys
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter

finalMessage = ""

def mainMethod() {
    // This script must be run from Jira -> Administration -> Add-ons -> Script Console
    jqlQuery = "project = DEMO and text ~ 'shortcut'"
    logImportantMessage "Executing query: <pre>${jqlQuery}</pre>"
    def issues = findIssues(jqlQuery)

    logImportantMessage "Found ${issues.size()} issues. Here they are:"
    logIssues(issues)

    return finalMessage // the returned value is shown after the execution in Jira's Web Script Console
}

def logIssues(Collection<MutableIssue> issues) {
    def tableStart = """
    <table id="issueTable" class="aui aui-table-sortable">
    <thead>
    <tr>
        <th class="aui-table-column-issue-key">Issue</th>
        <th>Summary</th>
    </tr>
    </thead>
    <tbody>
    """

    logMessage tableStart
    issues.each { issue -> logMessage formatIssue(issue) }

    def tableEnd = "</tbody></table>"
    logMessage tableEnd
}

def logMessage(Object message) {
    finalMessage += "${message}"
}

def logImportantMessage(Object message) {
    logMessage "<strong>${message}</strong>"
}

def formatIssue(MutableIssue issue) {
    def issueLink = getIssueLink(issue)
    def htmlLink = "<a href=\"${issueLink}\">${issue.key}</a>"
    "<tr><td>$htmlLink</td><td>${issue.summary}</td></tr>"
}

def getIssueLink(MutableIssue issue) {
    def properties = ComponentAccessor.applicationProperties
    def jiraBaseUrl = properties.getString(APKeys.JIRA_BASEURL)
    "${jiraBaseUrl}/browse/${issue.key}"
}

def findIssues(String jqlQuery) {
    def issueManager = ComponentAccessor.issueManager
    def user = ComponentAccessor.jiraAuthenticationContext.user
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class)
    def searchProvider = ComponentAccessor.getComponent(SearchProvider.class)

    def query = jqlQueryParser.parseQuery(jqlQuery)
    def results = searchProvider.search(query, user, PagerFilter.unlimitedFilter)
    results.issues.collect { issue -> issueManager.getIssueObject(issue.id) }
}

mainMethod()
