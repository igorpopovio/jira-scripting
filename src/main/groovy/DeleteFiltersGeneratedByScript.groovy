import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.bc.filter.SearchRequestService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchRequest
import com.atlassian.jira.user.ApplicationUser
import groovy.time.TimeCategory
import groovy.time.TimeDuration

finalMessage = ""

def mainMethod() {
    // This script must be run from Jira -> Administration -> Add-ons -> Script Console

    GENERATED_BY_SCRIPT_TAG = "#generated-by-script"
    FILTER_OWNER = "admin"
    TIME = 5.minutes.ago

    def user = ComponentAccessor.userManager.getUserByKey(FILTER_OWNER)
    def filters = findAllFiltersGeneratedByScriptAndOwnedByUser(user)
    filters = findAllFiltersGeneratedAfter(TIME, filters)


    def relativeTime = new Date() - TIME
    logMessage "Will delete the filters generated since ${TIME} ($relativeTime ago)"
    logFilterDetails(filters)
    // deleteFilters(filters, user)

    return finalMessage // the returned value is shown after the execution in Jira's Web Script Console
}

def findAllFiltersGeneratedAfter(Date date, ArrayList<SearchRequest> filters) {
    filters.findAll { filter ->
        def generationDate = extractClearDateFrom(filter.description)
        generationDate.after(date)
    }
}

def deleteFilters(ArrayList<SearchRequest> filters, user) {
    def searchRequestService = ComponentAccessor.getComponent(SearchRequestService.class)
    filters.each { filter ->
        searchRequestService.deleteFilter(createServiceContext(user), filter.id)
    }
}

def logFilterDetails(Collection<SearchRequest> filters) {
    filters.each { filter -> logFilterDetails(filter) }
}

def logFilterDetails(SearchRequest filter) {
    def generationDate = extractClearDateFrom(filter.description)
    def timeSinceGeneration = new Date() - generationDate as TimeDuration;
    logMessage "<blockquote>"
    logImportantMessage "FILTER NAME: $filter.name, ID: $filter.id"
    logMessage "DESCRIPTION: $filter.description"
    logMessage "CLEAR DATE: $generationDate"
    logMessage "TIME SINCE CREATION: $timeSinceGeneration"
    logMessage "QUERY: <pre>$filter.query.queryString</pre>"
    logMessage "</blockquote>"
}

def extractClearDateFrom(String filterDescription) {
    def time = filterDescription.replace("#generated-by-script #date=", "").toLong()
    new Date(time)
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

def logImportantMessage(Object message) {
    logMessage "<strong>${message}</strong>"
}

use(TimeCategory) { mainMethod() }
