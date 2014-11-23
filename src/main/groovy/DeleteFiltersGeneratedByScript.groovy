import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.bc.filter.SearchRequestService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchRequest
import com.atlassian.jira.user.ApplicationUser

finalMessage = ""

GENERATED_BY_SCRIPT_TAG = "#generated-by-script"

def mainMethod() {
    // This script must be run from Jira -> Administration -> Add-ons -> Script Console

    def user = ComponentAccessor.userManager.getUserByKey("admin")
    def filters = findAllFiltersGeneratedByScriptAndOwnedByUser(user)

    logMessage "Will delete these filters:"
    showFilters(filters)
    deleteFilters(filters, user)

    return finalMessage // the returned value is shown after the execution in Jira's Web Script Console
}

def showFilters(ArrayList<SearchRequest> filters) {
    filters.each { filter -> logMessage getFilterDetails(filter) }
}

def deleteFilters(ArrayList<SearchRequest> filters, user) {
    def searchRequestService = ComponentAccessor.getComponent(SearchRequestService.class)
    filters.each { filter ->
        searchRequestService.deleteFilter(createServiceContext(user), filter.id)
    }
}

def getFilterDetails(SearchRequest filter) {
    "id: $filter.id, " +
            "name: $filter.name, " +
            "description: $filter.description, " +
            "query: <pre>$filter.query.queryString</pre>"
}

def createServiceContext(ApplicationUser user) {
    new JiraServiceContextImpl(user)
}

def findAllFiltersGeneratedByScriptAndOwnedByUser(ApplicationUser user) {
    def searchRequestService = ComponentAccessor.getComponent(SearchRequestService.class)
    def filters = searchRequestService.getOwnedFilters(user)
    filters.findAll { filter -> isFilterGeneratedByScript(filter) }
}

def isFilterGeneratedByScript(SearchRequest filter) {
    filter.description?.contains(GENERATED_BY_SCRIPT_TAG)
}

def logMessage(Object message) {
    finalMessage += "${message}<br/>"
}

mainMethod()
