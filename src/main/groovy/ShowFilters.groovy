import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.bc.filter.SearchRequestService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser

finalMessage = ""

def mainMethod() {
    // This script must be run from Jira -> Administration -> Add-ons -> Script Console

    def searchRequestService = ComponentAccessor.getComponent(SearchRequestService.class)
    // def user = ComponentAccessor.jiraAuthenticationContext.user
    def userName = "admin"
    def user = ComponentAccessor.userManager.getUserByKey(userName)
    def filter = searchRequestService.getFilter(createServiceContext(user), 10000L)

    logImportantMessage "Showing filter..."

    logMessage filter.id
    logMessage filter.name
    logMessage filter.query.queryString

    logImportantMessage "------------------"

    return finalMessage // the returned value is shown after the execution in Jira's Web Script Console
}

def logMessage(Object message) {
    finalMessage += "${message}<br/>"
}

def logImportantMessage(Object message) {
    logMessage "<strong>${message}</strong>"
}

def createServiceContext(ApplicationUser user) {
    new JiraServiceContextImpl(user)
}

mainMethod()
