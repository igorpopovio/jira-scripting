import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.bc.portal.PortalPageService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.portal.PortalPage
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

    def portalPageService = ComponentAccessor.getComponent(PortalPageService.class)
    // def user = ComponentAccessor.jiraAuthenticationContext.user
    def userName = "PUT JIRA USERNAME HERE"
    def user = ComponentAccessor.userManager.getUserByKey(userName)

    def idOfPortalPageToClone = 10100L
    def portalPage = portalPageService.getPortalPage(createServiceContext(user), idOfPortalPageToClone)
    def clonedPortalPage = clonePortalPage(user, portalPage)

    logMessage "I have cloned the specified dashboard!"
    logMessage "Here is the id: " + clonedPortalPage.id

    logImportantMessage "------------------"

    return finalMessage // the returned value is shown after the execution in Jira's Web Script Console
}

def clonePortalPage(ApplicationUser user, PortalPage portalPage) {
    def favourite = true
    def portalPageService = ComponentAccessor.getComponent(PortalPageService.class)
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

mainMethod()
