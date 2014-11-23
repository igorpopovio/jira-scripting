import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.bc.portal.PortalPageService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.properties.APKeys
import com.atlassian.jira.issue.search.SearchRequest
import com.atlassian.jira.issue.search.SearchRequestManager
import com.atlassian.jira.portal.PortalPage
import com.atlassian.jira.portal.PortletConfiguration
import com.atlassian.jira.portal.PortletConfigurationManager
import com.atlassian.jira.user.ApplicationUser

finalMessage = ""

def mainMethod() {
    // This script must be run from Jira -> Administration -> Add-ons -> Script Console

    // Here are some explanations so all this makes sense:
    // - the dashboards in Jira were previously called "portals" - even though they
    // were renamed in the web interface, they still have the same name in code.
    // A dashboard in Jira is represented by a "PortalPage" class and can be
    // managed by using the "PortalPageService".
    // - the gadgets are represented by a "PortletConfiguration" class
    // - these gadgets have a "userPrefs" map that can be changed based on need

    // how to get the id: go to the URL of the dashboard and take the id
    // ex. http://localhost:2990/jira/secure/Dashboard.jspa?selectPageId=10100
    def idOfDashboardToClone = 10100L

    def newDashboard = createNewDashboardBasedOn(idOfDashboardToClone) { String originalString ->
        originalString.replace("2014.4.0", "2015.4.0")
    }

    logImportantMessage "NEW DASHBOARD LINK"
    logImportantMessage getDashboardLink(newDashboard)

    return finalMessage // the returned value is shown after the execution in Jira's Web Script Console
}

def createNewDashboardBasedOn(long idOfPortalPageToClone, Closure changeToApply) {
    def newDashboard = clonePortalPageById(idOfPortalPageToClone, changeToApply)
    def gadgets = extractGadgetsFrom(newDashboard)
    logImportantMessage "GADGETS WITH FILTERS IN THE NEW DASHBOARD"
    gadgets.each { gadget ->
        gadget.userPrefs.each { preference ->
            if (hasFilter(preference)) {
                logMessage getGadgetDetails(gadget)
                logMessage "<blockquote>"

                SearchRequest oldFilter = extractFilterFrom(preference)
                logMessage "old filter -> ${getFilterDetails(oldFilter)}"

                SearchRequest newFilter = createFilterBasedOn(oldFilter, changeToApply)
                logMessage "new filter -> ${getFilterDetails(newFilter)}"

                setGadgetFilter(gadget, preference.key, newFilter)
                logMessage "</blockquote>"
            }
        }
    }
    newDashboard
}

def extractGadgetsFrom(PortalPage portalPage) {
    def portalPageService = ComponentAccessor.getComponent(PortalPageService.class)
    def user = ComponentAccessor.jiraAuthenticationContext.user

    portalPageService
            .getPortletConfigurations(createServiceContext(user), portalPage.id)
            .flatten()
}

def setGadgetFilter(PortletConfiguration gadget, String key, SearchRequest filter) {
    def newUserPrefs = new HashMap<>(gadget.userPrefs)
    newUserPrefs[key] = "filter-$filter.id".toString()
    gadget.userPrefs = newUserPrefs

    def gadgetManager = ComponentAccessor.getComponent(PortletConfigurationManager.class)
    gadgetManager.store(gadget)
}

def extractFilterFrom(Map.Entry<String, String> preference) {
    def filterId = extractFilterIdFrom(preference)
    def searchRequestManager = ComponentAccessor.getComponent(SearchRequestManager.class)
    searchRequestManager.getSearchRequestById(filterId)
}

def createFilterBasedOn(SearchRequest filter, Closure changeToApply) {
    def oldQuery = filter.query.queryString
    def newQuery = changeToApply(oldQuery)
    def query = createQueryFromJqlQuery(newQuery)

    def newFilter = new SearchRequest(query)
    newFilter.name = changeToApply(filter.name)
    newFilter.owner = filter.owner
    newFilter.permissions = filter.permissions
    newFilter.description = createScriptIdentificationTag()
    def searchRequestManager = ComponentAccessor.getComponent(SearchRequestManager.class)
    searchRequestManager.create(newFilter)
}

def hasFilter(Map.Entry<String, String> preference) {
    preference.value.toLowerCase().contains('filter')
}

def clonePortalPageById(Long idOfPortalPageToClone, Closure changeToApply) {
    def portalPageService = ComponentAccessor.getComponent(PortalPageService.class)
    def user = ComponentAccessor.jiraAuthenticationContext.user

    def portalPage = portalPageService.getPortalPage(createServiceContext(user), idOfPortalPageToClone)

    def favourite = true
    def newDashboard = portalPageService.createPortalPageByClone(
            createServiceContext(user),
            createNewPortalPage(portalPage, changeToApply),
            portalPage.id,
            favourite)
    portalPageService.updatePortalPage(createServiceContext(user),
            newDashboard,
            favourite)
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

def createNewPortalPage(PortalPage portalPage, Closure changeToApply) {
    def name = ""
    name += changeToApply(portalPage.name)
    name += " "
    name += createScriptIdentificationTag() // name must be unique or else it will fail
    PortalPage
            .portalPage(portalPage)
            .name(name)
            .description(createScriptIdentificationTag())
            .build();
}

def createScriptIdentificationTag() {
    "#generated-by-script #date=${new Date().getTime()}"
}

def extractFilterIdFrom(Map.Entry<String, String> preference) {
    // example preference.value: "filter-12345"
    preference.value.replace("filter-", "").toLong()
}

def createQueryFromJqlQuery(String jqlQuery) {
    def user = ComponentAccessor.jiraAuthenticationContext.user.directoryUser
    def searchService = ComponentAccessor.getComponent(SearchService.class)
    return searchService.parseQuery(user, jqlQuery).getQuery()
}

def getDashboardLink(PortalPage dashboard) {
    def properties = ComponentAccessor.applicationProperties
    def jiraBaseUrl = properties.getString(APKeys.JIRA_BASEURL)
    def link = "$jiraBaseUrl/secure/Dashboard.jspa?selectPageId=$dashboard.id"
    "<a href=\"$link\">$dashboard.name</a>"
}

def getGadgetDetails(PortletConfiguration gadget) {
    "id: $gadget.id, row: $gadget.row, column: $gadget.column, type: ${getGadgetType(gadget)}"
}

def getGadgetType(PortletConfiguration gadget) {
    def uri = gadget.gadgetURI.toString()
    def type = uri.substring(uri.lastIndexOf('/') + 1)
    type = type.substring(0, type.lastIndexOf('.'))
    type.replace('-', ' ').capitalize()
}

def getFilterDetails(SearchRequest filter) {
    "id: $filter.id, name: $filter.name, query: <pre>$filter.query.queryString</pre>"
}

mainMethod()
