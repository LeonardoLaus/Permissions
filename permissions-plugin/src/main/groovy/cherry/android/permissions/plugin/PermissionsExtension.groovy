package cherry.android.permissions.plugin

class PermissionsExtension {
    def enabled = true

    def setEnabled(boolean enabled) {
        this.enabled = enabled
    }

    def getEnabled() {
        return enabled
    }
}