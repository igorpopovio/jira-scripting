import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.favourites.FavouritesManager
import com.atlassian.jira.portal.PortalPageManager
import com.atlassian.jira.user.ApplicationUser

finalMessage = ""

def mainMethod() {
    // This script must be run from Jira -> Administration -> Add-ons -> Script Console

    def userManager = ComponentAccessor.userManager
    def groupManager = ComponentAccessor.groupManager

    def group = "TEST GROUP"
    def dashboardId = 12345L
    def users = groupManager.getUsersInGroup(group)

//    users = []
//    users << ComponentAccessor.userManager.getUserByKey("TEST USER")

    logImportantMessage "FOUND ${users.size()} users in the \"$group\" group."
    users.each { user ->
        def applicationUser = userManager.getUserByKey(user.name)
        if (user.active) logMessage "$user.displayName ($user.name)"
        addDashboardToFavouritesForUser(applicationUser, dashboardId)
    }

    return finalMessage // the returned value is shown after the execution in Jira's Web Script Console
}

def addDashboardToFavouritesForUser(ApplicationUser user, Long dashboardId) {
    def favouritesManager = ComponentAccessor.getComponent(FavouritesManager.class)
    def dashboardManager = ComponentAccessor.getComponent(PortalPageManager.class)

    def dashboard = dashboardManager.getPortalPageById(dashboardId)
    def position = 0
    favouritesManager.addFavouriteInPosition(user, dashboard, position)
}

def logMessage(Object message) {
    finalMessage += "${message}<br/>"
}

def logImportantMessage(Object message) {
    logMessage "<strong>${message}</strong>"
}

mainMethod()
