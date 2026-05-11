package atlas.messenger.ui

actual fun selectImageFile(onSelected: (String?) -> Unit) {
    onSelected(null)
}