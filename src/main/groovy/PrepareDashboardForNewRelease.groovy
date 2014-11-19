import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.bc.portal.PortalPageService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchRequest
import com.atlassian.jira.issue.search.SearchRequestManager
import com.atlassian.jira.portal.PortalPage
import com.atlassian.jira.portal.PortletConfiguration
import com.atlassian.jira.portal.PortletConfigurationManager
import com.atlassian.jira.user.ApplicationUser

finalMessage = ""

// TODO: add link to the new dashboard:
// http://igors-laptop.local:2990/jira/secure/Dashboard.jspa?selectPageId=10310
// $jiraBaseUrl/secure/Dashboard.jspa?selectPageId=$dashboard.id

def mainMethod() {
    // This script must be run from Jira -> Administration -> Add-ons -> Script Console

    // Here are some explanations so all this makes sense:
    // - the dashboards in Jira were previously called "portals" - even though they
    // were renamed in the web interface, they still have the same name in code.
    // A dashboard in Jira is represented by a "PortalPage" class and can be
    // managed by using the "PortalPageService".
    // - the gadgets are represented by a "PortletConfiguration" class
    // - these gadgets have a "userPrefs" map that can be changed based on need

    def idOfPortalPageToClone = 10100L

    def clonedPortalPage = clonePortalPageById(idOfPortalPageToClone)
    def gadgets = extractGadgetsFrom(clonedPortalPage)
    gadgets.each { gadget ->
        gadget.userPrefs.each { preference ->
            if (hasFilter(preference)) {
                SearchRequest oldFilter = extractFilterFrom(preference)
                SearchRequest newFilter = createFilterBasedOn(oldFilter)
                setGadgetFilter(gadget, preference.key, newFilter)
            }
        }
    }

    logMessage "I have cloned the specified dashboard!"
    logMessage "Here is the id: " + clonedPortalPage.id

    logImportantMessage "------------------"

    return finalMessage // the returned value is shown after the execution in Jira's Web Script Console
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
    logMessage "The filterId is: $filterId"
    def searchRequestManager = ComponentAccessor.getComponent(SearchRequestManager.class)
    searchRequestManager.getSearchRequestById(filterId)
}

def createFilterBasedOn(SearchRequest filter) {
    def oldQuery = filter.query.queryString
    def newQuery = "key=DEMO-1"
    logMessage "The old query is: $oldQuery"
    logMessage "The new query is: $newQuery"
    def query = createQueryFromJqlQuery(newQuery)

    def newFilter = new SearchRequest(query)
    newFilter.name = "$filter.name NEW ${new Date()}"
    newFilter.owner = filter.owner
    newFilter.permissions = filter.permissions
    def searchRequestManager = ComponentAccessor.getComponent(SearchRequestManager.class)
    searchRequestManager.create(newFilter)
}

def hasFilter(Map.Entry<String, String> preference) {
    preference.value.toLowerCase().contains('filter')
}

def clonePortalPageById(Long idOfPortalPageToClone) {
    def portalPageService = ComponentAccessor.getComponent(PortalPageService.class)
    def user = ComponentAccessor.jiraAuthenticationContext.user

    def portalPage = portalPageService.getPortalPage(createServiceContext(user), idOfPortalPageToClone)

    def favourite = true
    portalPageService.createPortalPageByClone(
            createServiceContext(user),
            createNewPortalPage(portalPage),
            portalPage.id,
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

def createNewPortalPage(PortalPage portalPage) {
    PortalPage
            .portalPage(portalPage)
            .name("${portalPage.name} CLONED ON ${new Date()}")
            .build();
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

mainMethod()
