import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.bc.portal.PortalPageService
import com.atlassian.jira.component.ComponentAccessor

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

    def portalPageService = ComponentAccessor.getComponent(PortalPageService.class)
    def user = ComponentAccessor.jiraAuthenticationContext.user
    def pages = portalPageService.getOwnedPortalPages(user)

    pages.each { page ->
        logImportantMessage "<h1>${page.name}</h1>"
        logMessage page.id
        logMessage page.ownerUserName
        logImportantMessage "CONFIGURATIONS"
        def configurations = portalPageService.getPortletConfigurations(createServiceContext(), page.id)
        configurations.each { line ->
            line.each { gadget ->
                logMessage gadget.id
                logMessage gadget.color
                logMessage gadget.gadgetURI
                gadget.userPrefs.each { preference ->
                    logMessage "${preference.key} = ${preference.value}"
                }
            }
        }
        logImportantMessage "------------------"
    }

    return finalMessage // the returned value is shown after the execution in Jira's Web Script Console
}

def logMessage(Object message) {
    finalMessage += "${message}<br/>"
}

def logImportantMessage(Object message) {
    logMessage "<strong>${message}</strong>"
}

def createServiceContext() {
    User user = ComponentAccessor.jiraAuthenticationContext.user.directoryUser
    new JiraServiceContextImpl(user)
}

mainMethod()
