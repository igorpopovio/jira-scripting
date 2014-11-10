import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.properties.APKeys
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.label.LabelManager
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

    def newLabel = "productivity"
    logImportantMessage "Will add the ${newLabel} to all mentioned issues..."
    issues.each { issue -> addLabelToIssue(issue, newLabel) }
    logImportantMessage "Successfully updated all the mentioned issues..."

    return finalMessage // the returned value is shown after the execution in Jira's Web Script Console
}

def logIssues(Collection<MutableIssue> issues) {
    logMessage "<pre>"
    issues.each { issue -> logMessage formatIssue(issue) }
    logMessage "</pre>"
}

def logMessage(Object message) {
    finalMessage += "${message}<br/>"
}

def logImportantMessage(Object message) {
    logMessage "<strong>${message}</strong>"
}

def formatIssue(MutableIssue issue) {
    def issueLink = getIssueLink(issue)
    def htmlLink = "<a href=\"${issueLink}\">${issue.key}</a>"
    "<strong>${htmlLink}</strong> - ${issue.summary}"
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

def addLabelToIssue(MutableIssue issue, String label) {
    def labelManager = ComponentAccessor.getComponent(LabelManager.class)
    def user = ComponentAccessor.jiraAuthenticationContext.user.directoryUser
    def sendEmailUpdates = false
    labelManager.addLabel(user, issue.id, label, sendEmailUpdates)
}

mainMethod()
