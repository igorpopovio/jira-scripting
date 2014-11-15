import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.bc.filter.SearchRequestService
import com.atlassian.jira.bc.issue.search.SearchService
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
    logMessage filter.query.whereClause.toString()

    def jqlQuery = filter.query.queryString + " ORDER BY issueKey"
    logMessage "Creating a query object from a string: \"${jqlQuery}\""

    def query = createQueryFromJqlQuery(jqlQuery)
    logMessage query

    filter.query = query
    def persistedFilter = searchRequestService.updateFilter(createServiceContext(user), filter)
    logMessage "The id of the persisted filter is: ${persistedFilter.id}"

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

def createQueryFromJqlQuery(String jqlQuery) {
    def user = ComponentAccessor.jiraAuthenticationContext.user.directoryUser
    def searchService = ComponentAccessor.getComponent(SearchService.class)

    def parseResult = searchService.parseQuery(user, jqlQuery);

    if (!parseResult.isValid())
        throw new RuntimeException("The query is not valid! query = ${jqlQuery}")

    // not needed - it is here just so I don't forget it...
    def searchContext = searchService.getSearchContext(user, parseResult.getQuery());

    return parseResult.getQuery()
}

mainMethod()
